import java.io.File;

import hudson.model.*;
import urbanCode.*;
import es.eci.utils.StringUtil;
import es.eci.utils.TmpDir;
import es.eci.utils.ScmCommand;
import es.eci.utils.CheckSnapshots;
import es.eci.utils.NexusHelper;
import es.eci.utils.ZipHelper;
import es.eci.utils.GlobalVars;
import groovy.json.*;
import groovy.lang.Closure;

import java.io.File;

/**
 * Este script se invoca DESDE EL JOB DE LA CORRIENTE para crear la instantánea
 * en Urban Code y además lanzarla contra un determinado entorno
 * Parámetros
 * udclient - Ruta del cliente udclient
 * urlUrbanCode - URL de udeploy
 * user - Usuario Urban Code
 * password - Password del usuario Urban Code
 * groupIdUrbanCode - groupId Nexus asociado a la aplicación Urban Code 
 * snapshot - Instantánea Urban Code
 * urlNexus - URL de nexus 
 * entorno - Identificador de entorno (puede ser null, en cuyo caso no se lanza
 * la instantánea)
 */

def build = Thread.currentThread().executable;
def resolver = build.buildVariableResolver;

def udClientCommand = 	build.getEnvironment(null).get("UDCLIENT_COMMAND"); 			println("udClientCommand -> " + udClientCommand);
def urlUrbanCode = 		build.getEnvironment(null).get("UDCLIENT_URL"); 				println("urlUrbanCode -> " + urlUrbanCode);
def user = 				build.getEnvironment(null).get("UDCLIENT_USER"); 				println("user -> " + user);
def groupIdUrbanCode = 	build.getEnvironment(null).get("URBAN_GROUP_ID"); 				println("groupIdUrbanCode -> " + groupIdUrbanCode);
def urlNexus = 			build.getEnvironment(null).get("NEXUS_FICHAS_DESPLIEGUE_URL"); 	println("urlNexus -> " + urlNexus);
def rtcUser = 			build.getEnvironment(null).get("userRTC"); 						println("rtcUser -> " + rtcUser);
def rtcUrl = 			build.getEnvironment(null).get("urlRTC"); 						println("rtcUrl -> " + rtcUrl);
def maven = 			build.getEnvironment(null).get("MAVEN_HOME") + "/bin/mvn"; 		println("maven -> " + maven);
def cScriptsStore = 	build.getEnvironment(null).get("C_SCRIPTS_HOME"); 				println("cScriptsStore -> " + cScriptsStore);
def gradleBin =		    build.getEnvironment(null).get("GRADLE_HOME") + "/bin/gradle"; 	println("gradleHome -> " + gradleBin);

def password = 			resolver.resolve("UDCLIENT_PASS"); 				println("password -> " + password);
def instantanea = 		resolver.resolve("instantanea"); 				println("instantanea -> " + instantanea);
def entorno = 			resolver.resolve("entornoUrbanCode"); 			println("entorno -> " + entorno);
def stream = 			resolver.resolve("stream"); 					println("stream -> " + stream);
def streamCargaInicial =resolver.resolve("streamCargaInicial"); 		println("streamCargaInicial -> " + streamCargaInicial);
def rtcPass = 			resolver.resolve("pwdRTC"); 					println("rtcPass -> " + rtcPass);
def application = 		resolver.resolve("aplicacionUrbanCode"); 		println("application -> " + application);
def nexusUser = 		resolver.resolve("DEPLOYMENT_USER"); 			println("nexusUser -> " + nexusUser);
def nexusPass = 		resolver.resolve("DEPLOYMENT_PWD"); 			println("nexusPass -> " + nexusPass);
def homeStream = 		resolver.resolve("homeStream"); 				println("homeStream -> " + homeStream);
def providedComponents =resolver.resolve("providedComponents");			println("providedComponents -> " + providedComponents);

def streamTarget = 		resolver.resolve("streamTarget").equals("") ? stream : resolver.resolve("streamTarget"); println("streamTarget -> " + streamTarget);

def urbanConnect = 		build.getEnvironment(null).get("URBAN_CONNECTION"); println("urbanConnect -> " + urbanConnect);
def urbanConnLocal =	resolver.resolve("URBAN_CONNECTION"); 				println("urbanConnLocal -> " + urbanConnLocal);
urbanConnect = (urbanConnLocal == null) ? urbanConnect : urbanConnLocal;	println("Recalculated urbanConnect -> " + urbanConnect);

def componentsUrban = 	resolver.resolve("componentsUrban");

/**
 * Comienzo del script que sube los descriptores parciales y completos a Nexus y da de alta la aplicación en
 * UrbanCode dependiendo del valor de la variable de entorno "URBAN_CONNECTION".
 */
try {
	UrbanCodeExecutor exe = new UrbanCodeExecutor(udClientCommand, urlUrbanCode, user, password);
	exe.initLogger({ println it });

	String artifactId = application.replace(" - ", "_").replace(" -", "_").replace("- ", "_").replace(" ", "_");

	def theStream = stream;
	if (streamCargaInicial != null && streamCargaInicial.trim().length() > 0) {
		theStream = streamCargaInicial;
	}
	def objCompleteSnapshot = createCompleteDescriptor(
			streamTarget, rtcUser, rtcPass, rtcUrl, instantanea, exe,
			theStream, application, artifactId, urlNexus, maven, groupIdUrbanCode,
			urbanConnect, nexusUser, nexusPass, gradleBin, cScriptsStore, build,
			new UrbanCodeExecutor(udClientCommand, urlUrbanCode, user, password),
			entorno);


} catch(Exception e) {
	println "ERROR: ${e.getMessage()}"
	e.printStackTrace()
	throw e
}

/**
 * Creación del descriptor completo si existe la streamTarget.
 * La instantánea completa se crea comprobando qué jobs tienen definido el
 * parámetro "componenteUrbanCode". Si tienen definido ese parámetro
 * se añade al json descriptor completo su nombre y su versión.
 * @param streamTarget
 * @param rtcUser
 * @param rtcPass
 * @param rtcUrl
 * @param instantanea
 * @param exe
 * @param urbExe Objeto 
 * @return objCompleteSnapshot
 */
private createCompleteDescriptor(
		streamTarget, rtcUser, rtcPass, rtcUrl, instantanea, exe,
		stream, application, artifactId, urlNexus, maven, groupIdUrbanCode, urbanConnect,
		nexusUser, nexusPass, gradleBin, cScriptsStore, build,
		UrbanCodeExecutor urbExe, entorno) {

	CheckSnapshots chk = new CheckSnapshots();
	chk.initLogger { println it };
	boolean existsStream = chk.checkRTCstreams(streamTarget, rtcUser, rtcPass, rtcUrl);
	UrbanCodeSnapshot objCompleteSnapshot = null;

	if(existsStream) {
		def baselinesJson = returnBaseLinesJson(streamTarget, instantanea, rtcUser, rtcPass, rtcUrl);

		println("Líneas base: " + baselinesJson + "\n");

		if(baselinesJson != null) {
			def jsonSlurper = new JsonSlurper()
			def object = jsonSlurper.parseText(baselinesJson)

			def components = [];
			object.each {
				def jobName = "${stream} -COMP- ${it.name}";
				//println("Comprobando si el job \"${jobName}\" en Jenkins tiene el parámetro \"componenteUrbanCode\"");
				def job = hudson.model.Hudson.instance.getJob(jobName);
				def jobParameters = [];
				def compoUrbanValue = null;
				def documentValue = null;
				if(job != null) {
					job.getProperties().values().each {
						if(it instanceof hudson.model.ParametersDefinitionProperty) {
							jobParameters = it.getParameterDefinitionNames();
							
							// Si existe el parámetro "componenteUrbanCode" y el parámetro "documentacion" los
							// añadimos al descriptor completo.
							if (it.getParameterDefinition("componenteUrbanCode") != null) {
								compoUrbanValue = it.getParameterDefinition("componenteUrbanCode").getDefaultParameterValue().getValue();
								//println("componenteUrbancode del job ${jobName} -> \"${compoUrbanValue}\"");
								
								//Comprobamos que existe el parámetro "documentacion".
								if ((it.getParameterDefinition("documentacion") != null)) {
									documentValue = it.getParameterDefinition("documentacion").getDefaultParameterValue().getValue();
									println("documentValue del job ${jobName} -> ${documentValue}");
								}									
								else {
									println("[WARNING]: El parámetero \"documentacion\" del job ${jobName} es null, por lo " +
											"que no se genera ficha de despliegue para su documentacion")
								}																
							}												
							// println("JobParameters: ${jobParameters}");							
						}
					}
				} else {
					println("[WARNING]: El job ${jobName} no existe en Jenkins.");
				}

				if(jobParameters.contains("componenteUrbanCode")) {
					if ( !StringUtil.isNull(compoUrbanValue) ) {
						println("[INFO]: Se añada a la ficha el componente UrbanCode: ${compoUrbanValue}");
						components.add(["${compoUrbanValue}": "${it.baselines.name}".replace(']','').replace('[','')])
					} else
						println("[WARNING]: El componente \"${it.name}\" tiene el parámetro UrbanCode a vacío o no existe, no irá a la ficha")
				}
				
				if(documentValue != null) {
					if(documentValue.toString().trim().equals("true")) {
						println("El componente ${it.name} genera ficha despluegue para la documentación...")						
						components.add(["${compoUrbanValue}.doc":"${it.baselines.name}".replace(']','').replace('[','')]);
						
						try {
							// Inclusión de la versión (CreateVersion) para la parte de doc
							def componenteUrbanCode = compoUrbanValue + ".doc"
							def versionUrbanCode = "${it.baselines.name}".replace(']','').replace('[','').replace(',','')
							if(urbanConnect == "true") {
								println("Generando version para ${componenteUrbanCode} con version ${versionUrbanCode} en Urban...")
								UrbanCodeComponentVersion componentVersion = 
										new UrbanCodeComponentVersion(componenteUrbanCode, versionUrbanCode, null, null)
								// Crearla sobre Urban Code
								def json = exe.createVersion(componentVersion)
								println json
							}
						} catch (Exception e) {
							// Los errores en este comando son frecuentes, y se les hace caso omiso 
							println "WARNING: ${e.getMessage()}"
							println "Probablemente la versión existe ya en Urban Code"
						}
					}
				}
			}

			def jsonComplete = JsonOutput.toJson(["name": "${instantanea}", "application": "${application}" ,
				"description": "Snapshot Urban Code", "versions" : components])
			
			println ""
			println(jsonComplete) // Json de UrbanCode completo
			println ""

			// Se sube el nuevo descriptor al Nexus.
			File jsonFile = new File("descriptor.json");
			jsonFile.text = jsonComplete;

			TmpDir.tmp { tmpDir ->
				File tmp = new File(tmpDir, "descriptor.json")
				tmp.text = jsonFile.text
				File zip = ZipHelper.addDirToArchive(tmpDir);
				try {
					println "Subiendo el artifactId a la ruta ${groupIdUrbanCode}:${artifactId}:${instantanea} desde ${zip.getCanonicalPath()}"
					NexusHelper.uploadTarNexus(nexusUser, nexusPass, gradleBin, cScriptsStore, "fichas_despliegue", groupIdUrbanCode, artifactId, "${instantanea}", urlNexus, "true", zip.getCanonicalPath(), "zip", {println it});
					println "Subido del descriptor completo a Nexus."
				} catch (Exception e) {
					println("[WARNING]: Ha habido un problmema subiendo el descriptor completo a Nexus:");
					e.printStackTrace();
				}
				finally {
					zip.delete()
				}
			}

			jsonFile.delete();

			objCompleteSnapshot = UrbanCodeSnapshot.parseJSON(jsonComplete);
			if(urbanConnect == "true") {
				try {
					exe.createSnapshot(objCompleteSnapshot);
					if (entorno != null && !entorno.trim().equals("")) {
						println("Parámetro \"entorno\" válido. Se lanza la instantánea en el entorno indicado...")
						// lanzamiento de las instantáneas en el entorno indicado
						UrbanCodeApplicationProcess process = 
							new UrbanCodeApplicationProcess(objCompleteSnapshot, 
								Constants.DEPLOY_PROCESS, entorno, false);
						exe.requestApplicationProcess(process);
					}
				} catch (Exception e) {
					
					List snapshotCompsList = [];
					// Componer la lista de los componentes incluidos en la snapshot
					objCompleteSnapshot.getVersions().each { Map version ->
						version.keySet().each { String key ->
							snapshotCompsList << "$key:$key"
						}
					}
					println("[WARNING] No se ha podido crear la snapshot en UrbanCode. Revisar la configuración.")
					def composNotUrban = chk.
						checkComposInUrban(
							urbExe, 
							application, 
							snapshotCompsList);
					if(composNotUrban.size() > 0) {
						println("[ERROR] Componentes no dados de alta en UrbanCode:");
						composNotUrban.each {
							println ("\"${it}\"");
						}
					}
					throw e;
				}
			}
		}
	} else {
		println("No existe el streamTarget \"${streamTarget}\". No se generará el descriptor completo.")
	}

	return objCompleteSnapshot;
}


/**
 * Devuelve el json con las baselines de cada componente segun el snapshot formado de
 * conjuntar streamTarget e instantaea.
 * @param streamTarget
 * @param instantanea
 * @param rtcUser
 * @param rtcPass
 * @param rtcUrl
 * @return (String) baselinesJson
 */
private String returnBaseLinesJson(streamTarget, instantanea, rtcUser, rtcPass, rtcUrl) {
	def baselinesJson = null;
	def scm = new ScmCommand(ScmCommand.Commands.SCM);
	//scm.initLogger { println it }
	def rtc_snapshot = streamTarget + " - " + instantanea;
	def command = "list baselines -s \"${rtc_snapshot}\" -j"

	TmpDir.tmp { File baseDir ->
		baselinesJson = scm.ejecutarComando(command, rtcUser, rtcPass, rtcUrl, baseDir);
	}

	return baselinesJson;
}

/**
 * Crea un fichero donde almacena el descriptor y lo sube a Nexus.
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
private void createJsonFileAndUpload(descriptor, artifactId, instantanea, urlNexus, groupIdUrbanCode, nexusUser, nexusPass, gradleBin, cScriptsStore) {
	File jsonFile = new File("descriptor.json");
	jsonFile.text = descriptor;
	UrbanCodeSnapshot.zipAndUpload(jsonFile, artifactId, instantanea, urlNexus, groupIdUrbanCode, nexusUser, nexusPass, gradleBin, cScriptsStore);
	jsonFile.delete();
}

