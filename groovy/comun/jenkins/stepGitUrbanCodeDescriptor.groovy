package jenkins

import urbanCode.UrbanCodeSnapshot;
import groovy.json.JsonOutput;
import urbanCode.UrbanCodeApplicationProcess
import urbanCode.UrbanCodeComponentVersion
import urbanCode.UrbanCodeExecutor
import urbanCode.UrbanCodeSnapshot;
import urbanCode.Constants;
import es.eci.utils.TmpDir;
import es.eci.utils.NexusHelper;
import es.eci.utils.ZipHelper;

def application = build.buildVariableResolver.resolve("application");
def component = build.buildVariableResolver.resolve("component");
def componenteUrbanCode = build.buildVariableResolver.resolve("componenteUrbanCode");
def tag = build.buildVariableResolver.resolve("tag");
def parentWorkspace = build.buildVariableResolver.resolve("parentWorkspace");

def urlNexus = 	build.getEnvironment(null).get("NEXUS_FICHAS_DESPLIEGUE_URL");
def urlNexusUser = build.buildVariableResolver.resolve("DEPLOYMENT_USER"); println("urlNexusUser -> " + urlNexusUser);
def nexusPass = build.buildVariableResolver.resolve("DEPLOYMENT_PWD"); println("nexusPass -> " + nexusPass);

def gradleBin = build.getEnvironment(null).get("GRADLE_HOME") + "/bin/gradle";
def cScriptsStore =	build.getEnvironment(null).get("C_SCRIPTS_HOME");
def groupIdUrbanCode = build.getEnvironment(null).get("URBAN_GROUP_ID");

def entornoUrban = build.buildVariableResolver.resolve("entornoUrbanCode");
def udClientCommand = build.getEnvironment(null).get("UDCLIENT_COMMAND");
def urlUrbanCode = build.getEnvironment(null).get("UDCLIENT_URL");
def urbanUser = build.getEnvironment(null).get("UDCLIENT_USER");
def urbanPassword = build.buildVariableResolver.resolve("UDCLIENT_PASS");
def urbanConnect = build.getEnvironment(null).get("URBAN_CONNECTION");
def urbanConnLocal = build.buildVariableResolver.resolve("URBAN_CONNECTION");
urbanConnect = (urbanConnLocal == null || (urbanConnLocal != "true" && urbanConnLocal != "false")) ? urbanConnect : urbanConnLocal;

try {	
	UrbanCodeExecutor exe = new UrbanCodeExecutor(udClientCommand, urlUrbanCode, urbanUser, urbanPassword);
	exe.initLogger({ println it });
	
	if(component != null && tag != null && application != null) {
		def components = [];
		components.add(["${componenteUrbanCode}" : tag]);

		def jsonDescriptor = JsonOutput.toJson(["name":"${component}_${tag}", "application":"${application}", "description":"Descriptor Json", "versions": components]);
		println("JsonDescriptor -> ${jsonDescriptor}");

		File jsonFile = new File("${parentWorkspace}/descriptor.json");
		jsonFile.text = jsonDescriptor;

		// Subimos el artefacto a Nexus.
		println "Subiendo el artifactId ${application}"
		TmpDir.tmp { tmpDir ->
			File tmp = new File(tmpDir.getCanonicalPath() + System.getProperty("file.separator")
					+ "descriptor.json")
			tmp.text = jsonFile.text
			println("El contenido de la ficha de despliegue que se va a subir es: \n ${tmp.text}");
			File zip = ZipHelper.addDirToArchive(tmpDir);
			try {
				println "Subiendo el descriptor.json parcial a Nexus:"											
				def artifactId = application.replace(" - ", "_").replace(" -", "_").replace("- ", "_").replace(" ", "_");
				
				NexusHelper.uploadTarNexus(urlNexusUser, nexusPass, gradleBin, cScriptsStore,
						"fichas_despliegue", groupIdUrbanCode, artifactId, "${component}_${tag}",
						urlNexus, "true", zip.getCanonicalPath(), "zip", {println it});
			} catch (Exception e) {
				println("[WARNING]: Ha habido un problema subiendo el descriptor a Nexus:");
				throw e;
			}
			finally {
				zip.delete()
			}
		}
		
				
		if(urbanConnect == "true") {
			try {
				// Se crea la versión primero sobre UrbanCode
				UrbanCodeComponentVersion componentVersion = 
					new UrbanCodeComponentVersion(componenteUrbanCode, tag, null, null);
				exe.createVersion(componentVersion);
			}
			catch (Exception e) {
				println "WARNING: Probablemente la versión ${tag} ya existía previamente"
				
			}
			
			// Se crea la instantánea en UrbanCode
			def objCompleteSnapshot = UrbanCodeSnapshot.parseJSON(jsonDescriptor);
			println("Creando la instantánea en UrbanCode...")
			exe.createSnapshot(objCompleteSnapshot);
			if (entornoUrban != null && !entornoUrban.trim().equals("")) {
				// lanzamiento de las instantáneas en el entorno indicado
				println("Lanzamiento de las instantáneas UrbanCode en el entorno ${entornoUrban}...")
				UrbanCodeApplicationProcess process = new UrbanCodeApplicationProcess(objCompleteSnapshot, Constants.DEPLOY_PROCESS, entornoUrban, false);
				exe.requestApplicationProcess(process);
			}
		}
		
		jsonFile.delete();
		
	}
	
} catch(Exception e) {
	println "ERROR: ${e.getMessage()}"
	e.printStackTrace()
	throw e
}

/**
 * Crea el archivo json, lo comprime en un zip y lo sube a Nexus
 * @param descriptor
 * @param artifactId
 * @param instantanea
 * @param urlNexus
 * @param groupIdUrbanCode
 * @param nexusUser
 * @param nexusPass
 * @param gradleBin
 * @param cScriptsStore
 */
private void createJsonFileAndUpload(descriptor, artifactId, instantanea, urlNexus, groupIdUrbanCode, nexusUser, nexusPass, gradleBin, cScriptsStore, parentWorkspace) {
	File jsonFile = new File("${parentWorkspace}/descriptor.json");
	jsonFile.text = descriptor;
	UrbanCodeSnapshot.zipAndUpload(jsonFile, artifactId, instantanea, urlNexus, groupIdUrbanCode, nexusUser, nexusPass, gradleBin, cScriptsStore);
	jsonFile.delete();
}
