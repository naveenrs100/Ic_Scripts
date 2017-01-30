// listado de tecnologías soportadas
// $JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/version/stepFileVersioner.groovy
import groovy.json.*
import groovy.io.FileVisitResult
import es.eci.ic.version.Versioner
import es.eci.ic.version.VersionerFactory

def env = System.getenv()
//---------------> Variables entrantes
def tecnology = args[0]
def parentWorkspace =  env['WORKSPACE']
def action = args[2]
def save = args[3]
def checkSnapshot = args[4]
def checkErrors = args[5]
def homeStream = args[6]
def changeVersion = args[7]
def fullCheck = Boolean.TRUE

//-------------------> Lógica

Versioner versioner = VersionerFactory.getVersioner({ println it },tecnology,action,parentWorkspace,save,checkSnapshot,checkErrors,homeStream,changeVersion,fullCheck)
versioner.write()