/**
 * Función que introduce los parametros necesarios para el envío de correos en cuelquier flujo.
 * $JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/mail/setMailParameters.groovy
 */
import hudson.model.*
import es.eci.utils.SetMailParameters


// Variables --------
def build = Thread.currentThread().executable
def resolver = build.buildVariableResolver
def causa = build.getCause(Cause.UpstreamCause)
def action = resolver.resolve("action")
def managersMail = resolver.resolve("managersMail")
def userRTC = build.getEnvironment(null).get("userRTC")
def mailSubject = resolver.resolve("MAIL_SUBJECT")
def siempreEmail = resolver.resolve("siempreEmail")!="false"
def numeroLineas = resolver.resolve("numeroLineas")
// Destinatarios por defecto
def defaultManagersMail = build.getEnvironment(null).get("MANAGERS_MAIL")

if (numeroLineas==null) numeroLineas = 200

SetMailParameters mailParams = new SetMailParameters()
mailParams.initLogger({println it})
mailParams.setParameters(numeroLineas,causa,action,build,managersMail,userRTC,mailSubject, defaultManagersMail)