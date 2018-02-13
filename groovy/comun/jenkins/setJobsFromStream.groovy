package jenkins

import antlr.StringUtils;
import components.MavenComponent;
import es.eci.utils.StringUtil
import es.eci.utils.jenkins.GetJobsUtils
import es.eci.utils.jenkins.RTCWorkspaceHelper
import groovy.io.*
import groovy.json.*
import hudson.model.*
import es.eci.utils.TargetFoundException

// Por defecto se arrastran dependencias.
def arrastrarDepParam = build.buildVariableResolver.resolve("arrastrarDependencias");
def arrastrarDependencias = (arrastrarDepParam == null || arrastrarDepParam.trim().equals("")) ? "true" : arrastrarDepParam;

// Se determina si es un proyecto Git o RTC
def stream = build.buildVariableResolver.resolve("stream");
def jobsFromPlugin = build.buildVariableResolver.resolve("jobs");
def streamTarget = build.buildVariableResolver.resolve("streamTarget");
def gitGroup = build.buildVariableResolver.resolve("gitGroup");
def streamCargaInicial = build.buildVariableResolver.resolve("streamCargaInicial");
def action = build.buildVariableResolver.resolve("action");
def onlyChanges = build.buildVariableResolver.resolve("onlyChanges");
def todos_o_ninguno = build.buildVariableResolver.resolve("todos_o_ninguno");
def workspaceRTC = RTCWorkspaceHelper.getWorkspaceRTC(action, stream);
def jenkinsHome	= build.getEnvironment(null).get("JENKINS_HOME");
def scmToolsHome = build.getEnvironment(null).get("SCMTOOLS_HOME");
def daemonsConfigDir = build.getEnvironment(null).get("DAEMONS_HOME");
def userRTC = build.getEnvironment(null).get("userRTC");
def pwdRTC = build.buildVariableResolver.resolve("pwdRTC");
def urlRTC = build.getEnvironment(null).get("urlRTC");
def parentWorkspace = build.workspace.toString();
String componentesRelease = build.buildVariableResolver.resolve("componentesRelease");
def getOrdered = 
	(build.buildVariableResolver.resolve("getOrdered") != null) && (!build.buildVariableResolver.resolve("getOrdered").trim().equals("")) ? build.buildVariableResolver.resolve("getOrdered") : "false";

// Parámetros que viene de Git. Ir limp
def branch = build.buildVariableResolver.resolve("originBranch");
def commitsId = build.buildVariableResolver.resolve("commitsId");
def gitHost = build.getEnvironment(null).get("GIT_HOST");
def keystoreVersion = build.getEnvironment(null).get("GITLAB_KEYSTORE_VERSION");
def privateGitLabToken = build.getEnvironment(null).get("GITLAB_PRIVATE_TOKEN");
def technology = build.buildVariableResolver.resolve("technology");
def urlGitlab = build.getEnvironment(null).get("GIT_URL");
def urlNexus = build.getEnvironment(null).get("MAVEN_REPOSITORY");
def lastUserIC = build.getEnvironment(null).get("userGit");
def gitCommand = build.getEnvironment(null).get("GIT_SH_COMMAND");
def mavenHome = build.getEnvironment(null).get("MAVEN_HOME");

// Parámetros que pueden venir del plugin DetectChanges
def paramScmComponentsList = build.buildVariableResolver.resolve("scmComponentsList");
def paramFinalComponentsList = build.buildVariableResolver.resolve("finalComponentsList");

// True si se desea que los jobs se lancen secuencialmente. "jobs" ha de ser entonces
// una lista de sublistas con un solo job por sublista.
def forzarSecuencial = build.buildVariableResolver.resolve("forzarSecuencial");

// Naturaleza del SCM del proyecto
def projectNature;
if(gitGroup != null && !gitGroup.trim().equals("")) {
	projectNature = "git";
} else if(stream != null && !stream.trim().equals("")) {
	projectNature = "rtc";
}

GetJobsUtils gju = new GetJobsUtils(projectNature, action, onlyChanges,
									workspaceRTC, jenkinsHome, scmToolsHome,
									daemonsConfigDir, userRTC, pwdRTC, urlRTC,
									parentWorkspace, commitsId, gitHost, keystoreVersion,
									privateGitLabToken, technology, urlGitlab,
									urlNexus, lastUserIC, gitCommand, mavenHome,
									stream, todos_o_ninguno, getOrdered,
									componentesRelease, gitGroup, branch, streamCargaInicial,
									arrastrarDependencias);

gju.initLogger { println it };

// Revisar si las variables finalComponentsList y scmComponentsList están informadas

/** CÁLCULO DE COMPONENTES INTRODUCIDOS A MANO **/
List<String> listaComponentesRelease = gju.getComponentsReleaseList();
println("\nlistaComponentesRelease -> ${listaComponentesRelease}\n")


// Si scmComponentsList está informada, se salta la lectura de los componetes del SCM

/** CÁLCULO DE COMPONENTES TOTALES QUE CUELGAN DEL GRUPO ADECUADO EN EL SCM (stream ó gitGroup) **/
List<String> scmComponentsList = []

if (StringUtil.notNull(paramScmComponentsList)) {
	scmComponentsList = paramScmComponentsList.split(",");
	println("\nYa viene informado desde plugin: scmComponentsList -> ${scmComponentsList}\n");
}
else {
	scmComponentsList = gju.getScmComponentsList();
	println("\nscmComponentsList -> ${scmComponentsList}\n");
}


// Si finalComponentsList viene informada, se salta la construcción de la lista final de componentes

/** CÁLCULO DE COMPONENTES SI HAY CAMBIOS **/
List<String> thisFinalComponentsList = [];
if (StringUtil.notNull(paramFinalComponentsList)) {
	thisFinalComponentsList = paramFinalComponentsList.split(",");		
	println("\nYa viene informado desde plugin: finalComponentsList -> ${thisFinalComponentsList}\n");
}
else {
	thisFinalComponentsList = gju.getFinalComponentList(scmComponentsList,listaComponentesRelease);
	println("\nfinalComponentsList -> ${thisFinalComponentsList}\n");
}

// Limpieza del finalComponentsList
def scmGroup;
if(StringUtil.notNull(gitGroup)) {
	scmGroup = gitGroup;
} else {
	if(StringUtil.notNull(streamCargaInicial)) {
		scmGroup = streamCargaInicial;
	} else {
		scmGroup = stream;
	}
}
def finalComponentsList = [];
thisFinalComponentsList.each { String compo ->
	def job = Hudson.instance.getJob("${scmGroup} -COMP- ${compo}");
	if(job != null && !job.disabled) {
		finalComponentsList.add(compo);
	} else {
		println("[WARNING] El job \"${scmGroup} -COMP- ${compo}\" no tiene job activo en Jenkins. No se incluirá en finalComponentsList ");
	}
}


/** CÁLCULO DEL ORDEN DE LOS COMPONENTES (SI ES NECESARIO) Y DE SUS COMPONENTES ARRASTRADOS POR DEPENDENCIAS **/
List<List<String>> jobs = new ArrayList<ArrayList<String>>();
if(jobsFromPlugin == null || jobsFromPlugin.trim().equals("")) { 		
	List<List<MavenComponent>> sortedMavenCompoGroups = gju.getOrderedList(build, finalComponentsList, scmComponentsList);
	println("\nsortedMavenCompoGroups -> ${sortedMavenCompoGroups}\n")
	jobs = gju.getJobsList(finalComponentsList, sortedMavenCompoGroups);
}
else {
	println("El parámetro \"jobs\" ya viene indicado desde el plugin de ordenación.");
	def jobsObject = new JsonSlurper().parseText(jobsFromPlugin);
	jobsObject.each { thisJobsList ->
		ArrayList<String> tmpList = new ArrayList<String>();
		thisJobsList.each { String thisJob ->
			tmpList.add(thisJob);
		}
		jobs.add(tmpList);
	}		
}

/** COMPROBACIÓN DE QUE NO EXISTEN DIRECTORIOS TARGET DE POR MEDIO **/
if(getOrdered == "true") {
	File parentWorkspaceDir = new File(build.workspace.toString());
	def notEmptyTargets = [];
	parentWorkspaceDir.eachFileRecurse { File file ->
		if(file.getName() == "pom.xml") {
			def targetFile = new File(file.getParentFile().getCanonicalPath(),"target")
			if(targetFile.exists()) {
				println(targetFile.getCanonicalPath());
				if(targetFile.list().length > 0) {
					targetFile.deleteDir();
					notEmptyTargets.add(targetFile);
				}
			}
		}
	}
	
	if(notEmptyTargets.size() > 0) {
		String directories = "";
		for(String dir : notEmptyTargets) {
			directories = directories + "\t - ${dir}\n";
		}
		throw new TargetFoundException("\n\n------ [ERROR] Hay directorios target no vacíos subidos a RTC o Git. Límpielos antes de ejecutar otra construcción. " +
										"Son los siguientes:\n ${directories}");
	} else {
		println("##### No se han detectado directorios target con contenido en la descarga.")
	}
}


/** CÁLCULO DE LA LISTA DE JOBS SI SE DESEA FORZAR UN LANZAMIENTO SECUENCIAL **/
if(forzarSecuencial != null && forzarSecuencial.trim().equals("true")) {
	jobs = gju.getSequentialJobs(jobs);	
}


if (jobs!=null) {
	def params = []
	
	// Calculamos si la lista de listas de jobs está vacía.
	boolean emptyJobs = true;
	jobs.each {
		if(it.size() > 0) {
			emptyJobs = false
		}
	}
	def jobsString = JsonOutput.toJson(jobs);
	if(!emptyJobs) {
		params.add(new StringParameterValue("jobs","${jobsString}"))
	} else if(!emptyJobs) {
		params.add(new StringParameterValue("jobs",""))
	}
	
	
	params.add(new StringParameterValue("homeStream","${build.workspace}"));
	if (projectNature.equals("rtc") && action!="release" && action != "addFix" && action != "addHotfix") {
		params.add(new StringParameterValue("streamTarget",stream));
	}
	File fArtifacts = new File(build.workspace.toString() + '/artifacts.json');
	// Así aseguramos que el artifacts.json pasa a los esclavos
	if (fArtifacts.exists()) {
		String artifacts = fArtifacts.text;
		params.add(new StringParameterValue("artifactsFile", artifacts));
		params.add(new StringParameterValue("artifactsJson", artifacts));
	}
	def componentsUrban = gju.getComponentsUrban(scmComponentsList);
	params.add(new StringParameterValue("componentsUrban","${componentsUrban}"));
	
	params.add(new StringParameterValue("finalComponentsList", "${finalComponentsList}".substring(1, "${finalComponentsList}".length()-1)));

	gju.setParams(build,params)
	
	if (emptyJobs) {
		println("La lista de jobs en setJobsFromStream ha resultado ser vacía.");
		build.setResult(Result.NOT_BUILT);
	}
}


