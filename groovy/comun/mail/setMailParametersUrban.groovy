package mail

/**
 * Función que introduce los parametros necesarios para el envío de correos en cuelquier flujo.
 * $JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/mail/setMailParametersUrban.groovy
 */
import es.eci.utils.mail.SetMailParametersUrban
import hudson.model.*

def causa = build.getCause(Cause.UpstreamCause)
def action = build.buildVariableResolver.resolve("action")
def managersMail = build.buildVariableResolver.resolve("managersMail")
def stream = build.buildVariableResolver.resolve("stream")
def gitGroup = build.buildVariableResolver.resolve("gitGroup")
def mailSubject = build.buildVariableResolver.resolve("MAIL_SUBJECT")
def defaultManagersMail = build.getEnvironment(null).get("MANAGERS_MAIL")

def origen = ""

if (stream == null)
	origen = gitGroup
else
	origen = stream

println "Causa: " + causa
println "Action: " + action
println "ManagersMail: " + managersMail
println "MailSubject: " + mailSubject
println "origen: " + origen

SetMailParametersUrban mailParams = new SetMailParametersUrban()
mailParams.initLogger( { println it } )
mailParams.setParameters(causa,action,build,managersMail,mailSubject,defaultManagersMail,origen)