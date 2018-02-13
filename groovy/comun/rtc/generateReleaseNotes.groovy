package rtc

// busca las dos últimas líneas base del usuario JENKINS_RTC para así mostrar los cambios de la release note.
// $JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/rtc/generateReleaseNotes.groovy
import hudson.model.*
import jenkins.model.*
import es.eci.utils.*
import components.*

String n = "40"

def urlRTC = build.getEnvironment(null).get("urlRTC")
def userRTC= build.getEnvironment(null).get("userRTC") 
def scmToolsHome = build.getEnvironment(null).get("SCMTOOLS_HOME")
def daemonConfigDir = build.getEnvironment(null).get("DAEMONS_HOME")
def invokerName = build.buildVariableResolver.resolve("jobInvoker")
def invoker = Hudson.instance.getJob(invokerName)
def stream = build.buildVariableResolver.resolve("streamTarget")
def component = build.buildVariableResolver.resolve("component")
File parentWorkspace = new File(build.buildVariableResolver.resolve("parentWorkspace"))
def version = build.buildVariableResolver.resolve("version")
def pwdRTC = build.buildVariableResolver.resolve("pwdRTC") 
def buildInvoker = null
String ultBL = build.buildVariableResolver.resolve("lastBaseline")
String pultBL = build.buildVariableResolver.resolve("penultimateBaseline")

if (invoker!=null)
	buildInvoker = invoker.getLastBuild()
else
	buildInvoker = build
	
HelperGenerateReleaseNotes help = new HelperGenerateReleaseNotes();
help.initLogger({println it})
help.generateReleaseNotes(n,stream,component,userRTC,pwdRTC,urlRTC,parentWorkspace,scmToolsHome,daemonConfigDir,build,buildInvoker,ultBL,pultBL)

