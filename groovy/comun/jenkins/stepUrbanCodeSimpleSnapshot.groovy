import es.eci.utils.NexusHelper;
import es.eci.utils.TmpDir;
import urbanCode.UrbanCodeExecutor
import urbanCode.UrbanCodeSnapshot

/**
 * Este script se invoca DESDE EL JOB DE LA CORRIENTE para crear la instantánea
 * en Urban Code y además lanzarla contra un determinado entorno
 * Parámetros
 * udclient - Ruta del cliente udclient
 * urlUrbanCode - URL de udeploy
 * user - Usuario Urban Code
 * password - Password del usuario Urban Code
 * snapshot - Instantánea Urban Code
 * jsonDescriptor - JSON de instantánea válido de Urban Code
 * groupIdUrbanCode - groupId Nexus asociado a la aplicación Urban Code 
 * urlNexus - URL de nexus 
 */

def build = Thread.currentThread().executable;
def resolver = build.buildVariableResolver;

def udClientCommand = 	build.getEnvironment(null).get("UDCLIENT_COMMAND"); 			println("udClientCommand -> " + udClientCommand);
def urlUrbanCode = 		build.getEnvironment(null).get("UDCLIENT_URL"); 				println("urlUrbanCode -> " + urlUrbanCode);
def user = 				build.getEnvironment(null).get("UDCLIENT_USER"); 				println("user -> " + user);
def groupIdUrbanCode = 	build.getEnvironment(null).get("URBAN_GROUP_ID"); 				println("groupIdUrbanCode -> " + groupIdUrbanCode);
def urlNexus = 			build.getEnvironment(null).get("NEXUS_FICHAS_DESPLIEGUE_URL"); 	println("urlNexus -> " + urlNexus);
def cScriptsStore = 	build.getEnvironment(null).get("C_SCRIPTS_HOME"); 				println("cScriptsStore -> " + cScriptsStore);
def gradleBin =		    build.getEnvironment(null).get("GRADLE_HOME") + "/bin/gradle"; 	println("gradleHome -> " + gradleBin);

def application = 		resolver.resolve("aplicacionUrbanCode"); 		println("application -> " + application);
def password = 			resolver.resolve("UDCLIENT_PASS"); 				println("password -> " + password);
def instantanea = 		resolver.resolve("instantanea"); 				println("instantanea -> " + instantanea);
def descriptor = 		resolver.resolve("descriptor"); 				println("descriptor -> " + descriptor);
def nexusUser = 		resolver.resolve("DEPLOYMENT_USER"); 			println("nexusUser -> " + nexusUser);
def nexusPass = 		resolver.resolve("DEPLOYMENT_PWD"); 			println("nexusPass -> " + nexusPass);

try {
	UrbanCodeExecutor exe = new UrbanCodeExecutor(udClientCommand, urlUrbanCode, user, password);
	exe.initLogger({ println it });
	
	// Comunicación con urban
	exe.createSnapshot(UrbanCodeSnapshot.parseJSON(descriptor));
	
	if (groupIdUrbanCode != null || groupIdUrbanCode.trim().length() == 0) {
		// Comunicación con nexus
		TmpDir.tmp { File tmpDir ->
			File tmp = new File(tmpDir, "descriptor.json");
			tmp.text = descriptor;
			String artifactId = application.replace(" - ", "_").
				replace(" -", "_").replace("- ", "_").replace(" ", "_");
			// Subir a nexus
			UrbanCodeSnapshot.zipAndUpload(tmp, artifactId, 
				instantanea, urlNexus, groupIdUrbanCode, nexusUser, 
				nexusPass, gradleBin, cScriptsStore);
		}	
	}
}
catch(Exception e) {
	println "ERROR: ${e.getMessage()}"
	e.printStackTrace()
	throw e
}