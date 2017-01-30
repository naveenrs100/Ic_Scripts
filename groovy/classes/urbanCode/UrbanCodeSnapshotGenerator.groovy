package urbanCode;

import es.eci.utils.GlobalVars
import es.eci.utils.JobRootFinder
import es.eci.utils.LogUtils
import es.eci.utils.NexusHelper
import es.eci.utils.TmpDir
import es.eci.utils.ZipHelper
import es.eci.utils.base.Loggable
import es.eci.utils.pom.MavenCoordinates
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import hudson.model.*


/**
 * Esta clase genera el fichero solicitado por Urban Code para una release
 * conjunta de varios elementos.
 *
 * El formato es un JSON:
 *
 *
 {
 "name": "NombreAPP-version",
 "application": "NombreAPP",
 "description": "descripción de los cambios",
 "versions": [{"componente1": "1.0"}, {"componente2": "1.0"}, {"componente3": "1.1"}]
 }
 */
class UrbanCodeSnapshotGenerator extends Loggable {


	static class Context {
		Strategy c;

		public Context( Strategy c ) {
			this.c = c;
		}

		public void setStrategy(Strategy c) {
			this.c = c;
		}

		//Método de estrategia 'c'
		public List<MavenCoordinates> versionResolver(String apiNexus, String repositoryId, File folder, String component, MavenCoordinates version) {
			return c.resolve(apiNexus, repositoryId, folder, component, version);
		}
	}


	//--------------------------------------------------------------------------
	// Propiedades de la clase

	// Ruta de maven
	private String maven = null;

	//--------------------------------------------------------------------------
	// Métodos de la clase

	// Normaliza una cadena para su uso en nexus
	private String normalize(String string) {
		return string.replaceAll(/\s/, "_")
	}

	/**
	 * Construye un informe json desde cero, en un fichero txt
	 * @param appUrbanCode
	 * @param snapshot
	 * @param coord
	 * @param dir
	 * @return descriptor
	 */
	private String createReport(String appUrbanCode, String snapshot, MavenCoordinates coord) {
		UrbanCodeSnapshot report = new UrbanCodeSnapshot();
		report.setName("${snapshot}");
		report.setApplication(appUrbanCode);
		report.setDescription("Snapshot Urban Code");
		report.addVersion(coord.getArtifactId(), coord.getVersion());

		String descriptor = report.toJSON();

		return descriptor;
	}

	/**
	 * Actualiza el fichero existente con el nuevo componente y versión
	 * @param descriptor
	 * @param coord
	 * @return descriptor actualizado
	 */
	private String updateReport(String descriptor, MavenCoordinates coord) {
		// Se genera primero un report con el contenido previo del descriptor.
		UrbanCodeSnapshot report = UrbanCodeSnapshot.parseJSON(descriptor);
		// Se añade al report las nuevas coordenadas maven.
		report.addVersion(coord.getArtifactId(), coord.getVersion());
		descriptor = report.toJSON();

		return descriptor;
	}

	/**
	 * Este método crea o actualiza una instantánea en Nexus en las coordenadas indicadas
	 * con los artefactos presentes en el directorio indicado.
	 *
	 * El informe reside en ${grupoUrbanCode}:appUrbanCode_normalizada:instantanea, en formato zip
	 *
	 * @param stream Nombre de la corriente
	 * @param component Nombre del componente
	 * @param snapshot Instantánea informada para la release
	 * @param folder Directorio con fuentes, a explorar para encontrar el entregable
	 * @param nexusURL URL del repositorio nexus donde se crea o actualiza el informe
	 * @param maven ejecutable de maven
	 * @param urbanCodeGroupId groupId para el artefacto
	 * @param apiNexus URL de la api de resolución de versiones de Nexus
	 * @param snapshotsRepository Nombre del repositorio de snapshots en Nexus
	 * @param trivial Si es cierto, se fuerza la aplicación de una estrategia trivial (versión
	 * en el version.txt, sin resolución de -SNAPSHOT, componente parametrizado)
	 */
	public void snapshot(
			String appUrbanCode,
			String component,
			String snapshot, // instantánea
			File folder,
			String nexusURL,
			String maven,
			String grupoUrbanCode,
			String apiNexus,
			String snapshotsRepository,
			Boolean trivial,
			String homeStream,
			build,
			String nexusUser,
			String nexusPass,
			String gradleBin,
			String cScriptsStore,
			String instantanea,
			String urlNexus,
			String application,
			String stream,
			String componentRealName) {
		
		log "Informe: ${appUrbanCode}:${component}:${snapshot}"
		log "folder: " + folder

		if (grupoUrbanCode != null && grupoUrbanCode != "\$grupoUrbanCode") {
			// dir: directorio de descarga
			this.maven = maven

			GlobalVars gVars = new GlobalVars(build);
			String descriptor = gVars.get("descriptor"); // descriptor acumulado de la release.
			String descriptorInd = ""; // descriptor individual del componente.

			if(descriptor == null) {
				log ("Creando un nuevo informe y creando variable global vacía \"descriptor\"");
				gVars.put("descriptor","");
				descriptor = "";
			}

			log "Actualizando el descriptor desde la variable global \"descriptor\"";

			File versionTxt = new File(folder.getCanonicalPath() + System.getProperty("file.separator") + "version.txt")
			log "Version.txt: " + versionTxt.getCanonicalPath()
			log "Version.txt existe: " + versionTxt.exists()
			List<MavenCoordinates> artefactos = null;
			// Busca la versión en el version.txt
			artefactos = findArtifacts(component, versionTxt)
			if (artefactos != null){
				// A priori nos quedamos con el primer artefacto
				MavenCoordinates coord = artefactos.get(0)
				log ("******** Version encontrada: " + coord.getVersion())
				// Si se trata de una snapshot, la version hay q concatenarla con el timestamp de Nexus. Aplicar estrategia por cada caso
				Strategy estrategia = StrategyFactory.getStrategy(folder, trivial);

				Context context = new Context(estrategia);
				List<MavenCoordinates> coordList = context.versionResolver(apiNexus, snapshotsRepository, folder, component, coord);
				log ("coord = artefactos.get(0) -> ${coord}")
				log ("coordList -> ${coordList}")
				
				// Si se llega al punto de que snapshot es null o vacío es que es una release de componente.
				if(snapshot == null || snapshot.trim().equals("")) { 
					String text = versionTxt.text;
					def configObject = new ConfigSlurper().parse(text);
					def componentName = component.replace(" - ", "_").replace(" -", "_").replace("- ", "_").replace(" ", "_");
					snapshot = "${componentName}_${configObject.version}";
				}
				
				// Se actualiza el descriptor parcial y se inicializa el descriptor individual.
				if (coordList != null && coordList.size() > 0) {
					coordList.each {c ->
						descriptor = this.report(c, appUrbanCode, descriptor, snapshot);
						descriptor = this.addDocumentationEntries(descriptor, build, c, stream, componentRealName);
						descriptorInd = this.report(c, appUrbanCode, descriptorInd, snapshot);
						descriptorInd = this.addDocumentationEntries(descriptorInd, build, c, stream, componentRealName);
					}
					log ("************ REPORT: " + descriptor);
					// Se vuelve a setear la variable global "descriptor" al job raíz con su valor actualizado.
					gVars.put("descriptor", descriptor);
				}
			} else {
				log "NO HAY ARTEFACTOS QUE PROCESAR"
			}

			// Se sube el descriptor individual a Nexus.
			JobRootFinder jRootFinder = new JobRootFinder(build);
			def rootBuild = jRootFinder.getRootBuild(build);
			if(rootBuild.getFullDisplayName().contains("-COMP-")) { // Si el job raiz no es el de componente no se sube la individual.
				File jsonIndFile = new File("descriptor.json");
				jsonIndFile.text = descriptorInd;

				TmpDir.tmp { tmpDir ->
					def artifactId = application.replace(" - ", "_").replace(" -", "_").replace("- ", "_").replace(" ", "_");
					def componentName = component.replace(" - ", "_").replace(" -", "_").replace("- ", "_").replace(" ", "_");
					File tmp = new File(tmpDir.getCanonicalPath() + System.getProperty("file.separator") + "descriptor.json")
					tmp.text = jsonIndFile.text
					File zip = ZipHelper.addDirToArchive(tmpDir);
					String text = versionTxt.text;
					def configObject = new ConfigSlurper().parse(text);
					try {
						log "Subiendo el artifactId a la ruta ${grupoUrbanCode}:${artifactId}:${componentName}_${configObject.version} desde ${zip.getCanonicalPath()}"
						NexusHelper.uploadTarNexus(nexusUser, nexusPass, gradleBin, cScriptsStore, "fichas_despliegue", grupoUrbanCode, artifactId, "${componentName}_${configObject.version}", urlNexus, "true", zip.getCanonicalPath(), "zip", {logger.log it});
						log "Subido del descriptor completo a Nexus."
					} catch (Exception e) {
						log("[WARNING]: Ha habido un problema subiendo el descriptor individual a Nexus:");
						e.printStackTrace();
					}
					finally {
						zip.delete()
					}
				}
			}

		} else {
			log "NO SE HA INFORMADO LA PROPIEDAD grupoUrbanCode"
		}
	}

	/**
	 * Saca las coordenadas del version.txt
	 * @param component
	 * @param versionTxt
	 * @return List<MavenCoordinates>
	 */
	private List<MavenCoordinates> findArtifacts(String component, File versionTxt) {
		List<MavenCoordinates> coords = new ArrayList();
		if (versionTxt.exists()) {
			String text = versionTxt.text;
			def configObject = new ConfigSlurper().parse(text);
			MavenCoordinates coord = new MavenCoordinates(configObject.groupId, component, configObject.version);
			coords.add(coord);
		}
		return coords;
	}

	/**
	 * Devuelve el String "descriptor" con el json de componentes ya actualizado.
	 * @param coord
	 * @param appUrbanCode
	 * @param descriptor
	 * @param snapshot
	 * @return String descriptor
	 */
	private String report (MavenCoordinates coord, String appUrbanCode, String descriptor, String snapshot) {
		if (descriptor.equals("")) {
			descriptor = createReport(appUrbanCode, snapshot, coord);
		}
		else {
			descriptor = updateReport(descriptor, coord);
		}
		return descriptor;
	}

	/**
	 * Si hay parámetro "documentacion" marcado
	 * se añade al descriptor del componente.
	 * @param descriptor
	 * @param build
	 * @return descriptor
	 */
	private String addDocumentationEntries(String descriptor, AbstractBuild build, MavenCoordinates coord, String stream, String componentRealName) {
		def ret = null;
		def jobName = "${stream} -COMP- ${componentRealName}";
		def job = hudson.model.Hudson.instance.getJob(jobName);
		def documentValue = null;
		if(job != null) {
			job.getProperties().values().each {
				if(it instanceof hudson.model.ParametersDefinitionProperty) {
					if ((it.getParameterDefinition("componenteUrbanCode") != null) && ((it.getParameterDefinition("documentacion") != null))) {
						documentValue = it.getParameterDefinition("documentacion").getDefaultParameterValue().getValue();
						log("documentValue del job ${jobName} -> ${documentValue}");
					}
				}
			}
		}

		if(documentValue != null && documentValue.toString().trim().equals("true")) {
			JsonSlurper jsonSlurper = new JsonSlurper();
			def jsonObject = jsonSlurper.parseText(descriptor);
			def versions = jsonObject.versions;

			versions.add(["${coord.getArtifactId()}.doc":"${coord.getVersion()}"]);

			ret = JsonOutput.toJson(["name":"${jsonObject.name}","application":"${jsonObject.application}","description":"${jsonObject.description}","versions":versions]);

			log("El descriptor individual queda:")
			log(ret);

		} else {
			ret = descriptor;
			log("No se modifica el descriptor con entrada para documentación.")
		}

		return ret;
	}

}

