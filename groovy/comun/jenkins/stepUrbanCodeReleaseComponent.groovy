import hudson.model.AbstractBuild
import urbanCode.UrbanCodeExecutor
import urbanCode.UrbanCodeSnapshot;
import urbanCode.UrbanCodeSnapshotDeployer
import es.eci.utils.JobRootFinder
import es.eci.utils.NexusHelper
import es.eci.utils.ParameterValidator
import es.eci.utils.ParamsHelper
import es.eci.utils.TmpDir
import es.eci.utils.ZipHelper

/**
 * 
 * Debe lanzarse dentro del workflow de release (es decir, POR COMPONENTE).
 * + Recupera el job ancestro superior del que se esté ejecutando (la raíz del 
 * árbol).
 * + En caso de que el ancestro sea un job de componente, concluimos que la
 * release se ha lanzado desde componente, y debemos comunicarnos con Urban.
 * En otro caso, el script termina aquí.
 * + Recupera los parámetros entornoUrbanCode, componenteUrbanCode, 
 * aplicacionUrbanCode y builtVersion del job de componente.  
 * Si faltara alguno, no puede continuar.
 * + Con ambos valores, compone una lista de versiones y componentes y la envía
 * a Urban Code como instantánea '$componente_$builtVersion' para su despliegue
 * 
 */
def build = Thread.currentThread().executable
def resolver = build.buildVariableResolver;

///////////////////////////////////////////////////////////////////////////////
// Contexto del script
// Nexus
String urlNexus = 			build.getEnvironment(null).get("ROOT_NEXUS_URL");
// UC
String urbanCodeCommand = 	build.getEnvironment(null).get("UDCLIENT_COMMAND");
String urlUrbanCode = 		build.getEnvironment(null).get("UDCLIENT_URL");
String userUrbanCode = 		build.getEnvironment(null).get("UDCLIENT_USER");
String pwdUrbanCode = 		resolver.resolve("UDCLIENT_PASS");
// Aplicación y entorno para Urban Code
String urbanCodeApp =		build.getEnvironment(null).get("aplicacionUrbanCode");
String urbanCodeEnv =		build.getEnvironment(null).get("entornoUrbanCode");
String urbanCodeComp =		build.getEnvironment(null).get("componenteUrbanCode");
// ¿Está cortada la conexión con Urban?
String urbanConnection =	build.getEnvironment(null).get("URBAN_CONNECTION");
///////////////////////////////////////////////////////////////////////////////

// Se usa para convertir una cadena en un artifactId apropiado
def cleanArtifactId(String s) {
	return s==null?null:s.replace(" - ", "_").
			 replace(" -", "_").
			 replace("- ", "_").
			 replace(" ", "_");
}

// Dar de alta, si es posible, el zip en Nexus
def publicarSnapshotNexus(
		build,
		String urbanCodeApp, 
		String urbanCodeComp,
		String builtVersion, 
		List<Map<String, String>> componentsVersions) {
	def groupIdUrbanCode = 	build.getEnvironment(null).get("URBAN_GROUP_ID");
	def resolver = build.buildVariableResolver;
	def nexusUser = 		resolver.resolve("DEPLOYMENT_USER");
	def nexusPass = 		resolver.resolve("DEPLOYMENT_PWD");
	def cScriptsStore = 	build.getEnvironment(null).get("C_SCRIPTS_HOME");
	def gradleBin =		    build.getEnvironment(null).get("GRADLE_HOME") + "/bin/gradle";
	def urlNexus = 			build.getEnvironment(null).get("NEXUS_FICHAS_DESPLIEGUE_URL");
	UrbanCodeSnapshot snapshot = new UrbanCodeSnapshot();
	snapshot.setApplication(urbanCodeApp);
	snapshot.setName("${urbanCodeComp}_${builtVersion}");
	snapshot.setDescription("${urbanCodeComp}_${builtVersion}");
	for (Map<String, String> map: componentsVersions) {
		for (String key: map.keySet()) {
			snapshot.addVersion(key, map[key]);
		}
	}
		
	TmpDir.tmp { File tmpDir ->
		File jsonFile = new File(tmpDir, "descriptor.json");
		jsonFile.text = snapshot.toJSON();
		File zip = ZipHelper.addDirToArchive(tmpDir);
		try {
			String artifactId = 
				cleanArtifactId(urbanCodeApp) + "_" + 
					cleanArtifactId(urbanCodeComp);
			println "Subiendo el artifactId a la ruta ${groupIdUrbanCode}:${artifactId}:${builtVersion}_completa desde ${zip.getCanonicalPath()}"
			NexusHelper.uploadTarNexus(
				nexusUser, 
				nexusPass, 
				gradleBin, 
				cScriptsStore, 
				"fichas_despliegue", 
				groupIdUrbanCode, 
				artifactId, 
				builtVersion, 
				urlNexus, 
				"true", 
				zip.getCanonicalPath(), 
				"zip", 
				{println it});
			println "Subida del descriptor de componente a Nexus."
		} catch (Exception e) {
			println("[WARNING]: Ha habido un problmema subiendo el descriptor completo a Nexus:");
			e.printStackTrace();
		}
		finally {
			zip.delete()
		}
	}
}

def isNotNull = { String s ->
	return s != null && s.trim().length() > 0;
}

JobRootFinder finder = new JobRootFinder(build);
finder.initLogger { println it }

AbstractBuild ancestor = finder.getRoot();

println "Identificado el ancestro: $ancestor"

if (ancestor.getProject().getName().contains("-COMP-")) {
	String builtVersion = ParamsHelper.getParam(ancestor, "builtVersion");
	boolean validParameters = true;
	try {
		println "El job ha sido lanzado desde componente, preparando despliegue..."
		ParameterValidator.builder().
			add("urbanCodeApp", urbanCodeApp, isNotNull).
			add("urbanCodeComp", urbanCodeComp, isNotNull).
			add("builtVersion", builtVersion, isNotNull).
				build().validate();
	}
	catch(Exception e) {
		println e.getMessage();
		// No son válidos
		validParameters = false;	
	}
	if (validParameters) {
		// Versiones
		List<Map<String, String>> componentsVersions = [];
		Map<String, String> tmp = [:];
		tmp.put(urbanCodeComp, builtVersion);
		componentsVersions << tmp;
		// Dar de alta el json zipeado en nexus
		publicarSnapshotNexus(
			build, 
			urbanCodeApp, 
			urbanCodeComp, 
			builtVersion, 
			componentsVersions);
		if (!Boolean.valueOf(urbanConnection)) {
			println "Se ha desconectado la conexión con Urban Code, no se ejecuta el despliegue"
		}
		else {
			// Si los parámetros están todos definidos, lanzar el despliegue
			UrbanCodeExecutor exec = new UrbanCodeExecutor(urbanCodeCommand,
								urlUrbanCode,
								userUrbanCode,
								pwdUrbanCode);
			exec.initLogger { println it };
			UrbanCodeSnapshotDeployer deploy = new UrbanCodeSnapshotDeployer(exec, urlNexus);
			deploy.initLogger { println it };
			deploy.deploySnapshotVersions(
				componentsVersions,
				urbanCodeApp,
				urbanCodeEnv,
				"${urbanCodeComp}_${builtVersion}");
		}
	}
	else {
		println "Falta algún parámetro necesario para desplegar en Urban, no se ejecuta el despliegue"
	}
	
}
else {
	println "El job no ha sido lanzado desde componente, no se lanza despliegue contra Urban Code"
}