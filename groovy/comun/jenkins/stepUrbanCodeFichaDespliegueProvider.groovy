/**
 * Llama a la clase UrbanCodeFichaDespliegue informando si ha sido ejecutado desde corriente o
 * desde componente, 
 */

import es.eci.utils.GlobalVars
import es.eci.utils.JobRootFinder
import es.eci.utils.ParamsHelper
import hudson.model.AbstractBuild
import hudson.model.Result
import urbanCode.UrbanCodeFichaDespliegue
import urbanCode.UrbanCodeGenerateJsonDescriptor;

def build = Thread.currentThread().executable
def resolver = build.buildVariableResolver;

///////////////////////////////////////////////////////////////////////////////
// Nexus
String urlNexus =			build.getEnvironment(null).get("ROOT_NEXUS_URL");
String urlNexusDeploy =		build.getEnvironment(null).get("NEXUS_FICHAS_DESPLIEGUE_URL");
// UC
String udClientCommand =	build.getEnvironment(null).get("UDCLIENT_COMMAND");
String urlUrbanCode =		build.getEnvironment(null).get("UDCLIENT_URL");
String urbanUser =			build.getEnvironment(null).get("UDCLIENT_USER");
String urbanPassword =		resolver.resolve("UDCLIENT_PASS");
// Clase - Lanzamiento desde corriente
String urbanCodeApp =		build.getEnvironment(null).get("aplicacionUrbanCode");
String urbanCodeSnapName =	build.getEnvironment(null).get("instantanea");
boolean componentLauch =	false;
String jobInvokerType =		resolver.resolve("jobInvokerType");
String parentWorkspace =	build.getEnvironment(null).get("parentWorkspace");
// Clase - Lanzamiento desde componente
String urbanCodeEnv =		build.getEnvironment(null).get("entornoUrbanCode");
// Conexión con git
String gitCommand =			build.getEnvironment(null).get("GIT_SH_COMMAND");
String gitUser =			build.getEnvironment(null).get("GIT_USER");
String gitHost =			build.getEnvironment(null).get("GIT_HOST");
String gitGroup =			resolver.resolve("gitGroup");
// Clase - Componentes de la release o del deploy de git
String gitReleaseComp =		resolver.resolve("componentsUrban");
String gitDeployComp =		resolver.resolve("finalComponentsList");

///////////////////////////////////////////////////////////////////////////////
// Parámetros para la creación del descriptor

String groupIdUrbanCode =	build.getEnvironment(null).get("URBAN_GROUP_ID");
String maven = 				""
if (System.getProperty('os.name').toLowerCase().contains('windows')) {
	maven =					build.getEnvironment(null).get("MAVEN_HOME") + "\\bin\\mvn.bat";
}
else {
	maven =					build.getEnvironment(null).get("MAVEN_HOME") + "/bin/mvn";
}
String rtcUser = 			build.getEnvironment(null).get("userRTC");
String rtcUrl = 			build.getEnvironment(null).get("urlRTC");

String stream = 			resolver.resolve("stream");
String streamCargaInicial =	resolver.resolve("streamCargaInicial");
String streamTarget = 		resolver.resolve("streamTarget").equals("") ? stream : resolver.resolve("streamTarget");

String rtcPass = 			resolver.resolve("pwdRTC");

// Esta es la variable final que se envia a la clase
String theStream = stream;

if (streamCargaInicial != null && streamCargaInicial.trim().length() > 0) {
	theStream = streamCargaInicial;
}

///////////////////////////////////////////////////////////////////////////////

def isNull = { String s ->
	return s == null || s.trim().length() == 0;
}

def urbanConnect = 			build.getEnvironment(null).get("URBAN_CONNECTION");
def urbanConnLocal = 		resolver.resolve("URBAN_CONNECTION");
urbanConnect = (urbanConnLocal == null || (urbanConnLocal != "true" && urbanConnLocal != "false")) ? urbanConnect : urbanConnLocal;

///////////////////////////////////////////////////////////////////////////////

JobRootFinder finder = new JobRootFinder(build);
finder.initLogger { println it }

AbstractBuild ancestor = finder.getRoot();

// Si se ha lanzado desde componente
if (ancestor.getProject().getName().contains("-COMP-")) {
	// Informa a UrbanCodeFichaDespliegue de que viene de componente
	componentLauch = true
}

println "jobInvokerType: " + jobInvokerType
println "componentLauch: " + componentLauch

///////////////////////////////////////////////////////////////////////////////

// Si ha terminado ok, continuamos
if (ancestor.getResult() == Result.SUCCESS) {
	
	if ( (componentLauch && jobInvokerType.equals("components")) || (!componentLauch && jobInvokerType.equals("streams")) ) {
		
		// Generamos el descriptor
		UrbanCodeGenerateJsonDescriptor generateJsonDesc = new UrbanCodeGenerateJsonDescriptor();
			
		generateJsonDesc.setUrlNexus(urlNexus)
		generateJsonDesc.setMaven(maven)
		generateJsonDesc.setParentWorkspace(parentWorkspace)
		generateJsonDesc.setNombreAplicacionUrban(urbanCodeApp)
		generateJsonDesc.setInstantaneaUrban(urbanCodeSnapName)
		generateJsonDesc.setGroupIdUrbanCode(groupIdUrbanCode)
		generateJsonDesc.setStreamTarget(streamTarget)
		generateJsonDesc.setStream(theStream)
		generateJsonDesc.setRtcPass(rtcPass)
		generateJsonDesc.setRtcUser(rtcUser)
		generateJsonDesc.setRtcUrl(rtcUrl)
		generateJsonDesc.setGitCommand(gitCommand)
		generateJsonDesc.setGitUser(gitUser)
		generateJsonDesc.setGitHost(gitHost)
		generateJsonDesc.setGitGroup(gitGroup)
		generateJsonDesc.setGitReleaseComp(gitReleaseComp)
		generateJsonDesc.setGitDeployComp(gitDeployComp)
			
		generateJsonDesc.initLogger { println it }
				
		String descriptor = generateJsonDesc.execute();
			
		///////////////////////////////////////////////////////////////////////////////
			
		// Test de variables disponibles en Jenkins
		if (isNull(udClientCommand) || isNull(urlUrbanCode) || isNull(urbanUser) ||
			isNull(urbanPassword) || isNull(urlNexusDeploy)) {
				println "### ERROR: Variables de entorno insuficientes"
				build.setResult(Result.FAILURE)
		} else if (urbanConnect == "true") {
	
			// Ejecución de la lógica de Urban
			UrbanCodeFichaDespliegue urbanExecutor = new UrbanCodeFichaDespliegue();
			urbanExecutor.setUrlNexus(urlNexusDeploy)
			urbanExecutor.setUdClientCommand(udClientCommand)
			urbanExecutor.setUrlUrbanCode(urlUrbanCode)
			urbanExecutor.setUrbanUser(urbanUser)
			urbanExecutor.setUrbanPassword(urbanPassword)
			urbanExecutor.setDescriptor(descriptor)
			urbanExecutor.setNombreAplicacionUrban(urbanCodeApp)
			urbanExecutor.setInstantaneaUrban(urbanCodeSnapName)
				
			urbanExecutor.setComponentLauch(componentLauch)
			urbanExecutor.setEntornoUrban(urbanCodeEnv)
			
			urbanExecutor.initLogger { println it }
			urbanExecutor.execute()
			
		} else {
			println "--- INFO: La conexión contra Urban está desactivada."
		}
		
	} else {
		println "--- INFO: El tipo de invocación no coincide, no hay acciones hacia Urbancode."
	}
	
} else {
	println "--- INFO: El proceso no ha finalizado correctamente, no hay acciones hacia Urbancode."
}