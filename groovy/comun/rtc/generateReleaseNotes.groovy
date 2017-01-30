// busca las dos últimas líneas base del usuario JENKINS_RTC para así mostrar los cambios de la release note.
// $JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/rtc/generateReleaseNotes.groovy
import hudson.model.*
import jenkins.model.*
import es.eci.utils.*
import rtc.*
import components.*

String n = "40"

def build = Thread.currentThread().executable
def urlRTC = build.getEnvironment(null).get("urlRTC")
def userRTC= build.getEnvironment(null).get("userRTC") 
def scmToolsHome = build.getEnvironment(null).get("SCMTOOLS_HOME")
def daemonConfigDir = build.getEnvironment(null).get("DAEMONS_HOME")
def resolver = build.buildVariableResolver
def invokerName = resolver.resolve("jobInvoker")
def invoker = Hudson.instance.getJob(invokerName)
def stream = resolver.resolve("streamTarget")
def component = resolver.resolve("component")
File parentWorkspace = new File(resolver.resolve("parentWorkspace"))
def version = resolver.resolve("version")
def pwdRTC = resolver.resolve("pwdRTC") 
def buildInvoker = null
String ultBL = resolver.resolve("lastBaseline")
String pultBL = resolver.resolve("penultimateBaseline")

if (invoker!=null)
	buildInvoker = invoker.getLastBuild()
else
	buildInvoker = build
	
HelperGenerateReleaseNotes help = new HelperGenerateReleaseNotes();
help.initLogger({println it})
help.generateReleaseNotes(n,stream,component,userRTC,pwdRTC,urlRTC,parentWorkspace,scmToolsHome,daemonConfigDir,build,buildInvoker,ultBL,pultBL)

