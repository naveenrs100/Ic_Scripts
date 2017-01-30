// listado de tecnologías soportadas
// $JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/version/stepFileVersionerSystem.groovy
/*
 * Es necesario hacer este script por un problema en el con bootloader en jenkins con m�quina virtual IBM
 * loader "org/codehaus/groovy/tools/RootLoader@1bf68002" previously initiated loading for a different type
 * with name "org/xml/sax/Locator" defined by loader "com/ibm/oti/vm/BootstrapClassLoader@44807aef"
java.lang.LinkageError: loading constraint violation: loader "org/codehaus/groovy/tools/RootLoader@1bf68002"
previously initiated loading for a different type with name "org/xml/sax/Locator" defined by
loader "com/ibm/oti/vm/BootstrapClassLoader@44807aef"
 */
import hudson.model.*
import groovy.json.*
import groovy.io.FileVisitResult
import es.eci.utils.*

def build = Thread.currentThread().executable
def resolver = build.buildVariableResolver


//---------------> Variables entrantes
def tecnology = resolver.resolve("tecnology")
def parentWorkspace = resolver.resolve("parentWorkspace")
def action = resolver.resolve("action")
def save = resolver.resolve("save")
def checkSnapshot = resolver.resolve("checkSnapshot")
def checkErrors = resolver.resolve("checkErrors")
def homeStream = resolver.resolve("homeStream")
def changeVersion = resolver.resolve("changeVersion")
def exceptions = resolver.resolve("exceptions")


//-------------------> Lógica

try{
	VersionUtils utils = new VersionUtils(exceptions);
	utils.initLogger({ println it });
	if (tecnology==null || tecnology=="")
		tecnology = utils.getTecnology(parentWorkspace)
	utils.changeFileVersion(tecnology,action,parentWorkspace,save,checkSnapshot,checkErrors,homeStream,changeVersion,null)
}catch(Exception e){
	println "ERROR: ${e.getMessage()}"
	throw e
}