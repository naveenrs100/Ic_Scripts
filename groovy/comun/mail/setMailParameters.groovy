package mail

/**
 * Función que introduce los parametros necesarios para el envío de correos en cuelquier flujo.
 * $JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/mail/setMailParameters.groovy
 */
import es.eci.utils.mail.SetMailParameters
import hudson.model.*


// Variables --------
def action = build.buildVariableResolver.resolve("action")
def managersMail = build.buildVariableResolver.resolve("managersMail")
def userRTC = build.getEnvironment(null).get("userRTC")
def mailSubject = build.buildVariableResolver.resolve("MAIL_SUBJECT")
def siempreEmail = build.buildVariableResolver.resolve("siempreEmail")!="false"
def numeroLineas = build.buildVariableResolver.resolve("numeroLineas")
// Destinatarios por defecto
def defaultManagersMail = build.getEnvironment(null).get("MANAGERS_MAIL")

if (numeroLineas==null) numeroLineas = 200

SetMailParameters mailParams = new SetMailParameters()
mailParams.initLogger({println it})
mailParams.setParameters(numeroLineas,action,build,managersMail,userRTC,mailSubject, defaultManagersMail)