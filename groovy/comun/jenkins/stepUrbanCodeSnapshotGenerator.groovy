import es.eci.utils.Utiles;
import hudson.model.*
import urbanCode.UrbanCodeSnapshotGenerator;

/**
 * Este script se invoca POR COMPONENTE para actualizar el json de la instantánea
 * actual sobre nexus
 * Parámetros
 * stream - Nombre de la aplicación Urban Code
 * component - Nombre del componente Urban Code
 * snapshot - Identificador de instantánea
 * folder - Directorio de la máquina master jenkins donde encontrar el version.txt
 * nexusURL - URL del repositorio de nexus
 * maven - Ruta de maven home
 * grupoUrbanCode - groupId nexus asociado a la aplicación Urban Code
 * apiNexus URL de la api de resolución de versiones de Nexus
 * snapshotsRepository Nombre del repositorio de snapshots en Nexus
 * trivial Si es cierto, se fuerza la aplicación de una estrategia trivial (versión
 * en el version.txt, sin resolución de -SNAPSHOT, componente parametrizado)
 */

def build = Thread.currentThread().executable;
def resolver = build.buildVariableResolver;

def appUrbanCode = 			resolver.resolve("aplicacionUrbanCode");
def componentRealName = 	resolver.resolve("component");
def componenteUrbanCode = 	resolver.resolve("componenteUrbanCode");
def snapshot = 				resolver.resolve("instantanea");
def folder = 				resolver.resolve("parentWorkspace");
def nexusURL = 				build.getEnvironment(null).get("NEXUS_FICHAS_DESPLIEGUE_URL");
def maven = 				build.getEnvironment(null).get("MAVEN_HOME") + "/bin/mvn";
def grupoUrbanCode = 		build.getEnvironment(null).get("URBAN_GROUP_ID");
def apiNexus = 				build.getEnvironment(null).get("MAVEN_RESOLVE");
def snapshotsRepository = 	build.getEnvironment(null).get("MAVEN_SNAPSHOTS_REPOSITORY_NAME");
def stream = 				resolver.resolve("stream");
def homeStream = 			resolver.resolve("homeStream");
def nexusUser = 			resolver.resolve("DEPLOYMENT_USER");
def nexusPass = 			resolver.resolve("DEPLOYMENT_PWD");
def gradleBin =		    	build.getEnvironment(null).get("GRADLE_HOME") + "/bin/gradle"; 
def cScriptsStore = 		build.getEnvironment(null).get("C_SCRIPTS_HOME");
def instantanea = 			resolver.resolve("instantanea"); 
def urlNexus = 				build.getEnvironment(null).get("NEXUS_FICHAS_DESPLIEGUE_URL"); 
def application = 			resolver.resolve("aplicacionUrbanCode");

def trivial = 				resolver.resolve("trivial");
if(trivial == null) {
	trivial = true;
} else {
	trivial = Utiles.toBoolean(trivial);
}

/**
 * Comienzo del script que va actualizando el descriptor parcial con la información de cada componente.
 */
try {
	println("/nParámetros de entrada:")
	println("appUrbanCode = ${appUrbanCode}")
	println("componentName = ${componentRealName}")
	println("componenteUrbanCode = ${componenteUrbanCode}")
	println("snapshot = ${snapshot}")
	println("folder = ${folder}")
	println("nexusURL = ${nexusURL}")
	println("maven = ${maven}")
	println("grupoUrbanCode = ${grupoUrbanCode}")
	println("apiNexus = ${apiNexus}")
	println("stream = ${stream}")
	println("snapshotsRepository = ${snapshotsRepository}")
	println("trivial = ${trivial}\n")
	
	UrbanCodeSnapshotGenerator gen = new UrbanCodeSnapshotGenerator();
	gen.initLogger({ println it });
	gen.snapshot(
			appUrbanCode,
			componenteUrbanCode,
			snapshot,
			new File(folder),
			nexusURL,
			maven,
			grupoUrbanCode,
			apiNexus,
			snapshotsRepository,
			trivial,
			homeStream,
			build,
			nexusUser,
			nexusPass,
			gradleBin,
			cScriptsStore,
			instantanea,
			urlNexus,
			application,
			stream,
			componentRealName);
} catch(Exception e) {
	println "ERROR: ${e.getMessage()}"
	e.printStackTrace()
	throw e
}
