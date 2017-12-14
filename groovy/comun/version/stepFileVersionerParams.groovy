package version

// listado de tecnologías soportadas
// $JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/version/stepFileVersioner.groovy
import groovy.json.*
import groovy.io.FileVisitResult
import es.eci.utils.*
import es.eci.utils.SystemPropertyBuilder;

tecnologias = ["maven":"pom\\.xml","gradle":"build\\.gradle"]

SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder(); 
def params = parameterBuilder.getSystemParameters();

//---------------> Variables entrantes
def tecnology = params["tecnology"];
def parentWorkspace = params["parentWorkspace"];
def action = params["action"];
def save = params["save"];
def checkSnapshot = params["checkSnapshot"];
def checkErrors = params["checkErrors"];
def homeStream = params["homeStream"];
def changeVersion = params["changeVersion"];

// Lista separada por comas de directorios a obviar
def excepciones = params["tecnology"];


//-------------------> Lógica

try{
	VersionUtils utils = new VersionUtils(excepciones);
	utils.initLogger({ println it });
	if (tecnology==null || tecnology=="")
		tecnology = utils.getTecnology(parentWorkspace)
	utils.changeFileVersion(tecnology,action,parentWorkspace,save,checkSnapshot,checkErrors,homeStream,changeVersion,null)
}catch(Exception e){
	println "ERROR: ${e.getMessage()}"
	throw e
}