import es.eci.utils.jenkins.GetJobsUtils
import es.eci.utils.jenkins.RTCWorkspaceHelper
import groovy.io.*
import groovy.json.*
import hudson.model.*

def build = Thread.currentThread().executable;
def resolver = build.buildVariableResolver;

// Por defecto se arrastran dependencias.
def arrastrarDepParam = resolver.resolve("arrastrarDependencias");
def arrastrarDependencias = (arrastrarDepParam == null || arrastrarDepParam.trim().equals("")) ? "true" : arrastrarDepParam;

// Se determina si es un proyecto Git o RTC
def stream = resolver.resolve("stream");
def streamTarget = resolver.resolve("streamTarget");
def gitGroup = resolver.resolve("gitGroup");
def streamCargaInicial = resolver.resolve("streamCargaInicial");
def action = resolver.resolve("action");
def onlyChanges = resolver.resolve("onlyChanges");
def todos_o_ninguno = resolver.resolve("todos_o_ninguno");
def getOrdered = resolver.resolve("getOrdered");
def workspaceRTC = RTCWorkspaceHelper.getWorkspaceRTC(action, stream);
def jenkinsHome	= build.getEnvironment(null).get("JENKINS_HOME");
def scmToolsHome = build.getEnvironment(null).get("SCMTOOLS_HOME");
def daemonsConfigDir = build.getEnvironment(null).get("DAEMONS_HOME");
def userRTC = build.getEnvironment(null).get("userRTC");
def pwdRTC = resolver.resolve("pwdRTC");
def urlRTC = build.getEnvironment(null).get("urlRTC");
def parentWorkspace = build.workspace.toString();
String componentesRelease = resolver.resolve("componentesRelease");

// Parámetros que viene de Git. Ir limp
def branch = resolver.resolve("originBranch");
def commitsId = resolver.resolve("commitsId");
def gitHost = build.getEnvironment(null).get("GIT_HOST");
def keystoreVersion = build.getEnvironment(null).get("GITLAB_KEYSTORE_VERSION");
def privateGitLabToken = build.getEnvironment(null).get("GITLAB_PRIVATE_TOKEN");
def technology = resolver.resolve("technology");
def urlGitlab = build.getEnvironment(null).get("GIT_URL");
def urlNexus = build.getEnvironment(null).get("MAVEN_REPOSITORY");
def lastUserIC = build.getEnvironment(null).get("userGit");
def gitCommand = build.getEnvironment(null).get("GIT_SH_COMMAND");
def mavenHome = build.getEnvironment(null).get("MAVEN_HOME");

// True si se desea que los jobs se lancen secuencialmente. "jobs" ha de ser entonces
// una lista de sublistas con un solo job por sublista.
def forzarSecuencial = resolver.resolve("forzarSecuencial");

// Naturaleza del SCM del proyecto
def projectNature;
if(gitGroup != null && !gitGroup.trim().equals("")) {
	projectNature = "git";
} else if(stream != null && !stream.trim().equals("")) {
	projectNature = "rtc";
}

GetJobsUtils gju = new GetJobsUtils(build, projectNature, action, onlyChanges,
									workspaceRTC, jenkinsHome, scmToolsHome,
									daemonsConfigDir, userRTC, pwdRTC, urlRTC,
									parentWorkspace, commitsId, gitHost, keystoreVersion,
									privateGitLabToken, technology, urlGitlab,
									urlNexus, lastUserIC, gitCommand, mavenHome,
									stream, todos_o_ninguno, getOrdered,
									componentesRelease, gitGroup, branch, streamCargaInicial,
									arrastrarDependencias);

gju.initLogger { println it };

/** CÁLCULO DE COMPONENTES INTRODUCIDOS A MANO **/
List<String> listaComponentesRelease = gju.getComponentsReleaseList();
println("\nlistaComponentesRelease -> ${listaComponentesRelease}\n")

/** CÁLCULO DE COMPONENTES TOTALES QUE CUELGAN DEL GRUPO ADECUADO EN EL SCM (stream ó gitGroup) **/
List<String> scmComponentsList = gju.getScmComponentsList();
println("\nscmComponentsList -> ${scmComponentsList}\n")

/** CÁLCULO DE COMPONENTES SI HAY CAMBIOS **/
List<String> finalComponentsList = gju.getFinalComponentList(scmComponentsList,listaComponentesRelease);
println("\nfinalComponentsList -> ${finalComponentsList}\n")

/** CÁLCULO DEL ORDEN DE LOS COMPONENTES (SI ES NECESARIO) Y DE SUS COMPONENTES ARRASTRADOS POR DEPENDENCIAS **/
List<List<String>> sortedMavenCompoGroups = gju.getOrderedList(finalComponentsList, scmComponentsList);
println("\nsortedMavenCompoGroups -> ${sortedMavenCompoGroups}\n")

/** CÁLCULO DE LA LISTA DE LISTAS DE JOBS SEGÚN EL REQUERIMIENTO DE ORDENACIÓN **/
List<List<String>> jobs = gju.getJobsList(finalComponentsList, sortedMavenCompoGroups);

/** CÁLCULO DE LA LISTA DE JOBS SI SE DESEA FORZAR UN LANZAMIENTO SECUENCIAL **/
if(forzarSecuencial != null && forzarSecuencial.trim().equals("true")) {
	jobs = gju.getSequentialJobs(jobs);	
}

println("\njobs -> ${jobs}\n")
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
	def componentsUrban = gju.getComponentsUrban(jobsString);
	params.add(new StringParameterValue("componentsUrban","${componentsUrban}"));

	gju.setParams(build,params)
	
	if (emptyJobs) {
		println("La lista de jobs en setJobsFromStream ha resultado ser vacía.");
		build.setResult(Result.NOT_BUILT);
	}
}


