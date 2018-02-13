package es.eci.utils.mail

import java.util.regex.Matcher

import es.eci.utils.JobRootFinder
import es.eci.utils.ParamsHelper
import es.eci.utils.StringUtil;
import es.eci.utils.base.Loggable
import groovy.json.JsonSlurper
import hudson.model.*

class SetMailParametersUrban extends Loggable {

	private Map params = [:]
	private Result fatherResult = null
	
	// Este método decide si debemos enviar el mail, en función del
	//	parámetro pasado desde el paso de Provider
	private boolean getSendMail(AbstractBuild build) {
		String param = ParamsHelper.getParam(build, "sendMail")
		boolean ret = false;
		if (!StringUtil.isNull(param)) {
			ret = Boolean.valueOf(param);
		}
		return ret;
	}
	
	/**
	 * Este método crea en el contexto de la acción las variables necesarias 
	 * para el envío de la notificación por correo: destinatarios, cuerpo del
	 * correo, etc. 
	 * 
	 * Así mismo, informa una variable buildType del build stepNotifierMail
	 * con el tipo de job del que se informa (group/component)
	 *  
	 * @param causa Causa de la llamada al build.
	 * @param action build/deploy/release/addFix/addHotfix/GenerateReleaseNotes
	 * @param build Instancia de la ejecución en jenkins
	 * @param managersMail Cadena con las direcciones de correo 
	 * 	de los destinatarios, separadas por comas
	 * @param mailSubject Asunto
	 */
	def setParameters (Cause causa, String action, AbstractBuild build, String managersMail,
		String mailSubject, String defaultManagersMail, String origen) {
		
		boolean sendMail = getSendMail(build);
		
		if (sendMail) {
	 		if (causa != null){
				// Ejecución -----------
				def nombrePadre = causa.getUpstreamProject()
				def numeroPadre = causa.getUpstreamBuild()
				def buildInvoker = Hudson.instance.getJob(nombrePadre).
					getBuildByNumber(Integer.valueOf(numeroPadre))
				log "--- INFO: Acciones del buildInvoker: $nombrePadre"
				
				log "--- INFO: ACTION: " + action
				// Añadir a la lista de destinatarios los indicados expresamente en el job
				List<String> receivers = MailAddressParser.parseReceivers(managersMail)
	
				if (causa != null){
					JobRootFinder finder = new JobRootFinder();
					finder.initLogger(this);
					AbstractBuild ancestor = finder.getRootBuild(buildInvoker);
					// Añadir a la lista de destinatarios la lista de destinatarios por defecto
					// indicados en la variable de entorno MANAGERS_MAIL
					MailUtils.addDefaultManagersMail(defaultManagersMail, receivers)
					
					MailWriter writer = new MailWriter();
					writer.initLogger(this)
					log "--- INFO: Lista de correos 2: $receivers"
					writeResult(buildInvoker, mailSubject, build, receivers, writer, origen)
				}
			}
			else{
				log "### ERROR: ESTE JOB NECESITA SER LLAMADO SIEMPRE DESDE OTRO!!"
				build.setResult(Result.FAILURE)
			}
		}
		else {
			log "### WARNING: Se cancela el envío de correo al no haber interacción con Urban Code"
			build.setResult(Result.NOT_BUILT)
		}
	}
	
	/**
	 * Construye el resumen HTML del resultado del despliegue
	 * @param buildInvoker Ejecución en jenkins que nos interesa resumir
	 * @param mailSubject Asunto del correo de notificación
	 * @param build Ejecución en jenkins del propio paso de envío de correo (a esta
	 * 	ejecución se añadirán parámetros con el resumen, etc.)
	 * @param receivers Lista de destinatarios del correo de notificación
	 * @param writer Implementación de escritor de líneas de correo
	 */ 
	private void writeResult ( AbstractBuild buildInvoker, String mailSubject, AbstractBuild build, 
		List receivers, MailWriter writer, String origen) {
		// Introduce el resultado de la ejecución para mostrarlo en el correo
		
		boolean truncatedLog = false
		String logBuild = getBuildLog(buildInvoker) 
		truncatedLog = scanLogBuild(logBuild)
		
		// Añade los parametros para ser pintados en el mail
		params["MAIL_LIST"] = receivers.join(',')
		log "MAIL_LIST: $receivers"
		log "truncatedLog: $truncatedLog"
		
		if (truncatedLog) {
			params["resumenHTML"] = "<h3>El proceso lanzado sobre " + origen + " no ha finalizado correctamente. Por lo tanto,<br />\
				no se transmite la ficha a UrbanCode, ni se efectúa despliegue en ningún entorno (en caso de que se haya solicitado)</h3>"
			params["buildResult"] = "NOT_EXECUTED"
			params["MAIL_SUBJECT"] = "Despliegue Urban [" + origen + "] - NO EJECUTADO"
		} else {
		
			String parsedLogBuild = parseBuildLog(logBuild)
			
			log "PARSED LOG: " + parsedLogBuild
		
			params["buildResult"] = getResult(parsedLogBuild)
			params["duration"] = getDuration(logBuild) + " msec."
			params["app"] = getApp(parsedLogBuild)
			params["despliegue"] = "<b>" + getDeploy(parsedLogBuild) + "</b>"
			params["resumenHTML"] = parsedLogBuild
			params["descriptorHTML"] = "<pre><code>" + parseJson(logBuild) + "</code></pre>"
			params["MAIL_SUBJECT"] = "Despliegue Urban [" + getApp(parsedLogBuild) + "] - " + getResult(parsedLogBuild)
		}

		ParamsHelper.addParams(build, params);
	}

	// Funciones
	/**
	 * Obtiene el log de ejecución en bruto
	 * @param build
	 * @return
	 */
	def getBuildLog (build){
		def res = ""
		if (build!=null)
			res = build.getLog()
		return res
	}
	
	/**
	 * Obtiene el tiempo total de ejecución a partir del log de despliegue de UrbanCode
	 * @param log
	 * @return Tiempo total de ejecución
	 */
	private String getDuration(String buildLog){
		
		if (buildLog.contains("Tiempo total ejecucion:")) {
			def pattern = /Tiempo total ejecucion: (\d+)/
		
			Matcher makeMatch = buildLog =~ pattern
			makeMatch.find();
		
			return makeMatch[0][1]
		} else
			return "Desconocido"

	}
	
	/**
	 * Consulta si está activado o no el despliegue en entorno a partir del log de despliegue de UrbanCode
	 * @param log
	 * @return ACTIVADO o DESACTIVADO
	 */
	private String getDeploy(String buildLog){
		
		if (buildLog.contains("No está activado")) {
			return "DESACTIVADO"
		} else
			return "ACTIVADO"		

	}
	
	/**
	 * Obtiene la aplicación UrbanCode parseando el descriptor json del log de despliegue
	 * @param log
	 * @return
	 */
	private String getApp(String buildLog) {
		
		JsonSlurper json = new JsonSlurper()
		def parsedJson = json.parseText(parseJson(buildLog))
		
		return parsedJson.application.toString()
		
	}
	
	/**
	 * Busca el descriptor de la ficha de despliegue dentro del log de despliegue de UrbanCode
	 * @param log
	 * @return Ficha de despliegue en formato prettyPrint
	 */
	private String parseJson(String buildLog) {
		def pattern = /--- INFO: Descriptor: \{(.+)\}/

		Matcher makeMatch = buildLog =~ pattern
		makeMatch.find();
		
		return groovy.json.JsonOutput.prettyPrint("{" + makeMatch[0][1] + "}")
	}
	
	/**
	 * Indica si el log esta truncado por un error y no se puede parsear
	 * @param log
	 * @return
	 */
	private boolean scanLogBuild(String buildLog) {
		
		if ( buildLog.contains("FATAL: ") || buildLog.contains("no hay acciones hacia Urbancode") ) {
			return true
		} else
			return false		
	}
	
	/**
	 * Da formato al log de despliegue de UrbaCode
	 * @param log
	 * @return
	 */
	private String parseBuildLog (String buildLog) {
		
		int inicio = buildLog.indexOf("--- INFO: Inicio de tratamiento con Urban")
		int fin = buildLog.indexOf("Tiempo total ejecucion: ", inicio)
			
		String resultado = buildLog.substring(inicio, fin)
			.replaceAll("--- ", "<br />--- ")
			.replaceAll("### ERROR:", "<br /><br /><font color='red'>### ERROR:</font>")
			.replaceAll("### ERROR URBANCODE:", "<br /><font color='red'>### ERROR URBANCODE:</font>")
			.replaceAll("--- INFO: Creando Snapshot", "<br />--- INFO: Creando Snapshot")
			.replaceAll("--- INFO: Se procede", "<br />--- INFO: Se procede")
			.replaceAll("--- INFO: Revisando", "<br />--- INFO: Revisando")
			.replaceAll("--- INFO: No está activado", "<br />--- INFO: No está activado")
			.replaceAll("--- INFO: Los siguientes", "<br />--- INFO: Los siguientes")
			.replaceAll("Tiempo ", "<br />Tiempo ")
			.replaceAll("!!! WARNING:", "<br /><br /><font color='orange'>!!! WARNING:</font>")
			.replaceAll("!!! WARNING URBANCODE:", "<br /><br /><font color='orange'>!!! WARNING URBANCODE:</font>")
			
		return resultado

	}
	
	/**
	 * Establece el resultado del correo de notificación a partir del log de despliegue de UrbanCode
	 * @param log
	 * @return
	 */
	private String getResult (String buildLog) {
		
		if (buildLog.contains("!!! WARNING")) {
			return "UNSTABLE"
		} else if (buildLog.contains("### ERROR")) {
			return "FAILURE"
		} else {
			return "SUCCESS"
		}

	}
	
}