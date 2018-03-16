package jenkins

/**
 * Llama a la clase UrbanCodeFichaDespliegue informando si ha sido ejecutado desde corriente o
 * desde componente, 
 */

import buildtree.BuildBean
import buildtree.BuildTreeHelper
import es.eci.utils.JobRootFinder
import es.eci.utils.ParamsHelper
import es.eci.utils.StringUtil
import groovy.json.JsonSlurper
import hudson.model.AbstractBuild
import hudson.model.Result
import release.GetReleaseInfo
import urbanCode.UrbanCodeFichaDespliegue
import urbanCode.UrbanCodeGenerateJsonDescriptor

///////////////////////////////////////////////////////////////////////////////
// Clarive
String permisoClarive = build.buildVariableResolver.resolve("permisoClarive");

// Nexus
String urlNexusDeploy =		build.getEnvironment(null).get("NEXUS_FICHAS_DESPLIEGUE_URL");
String uDeployUser =		build.buildVariableResolver.resolve("DEPLOYMENT_USER");
String uDeployPass =		build.buildVariableResolver.resolve("DEPLOYMENT_PWD");
// UC
String udClientCommand =	build.getEnvironment(null).get("UDCLIENT_COMMAND");
String urlUrbanCode =		build.getEnvironment(null).get("UDCLIENT_URL");
String urbanUser =			build.getEnvironment(null).get("UDCLIENT_USER");
String urbanCodeEnv =		build.getEnvironment(null).get("entornoUrbanCode");
String urbanPassword =		build.buildVariableResolver.resolve("UDCLIENT_PASS");
boolean serviceStop =		false;
if (build.buildVariableResolver.resolve("serviceStop") != null) {
	serviceStop = build.buildVariableResolver.resolve("serviceStop");
}
// Clase - Lanzamiento desde corriente
String urbanCodeApp =		build.getEnvironment(null).get("aplicacionUrbanCode");
String urbanCodeSnapName =	build.getEnvironment(null).get("instantanea");
String nuevaInstantanea =	build.getEnvironment(null).get("nuevaInstantanea");
String jobInvokerType =		build.buildVariableResolver.resolve("jobInvokerType");
String jobAction =			build.getEnvironment(null).get("action");
String parentWorkspace =	build.getEnvironment(null).get("parentWorkspace");
String systemWorkspace =	build.getEnvironment(null).get("WORKSPACE");
String scmComponentsList = 	build.getEnvironment(null).get("scmComponentsList");

// Clase - Lanzamiento desde componente
boolean componentLauch =	false;
// Conexión con git
String gitCommand =			build.getEnvironment(null).get("GIT_SH_COMMAND");
String gitUser =			build.getEnvironment(null).get("GIT_USER");
String gitHost =			build.getEnvironment(null).get("GIT_HOST");
String gitGroup =			build.buildVariableResolver.resolve("gitGroup");
String targetBranch =		build.buildVariableResolver.resolve("targetBranch");

// Mail
String managersMail = 		build.getEnvironment(null).get("managersMail");

///////////////////////////////////////////////////////////////////////////////
// ¿Se está haciendo RELEASE_MANAGEMENT?
// En caso afirmativo, se debe informar en el workItem de RTC (caso de estar en 
//	RTC) un comentario indicando que se ha construido 

boolean releaseManagement = false;
if (build.getEnvironment(null).get("RELEASE_MANAGEMENT") != null) {
	releaseManagement = Boolean.parseBoolean(build.getEnvironment(null).get("RELEASE_MANAGEMENT"));
}

String releaseId = 			build.getEnvironment(null).get("workItem");

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

String stream = 			build.buildVariableResolver.resolve("stream");
String streamCargaInicial =	build.buildVariableResolver.resolve("streamCargaInicial");
String streamTarget = 		build.buildVariableResolver.resolve("streamTarget").equals("") ? stream : build.buildVariableResolver.resolve("streamTarget");

String rtcPass = 			build.buildVariableResolver.resolve("pwdRTC");
String rtcKeystore = 		build.getEnvironment(null).get("RTC_KEYSTORE_VERSION");


boolean sendMail = false;

// Esta es la variable final que se envia a la clase
String theStream = stream;

if (streamCargaInicial != null && streamCargaInicial.trim().length() > 0) {
	theStream = streamCargaInicial;
}

// Lanza la ficha aunque el proceso padre no haya acabado correctamente
boolean forceLaunch =		false;
if (build.buildVariableResolver.resolve("forceLaunch") != null) {
	forceLaunch = Boolean.valueOf(build.buildVariableResolver.resolve("forceLaunch"));
}

///////////////////////////////////////////////////////////////////////////////

def isNull = { String s ->
	return s == null || s.trim().length() == 0;
}

def urbanConnect = 			build.getEnvironment(null).get("URBAN_CONNECTION");
def urbanConnLocal = 		build.buildVariableResolver.resolve("URBAN_CONNECTION");
urbanConnect = (urbanConnLocal == null || (urbanConnLocal != "true" && urbanConnLocal != "false")) ? urbanConnect : urbanConnLocal;

///////////////////////////////////////////////////////////////////////////////

JobRootFinder finder = new JobRootFinder();
finder.initLogger { println it }

AbstractBuild ancestor = finder.getRootBuild(build);

// Si se ha lanzado desde componente
if (ancestor.getProject().getName().contains("-COMP-")) {
	// Informa a UrbanCodeFichaDespliegue de que viene de componente
	componentLauch = true
}

println "---------------------------------------------"
println "jobInvokerType: " + jobInvokerType
println "componentLauch: " + componentLauch
println "jobAction: " + jobAction
println "forceLaunch: " + forceLaunch
println "---------------------------------------------"

// Si lanzamos un deploy, cargamos el árbol de lanzamiento y nos lo llevamos a
// UrbanCodeGenerateJsonDescriptor para parsearlo y obtener los builtVersion
List<BuildBean> beanListTree = null
if (jobAction == "deploy") {
	BuildTreeHelper btHelperExecute = new BuildTreeHelper()
	beanListTree = btHelperExecute.executionTree(ancestor);
}

///////////////////////////////////////////////////////////////////////////////

// Si ha terminado ok, o es un lanzamiento manual, continuamos
if ( (ancestor.getResult() == Result.SUCCESS) || (forceLaunch) ) {
	
	// Si viene desde corriente o es un lanzamiento manual, continuamos
	if ( (!componentLauch && jobInvokerType.equals("streams")) || (forceLaunch) ) {
		
		sendMail = true;
		
		// Generamos el descriptor
		UrbanCodeGenerateJsonDescriptor generateJsonDesc = new UrbanCodeGenerateJsonDescriptor();
			
		generateJsonDesc.setUrlNexus(urlNexusDeploy)
		generateJsonDesc.setuDeployUser(uDeployUser)
		generateJsonDesc.setuDeployPass(uDeployPass)
		generateJsonDesc.setMaven(maven)
		generateJsonDesc.setParentWorkspace(parentWorkspace)
		generateJsonDesc.setSystemWorkspace(systemWorkspace)
		generateJsonDesc.setUdClientCommand(udClientCommand)
		generateJsonDesc.setUrlUrbanCode(urlUrbanCode)
		generateJsonDesc.setUrbanUser(urbanUser)
		generateJsonDesc.setUrbanPassword(urbanPassword)
		generateJsonDesc.setNombreAplicacionUrban(urbanCodeApp)
		generateJsonDesc.setInstantaneaUrban(urbanCodeSnapName)
		generateJsonDesc.setNuevaInstantaneaUrban(nuevaInstantanea)
		generateJsonDesc.setGroupIdUrbanCode(groupIdUrbanCode)
		generateJsonDesc.setActionDescUrban(jobAction)
		generateJsonDesc.setStreamTarget(streamTarget)
		generateJsonDesc.setStream(theStream)
		generateJsonDesc.setStreamFicha(stream)
		generateJsonDesc.setRtcPass(rtcPass)
		generateJsonDesc.setRtcUser(rtcUser)
		generateJsonDesc.setRtcUrl(rtcUrl)
		generateJsonDesc.setGitCommand(gitCommand)
		generateJsonDesc.setGitUser(gitUser)
		generateJsonDesc.setGitHost(gitHost)
		generateJsonDesc.setGitGroup(gitGroup)
		generateJsonDesc.setTargetBranch(targetBranch)
		generateJsonDesc.setBeanListTree(beanListTree)
		generateJsonDesc.setForceLaunch(forceLaunch)
		generateJsonDesc.setManagersMail(managersMail)
		generateJsonDesc.setScmComponentsList(scmComponentsList)
		if (releaseManagement && !StringUtil.isNull(releaseId)) {
			println "Gestión de release"
			try {
				File f = new File(parentWorkspace, GetReleaseInfo.RELEASE_INFO_FILE_NAME);
				if (f.exists()) {
					def releaseInfo =
						new JsonSlurper().parseText(f.text);
					generateJsonDesc.setReleaseId(releaseInfo.releaseId);
					generateJsonDesc.setRemedyRequest(releaseInfo.remedyRequest);
				}
				else {
					log "[WARNING] No hay información de release"
				}
			}
			catch(Exception e) {
				
			}
		}
		else {
			println "[WARNING] Se salta la gestión de releases al estar desactivada"
		}
			
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
			urbanExecutor.setPermisoClarive(permisoClarive)

			urbanExecutor.setEntornoUrban(urbanCodeEnv)
			urbanExecutor.setServiceStop(serviceStop)
			
			urbanExecutor.initLogger { println it }
			urbanExecutor.execute()
			
		} else {
			println "--- INFO: La conexión contra Urban está desactivada."
		}
		
	} else {
		println "--- INFO: El tipo de invocación no coincide, no hay acciones hacia Urbancode."
		build.setResult(Result.NOT_BUILT)
	}
	
} else {
	println "--- INFO: El proceso no ha finalizado correctamente, no hay acciones hacia Urbancode."
	build.setResult(Result.NOT_BUILT)
}


ParamsHelper.addParams(build, ["sendMail": Boolean.toString(sendMail)])
