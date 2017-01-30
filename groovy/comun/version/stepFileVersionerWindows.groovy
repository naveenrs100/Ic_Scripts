// listado de tecnologías soportadas
// $JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/version/stepFileVersioner.groovy
import groovy.json.*
import groovy.io.FileVisitResult
import es.eci.utils.*

tecnologias = ["maven":"pom\\.xml","gradle":"build\\.gradle"]

//---------------> Variables entrantes
def tecnology = args[0]
def env = System.getenv()
def parentWorkspace =  env['WORKSPACE']
def action = args[2]
def save = args[3]
def checkSnapshot = args[4]
def checkErrors = args[5]
def homeStream = args[6]
def changeVersion = args[7]

//-------------------> Lógica

try{
	VersionUtils utils = new VersionUtils();
	utils.initLogger({ println it });
	if (tecnology==null || tecnology=="")
		tecnology = utils.getTecnology(parentWorkspace)
	utils.changeFileVersion(tecnology,action,parentWorkspace,save,checkSnapshot,checkErrors,homeStream,changeVersion,Boolean.TRUE)
}catch(Exception e){
	println "ERROR: ${e.getMessage()}"
	throw e
}