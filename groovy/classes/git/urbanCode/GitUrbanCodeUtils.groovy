package git.urbanCode;

import hudson.model.*;
import org.w3c.dom.Document;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import urbanCode.UrbanCodeSnapshot;
import urbanCode.UrbanCodeComponentVersion;
import groovy.io.*;
import groovy.json.JsonSlurper;
import groovy.json.*;
import groovy.json.JsonSlurper;
import es.eci.utils.ZipHelper;
import es.eci.utils.NexusHelper;
import es.eci.utils.TmpDir;
import es.eci.utils.versioner.XmlUtils;

class GitUrbanCodeUtils {

	/**
	 * Obtenemos los componentes de la release a partir del archivo jenkinsComponents.txt que debería
	 * haber creado el getComponentsFromGit desde un principio y se crea en UrbanCode la snapshot.
	 * @param jenkinsComponentsFile
	 * @param build
	 * @param parentWorkspace
	 * @param udClient
	 * @param urlUrbanCode
	 * @param user
	 * @param password
	 */
	def urbanCodeCreateSnapshot(jenkinsComponentsFile,build,parentWorkspace,udClient,urlUrbanCode,user,password,exe) {
		def components = []
		if(jenkinsComponentsFile.exists() && !jenkinsComponentsFile.text.trim().equals("")) {
			components = removeLastComma(jenkinsComponentsFile.text).split(",");
		}
		else if(jenkinsComponentsFile.exists() && jenkinsComponentsFile.text.trim().equals("")) {
			println("NO HAY COMPONENTES NUEVOS QUE CONSTRUIR. TODOS ACTUALIZADOS.")
			build.setResult(Result.ABORTED);
		} else if(!jenkinsComponentsFile.exists()) {
			throw new Exception("El archivo \"jenkinsComponents.txt\" no existe en el directorio ${parentWorkspace}")
		}

		// Se da de alta en UrbanCode las versiones para cada componente.
		components.each { String component ->
			// Resolvemos versión por cada componente.
			def pomFile = new File("${parentWorkspace}/pom.xml");
			Document doc = XmlUtils.parseXml(pomFile);
			Node versionNode = XmlUtils.xpathNode(doc, "/project/version");
			String version = XmlUtils.solve(doc, versionNode.getTextContent());
			try {
				exe.initLogger({ println it });

				UrbanCodeComponentVersion componentVersion = new UrbanCodeComponentVersion(component, version, null, null);

				// Crear la versión sobre Urban Code
				def json = exe.createVersion(componentVersion);
				println("Creada versión en UrbanCode para el componente \"${component}\"");
				println(json);

			} catch(Exception e) {
				println("[WARNING]: No se ha podido dar de alta la version \"${version}\" para el componente \"${component}\"");
			}
		}
	}

	/**
	 * Crea la instantánea completa en Nexus
	 * @param jenkinsComponentsFile
	 * @param urbanConnect
	 * @param instantanea
	 * @param application
	 * @param gitGroup
	 * @param nexusUser
	 * @param nexusPass
	 * @param gradleBin
	 * @param cScriptsStore
	 * @param groupIdUrbanCode
	 * @param artifactId
	 * @param parentWorkspace
	 * @return
	 */
	def urbanCodeCreateCompleteDescriptor(jenkinsComponentsFile,urbanConnect,instantanea,application,gitGroup,nexusUser,nexusPass,
			gradleBin,cScriptsStore,groupIdUrbanCode,artifactId,parentWorkspace,exe) {
		// Se crea la instantánea completa.
		def componentsStrings = GitUrbanCodeUtils.removeLastComma(jenkinsComponentsFile.text).split(",");
		def components = [];
		componentsStrings.each { String component ->
			File pomFile = new File("${parentWorkspace}/pom.xml");
			def componentVersion = GitUrbanCodeUtils.getPomVersion(pomFile);
			def jobName = "${gitGroup} -COMP- ${component}";
			println("Comprobando si el job \"${jobName}\" en Jenkins tiene el parámetro \"componenteUrbanCode\"");
			def job = hudson.model.Hudson.instance.getJob(jobName);
			def jobParameters = [];
			if(job != null) {
				job.getProperties().values().each { value ->
					if(value instanceof hudson.model.ParametersDefinitionProperty) {
						jobParameters = value.getParameterDefinitionNames();
						println("JobParameters: ${jobParameters}");
					}
				}
			} else {
				println("El job ${jobName} no existe en Jenkins");
			}
			if(jobParameters.contains("componenteUrbanCode")) {
				def componentUrban;
				if(job != null) {
					job.getProperties().values().each { value ->
						println("value ->" + value)
						if(value instanceof hudson.model.ParametersDefinitionProperty) {
							componentUrban = value.getParameterDefinition("componenteUrbanCode").getDefaultParameterValue().getValue()
						}
					}
				}
				if(componentUrban != null) {
					if(!componentUrban.trim().equals("")) {
						println("El componente \"${component}\" está dado de alta en UrbanCode. Se añade al json...");
						components.add(["${componentUrban}": "${componentVersion}"])
					}
				}
			}
		}

		def jsonComplete = JsonOutput.toJson(["name": "${instantanea}", "application": "${application}" ,
			"description": "Snapshot Urban Code", "versions" : components])

		println("jsonComplete -> \n" + jsonComplete); // Json de UrbanCode completo

		// Se sube el nuevo descriptor al Nexus.
		TmpDir.tmp { tmpDir ->
			File tmp = new File(tmpDir.getCanonicalPath() + System.getProperty("file.separator") + "descriptor.json")
			tmp.text = jsonComplete;
			File zip = ZipHelper.addDirToArchive(tmpDir);
			try {
				println "Subiendo el artifactId a la ruta ${groupIdUrbanCode}:${artifactId}:${instantanea} desde ${zip.getCanonicalPath()}"
				NexusHelper.uploadTarNexus(nexusUser, nexusPass, gradleBin, cScriptsStore,
						"fichas_despliegue", groupIdUrbanCode, artifactId, "${instantanea}", urlNexus, "true", zip.getCanonicalPath(), "zip", {println it});

				println "Subido del descriptor completo a Nexus.";

			} catch (Exception e) {
				println("[WARNING]: Ha habido un problmema subiendo el descriptor completo a Nexus:");
				e.printStackTrace();
			}
			finally {
				zip.delete()
			}
		}

		def objCompleteSnapshot = UrbanCodeSnapshot.parseJSON(jsonComplete);
		if(urbanConnect == "true") {
			exe.createSnapshot(objCompleteSnapshot);
		}

	}

	/**
	 * Devuelve la versión de un pom.xml
	 * @param pomFile
	 * @return Sting pomVersion
	 */
	def getPomVersion(File pomFile) {
		Document doc = XmlUtils.parseXml(pomFile);
		Node versionNode = XmlUtils.xpathNode(doc, "/project/version");
		String pomVersion = XmlUtils.solve(doc, versionNode.getTextContent());
		return pomVersion;
	}

	/**
	 * Elimina el último carácter de un String.
	 * Lo usamos porque la lista de jobs vendrá
	 * con una coma al final que hay que eliminar.
	 * @param (String)text
	 * @return (String)result
	 */
	def removeLastComma(String text) {
		def result;
		if(text.endsWith(",")) {
			result = text.substring(0, text.length() - 1);
		} else {
			result = text;
		}
		return result;
	}

}
