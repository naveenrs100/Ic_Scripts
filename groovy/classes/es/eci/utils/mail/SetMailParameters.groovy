package es.eci.utils.mail

import org.jenkinsci.plugins.commons.tests.CoverageInfoBean
import org.jenkinsci.plugins.commons.tests.TestResultBean
import org.jenkinsci.plugins.jobexecutors.toplevelitems.ComponentChangesBean
import org.jenkinsci.plugins.jobexecutors.toplevelitems.ComponentsChangesAction
import org.jenkinsci.plugins.testsreportaggregator.beans.CoverageTestsInfoAction;

import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeLogReader

import buildtree.BuildBean;
import buildtree.BuildTreeHelper
import es.eci.utils.JobRootFinder
import es.eci.utils.ParamsHelper
import es.eci.utils.Stopwatch
import es.eci.utils.base.Loggable
import hudson.model.*

class SetMailParameters extends Loggable {

	private Map params = [:]
	File changeLogFile = null
	File releaseNotesFile = null
	
	def resultsTable = new HashMap()
	
	public SetMailParameters() {	
		resultsTable.put(Result.SUCCESS.toString(),"&Eacute;XITO")
		resultsTable.put(Result.FAILURE.toString(),"FALLO")
		resultsTable.put(Result.UNSTABLE.toString(),"INESTABLE")
		resultsTable.put(Result.ABORTED.toString(),"ABORTADO")
		resultsTable.put(Result.NOT_BUILT.toString(),"OMITIDO")
	}

	// Descompone una lista de destinatarios separada por comas en una lista de
	//	cadenas de caracteres
	private List<String> parseReceivers(String managersMail) {
		List<String> ret = new LinkedList<String>()
		def managers = managersMail.split(',')
		managers.each { if (it != null && it.trim().size() > 0) { ret << it } }
		return ret;
	}
	
	/**
	 * Este método crea en el contexto de la acción las variables necesarias 
	 * para el envío de la notificación por correo: destinatarios, cuerpo del
	 * correo, etc. 
	 * 
	 * Si el método determinara que no es necesario enviar el mail, asignará
	 * un valor falso a la variable sendMail del build stepNotifierMail
	 * 
	 * Así mismo, informa una variable buildType del build stepNotifierMail
	 * con el tipo de job del que se informa (group/component)
	 * 
	 * @param numeroLineas Número de líneas del log a 
	 * 	incluir en el correo 
	 * @param causa Causa de la llamada al build.
	 * @param action build/deploy/release/addFix/addHotfix/GenerateReleaseNotes
	 * @param build Instancia de la ejecución en jenkins
	 * @param managersMail Cadena con las direcciones de correo 
	 * 	de los destinatarios, separadas por comas
	 * @param userRTC Nombre de usuario funcional de RTC
	 * @param mailSubject Tema del correo
	 * @param defaultManagersMail Cadena con las direcciones de 
	 * 	correo obligatorias de los destintarios del equipo de IC,
	 * 	separadas por comas
	 */
	def setParameters (
			int numeroLineas,
			String action,
			AbstractBuild build,
			String managersMail,
			String userRTC,
			String mailSubject,
			String defaultManagersMail = null) {
		boolean sendMail = true;
		AbstractBuild buildInvoker = new JobRootFinder().getParentBuild(build);
		if (buildInvoker != null || action == "GenerateReleaseNotes"){
			// Ejecución -----------			
			log "**** ACTION: " + action
			// Añadir a la lista de destinatarios los indicados expresamente en el job
			List<String> receivers = parseReceivers(managersMail)
			String buildType = null;
			if (buildInvoker != null){
				JobRootFinder finder = new JobRootFinder();
				finder.initLogger(this);
				AbstractBuild ancestor = finder.getRootBuild(buildInvoker);
				// Si el ancestro está en NOT_BUILT, sendMail <- false
				if (ancestor.getResult().equals(Result.NOT_BUILT)
					|| ancestor.getResult().equals(Result.ABORTED)) {
					sendMail = false;
				}
				else {
					// Normalmente -COMP-
					// Algunos legacy tenían -COMPNew
					if (ancestor.getProject().getName().contains("-COMP-")
							|| ancestor.getProject().getName().contains("COMPNew")) {
						buildType = 'component';
						log "Ancestro: job de componente"
						// Componer el correo de componente
						prepareData(buildInvoker,build,userRTC, receivers)
						prepareDataReleaseNotes(buildInvoker, build, userRTC)	
					}
					else {
						log "Ancestro: job de grupo"
						buildType = 'group';
						// Si el ancestro es de grupo/corriente, y estamos en el componente, no
						//	hacemos nada
						if (!buildInvoker.getProject().getName().contains("-COMP-")
							&& !buildInvoker.getProject().getName().contains("COMPNew")) {
							log "Actual: job de grupo"
							sendMail = true;
							// Recuperación de informe de cobertura
						}
						else {						
							log "Actual: job de componente"
							sendMail = false;
						}				
						// Información de cambios
						//   --> Presente en el propio build
						// Información de pruebas y cobertura
						//	 --> Presente en el propio build	
					}
					ParamsHelper.addParams(build, ['buildType': buildType])
				}
				
				ParamsHelper.deleteParams(build, 'sendMail')
				ParamsHelper.addParams(build, ['sendMail':Boolean.toString(sendMail)])
				// Añadir a la lista de destinatarios la lista de destinatarios por defecto
				//	indicados en la variable de entorno MANAGERS_MAIL
				MailUtils.addDefaultManagersMail(defaultManagersMail, receivers)			
			}

			if (action == "GenerateReleaseNotes"){
				prepareDataReleaseNotes(buildInvoker,build,userRTC)
			}
			if (sendMail) {
				MailWriter writer = MailWriter.writer(buildType);
				writer.initLogger(this)
				log "Lista de correos 2: $receivers"
				writeResult(buildInvoker,numeroLineas,mailSubject,build, receivers, writer)
			}
			else {
				// Previene 
				ParamsHelper.addParams(build, ["MAIL_BUILD_STATUS":"CANCELADO"])
			}
		}
		else{
			log "ESTE JOB NECESITA SER LLAMADO SIEMPRE DESDE OTRO!!"
			build.setResult(Result.FAILURE)
		}

	}
	
	// Recoge los datos de cambios en RTC para componer el correo de componente
	def private prepareData(buildInvoker,build,userRTC, List destinatarios){

		for (Action a : buildInvoker.getActions()) {
			log a.getClass().getName()
		}
		changeLogFile = new File("${buildInvoker.getRootDir()}/changelog.xml")
		def changeSet = getChangeSet(buildInvoker,changeLogFile)
		log "Conjunto de cambios en ${buildInvoker.getRootDir()}/changelog.xml: $changeSet"
		
		if (changeSet != null && changeSet.isEmptySet()) {
			log "El conjunto de cambios existe pero está vacío"
		}
		if (changeSet!=null){
			log "buildInvoker.getRootDir(): ${buildInvoker.getRootDir()}"
			log "buildInvoker.getChangeSet: ${changeSet}"

			log "<<< LOG >>> [build : Class] ["+build+" : "+build.getClass()+"]"

			build.setResult(buildInvoker.getResult())
			log "build.getChangeSet: ${build.getChangeSet()}"

			// Añade los correos de los autores de los cambios
			def unknownMails = ""
			def autores = []
			// Comprueba que los correos no hay ningún unknow
			changeSet.each() { change ->
				def mail = change.getEmail()
				def author = change.getUser()
				if (mail!=null && mail.indexOf("@")==-1 && autores.find{a->a==author}==null){
					if ("${author}"!="${userRTC}"){
						autores.add(author)
						unknownMails += "<li>${author}: ${mail}</li>"
					}
				}
				else {
					if (mail!=null && mail.indexOf("@") !=-1 && !destinatarios.contains(mail)) {
						destinatarios.add(mail)
					}
				}
				log "${author}: ${mail}"
			}

			if (unknownMails.length()>0){
				params["unknownMails"] = unknownMails;
			}
			
			log "Destinatarios -->"
			destinatarios.each { log it }
			log "Lista de correos 1: $destinatarios"
		}
	}

	// Recoge el resultado de comparar las instantáneas/líneas base solicitadas
	//	en las release notes
	def private prepareDataReleaseNotes(buildInvoker,build,userRTC){
		// Generate release notes
		releaseNotesFile = new File("${buildInvoker.getRootDir()}/releaseNotesLog.xml")
		def releaseNotes = getChangeSet(buildInvoker,releaseNotesFile)
		log "Release notes en ${buildInvoker.getRootDir()}/releaseNotesLog.xml: $releaseNotes"
		if (releaseNotes != null && releaseNotes.isEmptySet()) {
			log "El conjunto de cambios existe pero está vacío"
		}
		if (releaseNotes!=null){
			log "buildInvoker.getRootDir(): ${buildInvoker.getRootDir()}"
			log "buildInvoker.getChangeSet: ${releaseNotes}"

			log "<<< LOG >>> [build : Class] ["+build+" : "+build.getClass()+"]"

			build.setResult(buildInvoker.getResult())
			log "build.getChangeSet: ${build.getChangeSet()}"
		}

	}

	/**
	 * Este método convierte la lista de pasos de construcción, que es el recorrido en 
	 * profundidad del árbol, en un mapa indexado por componente, que puede recorrerse
	 * en orden y cada componente indexa la lista de pasos hijos.
	 * @param beans Recorrido en profundidad del árbol de construcción
	 * @return Lista de componentes que intervienen en la construcción.  Cada uno tiene
	 * su propia lista de hijos
	 */
	private List<BuildBean> processExecutionTree(List<BuildBean> beans) {
		log "Proceso del árbol de ejecución para obtener los pasos por componente"
		log "Lista de beans a la entrada:"
		beans.each { bean -> log bean.toString() }
		List<BuildBean> ret = new LinkedList<BuildBean>();
		BuildBean currentComponent = null;
		beans.each { BuildBean currentBean ->
			if (currentBean.getName().contains('-COMP-') 
				|| currentBean.getName().contains('COMPNew')) {
				currentComponent = currentBean;
				ret << currentComponent;
			}
			else if (currentComponent != null) {
				List<BuildBean> children = currentComponent.getChildren();
				children << currentBean;
			}
		}
		log "Mapa de beans a la salida:"
		ret.each { component ->
			log "Componente: $component"
			component.getChildren().each { step ->
				log "\t" + step.toString()
			}
		}
		return ret;
	}
	
	/**
	 * Construye el resumen HTML de la ejecución de los pasos de una construcción.
	 * @param buildInvoker Ejecución en jenkins que nos interesa resumir (o bien es
	 * 	un lanzamiento de componente o de grupo)
	 * @param numeroLineas Líneas del log (desde la última) que se mostrarán en caso de error
	 * @param mailSubject Asunto del correo de notificación
	 * @param build Ejecución en jenkins del propio paso de envío de correo (a esta
	 * 	ejecución se añadirán parámetros con el resumen, etc.)
	 * @param destinatarios Lista de destinatarios del correo de notificación
	 * @param writer Implementación de escritor de líneas de correo
	 */ 
	private void writeResult (
			AbstractBuild buildInvoker,
			int linesNumber,
			String mailSubject, 
			AbstractBuild build, 
			List receivers,
			MailWriter writer){
		//----------
		// Introduce el resultado de la ejecución para mostrarlo en el correo

		def logBuild = getBuildLog(buildInvoker)
		def nombreHijo = buildInvoker.getEnvironment(null).get("LAST_TRIGGERED_JOB_NAME")
		def buildWorkFlow = getBuild(nombreHijo,logBuild)
			
		BuildTreeHelper helper = new BuildTreeHelper(linesNumber);
		helper.initLogger(this);
		List<BuildBean> beans = null;
		long timeExecutionTree = Stopwatch.watch {
			beans = helper.executionTree(buildInvoker);
		}
		log "Construcción del árbol: ${timeExecutionTree} mseg."

		StringBuilder resumenHTML = new StringBuilder()
		StringBuilder statusHTML = new StringBuilder()
		
		// Elaborar una estructura de datos que permita recorrer por componente
		List<BuildBean> executionByComponents = processExecutionTree(beans);
		
		executionByComponents.each { BuildBean componentBean ->
			// Cabecera del componente
			resumenHTML.append("<h3>")
			writer.addHTML(resultsTable, resumenHTML, componentBean, linesNumber, true, false)
			writer.addHTML(resultsTable, statusHTML, componentBean, linesNumber, false, true)
			resumenHTML.append("</h3>")
			List<BuildBean> children = componentBean.getChildren()
			children.each { BuildBean child ->
				// Líneas para cada paso de construcción
				writer.addHTML(resultsTable, resumenHTML, child, linesNumber, true, false)
				writer.addHTML(resultsTable, statusHTML, child, linesNumber, false, true)
			}
			
			//log "Compo: " + buildPaso.getComponent()
			CoverageTestsInfoAction coverageTestInfoAction = buildInvoker.getAction(
				org.jenkinsci.plugins.testsreportaggregator.beans.CoverageTestsInfoAction.class)
			if (coverageTestInfoAction != null) {
				Map<String, TestResultBean> mapaTestResults = coverageTestInfoAction.getResults()
	
				// Resultados Test Junit
				TestResultBean resultadosTest = mapaTestResults.get(componentBean.getComponent())
				if (resultadosTest != null) {
					resumenHTML.append('<label for="' + componentBean.getComponent() + '_junit">' + 
						'<p style="text-indent:5em"><span>Resumen de pruebas unitarias (expandir)</span></p></label><input type="checkbox" id="' + 
						componentBean.getComponent() + '_junit" checked><div class="hide"><table class="datos"><thead>' + 
						'<tr><td>Fallados</td><td>Correctos</td><td>Omitidos</td><td>Total</td></tr></thead><tbody><tr><td>' + 
						resultadosTest.getFailCount() + '</td><td>' + 
						(resultadosTest.getTotalCount() - (resultadosTest.getFailCount()+resultadosTest.getSkipCount())) +
						'</td><td>' + resultadosTest.getSkipCount() + '</td><td>' + resultadosTest.getTotalCount() +
						'</td></tr></tbody></table></div>')
				}
				
				// Resultados Cobertura
				Map<String, CoverageInfoBean> mapaCoverageInfo = coverageTestInfoAction.getCoverage()
				
				CoverageInfoBean resultadosCoverage = mapaCoverageInfo.get(componentBean.getComponent())
				
				if (resultadosCoverage != null) {
					resumenHTML.append('<label for="' + componentBean.getComponent() + '_cover">' + 
						'<p style="text-indent:5em"><span>Resumen de Cobertura de Proyecto (expandir)</span></p></label><input type="checkbox" id="' + componentBean.getComponent() + 
						'_cover" checked><div class="hide"><table class="datos"><thead><tr><td>Package</td><td>Branches</td>' +
						'<td>Complexity</td><td>Instructions</td><td>Methods</td><td>Lines</td><td>Classes</td></tr></thead><tbody>')
					resultadosCoverage.getMetricsByPackage().each { paquete, metricasMap ->
						resumenHTML.append('<tr><td>' + paquete + '</td>')
						resumenHTML.append('<td>' + metricasMap.branches.round(2) + '%</td>')
						resumenHTML.append('<td>' + metricasMap.complexity.round(2) + '%</td>')
						resumenHTML.append('<td>' + metricasMap.instructions.round(2) + '%</td>')
						resumenHTML.append('<td>' + metricasMap.methods.round(2) + '%</td>')
						resumenHTML.append('<td>' + metricasMap.lines.round(2) + '%</td>')
						resumenHTML.append('<td>' + metricasMap.classes.round(2) + '%</td></tr>')
					}
					resumenHTML.append('<tr><td><b>Overall</b></td>')
					resumenHTML.append('<td>' + resultadosCoverage.getTotals().branches.round(2) + '%</td>')
					resumenHTML.append('<td>' + resultadosCoverage.getTotals().complexity.round(2) + '%</td>')
					resumenHTML.append('<td>' + resultadosCoverage.getTotals().instructions.round(2) + '%</td>')
					resumenHTML.append('<td>' + resultadosCoverage.getTotals().methods.round(2) + '%</td>')
					resumenHTML.append('<td>' + resultadosCoverage.getTotals().lines.round(2) + '%</td>')
					resumenHTML.append('<td>' + resultadosCoverage.getTotals().classes.round(2) + '%</td></tr>')
					resumenHTML.append('</tbody></table></div>')
				}
			}
			
			// Resultados ChangeSet
			ComponentsChangesAction componentsChangesAction = buildInvoker.getAction(
				org.jenkinsci.plugins.jobexecutors.toplevelitems.ComponentsChangesAction.class)
			if (componentsChangesAction != null) {
				List<ComponentChangesBean> listaChanges = componentsChangesAction.getCompoChangesBean();
				listaChanges.each { elemento ->
					// Tenemos que recorrer la lista en cada iteración
					if (elemento.componentName == componentBean.getComponent()) {
						resumenHTML.append('<label for="' + componentBean.getComponent() + '_chang">' +
							'<p style="text-indent:5em"><span>Resumen de Cambios (expandir)</span></p></label>' +
							'<input type="checkbox" id="' + componentBean.getComponent() + '_chang" checked><div class="hide">')
						elemento.changeSetBean.each {
								resumenHTML.append('<p class="normal">Workitem: <b>' + it.WorkItem + '</b> por <b>' + it.Autor + '</b> - ' + it.Fecha +
								' (' + it.Comentario + ')</p>')
						}
						resumenHTML.append('</div>')
					}
				}
			}

		}
		
		// Añade los parametros para ser pintados en el mail
		def status = resultsTable[buildInvoker.getResult().toString()]
		status = status.replace('&Eacute;', 'E')
		params["buildResult"] = buildInvoker.getResult().toString()
		params["MAIL_BUILD_STATUS"] = status
		params["MAIL_LIST"] = receivers.join(',')
		log "MAIL_LIST: $receivers"
		params["duration"] = buildInvoker.getDurationString()
		params["urlCheckin"] = getUrlCheckin(buildInvoker)
		params["version"] = getVersion(buildInvoker)
		params["statusHTML"] = statusHTML.toString()
		params["resumenHTML"] = resumenHTML.toString()
		params["resultadoTraducido"] = resultsTable[buildInvoker.getResult().toString()]
		if (changeLogFile != null) {
			params["rutaFicheroChangeLog"] = changeLogFile.getCanonicalPath()
		}
		if (releaseNotesFile != null) {
			log("Preparando la ruta del fichero de release notes: " + releaseNotesFile.getCanonicalPath());
			params["rutaFicheroReleaseNotesLog"] = releaseNotesFile.getCanonicalPath()
		}
		else {
			log "No hay fichero de release notes"
		}
		if (mailSubject==null || mailSubject.length()==0)
			params["MAIL_SUBJECT"] = buildInvoker.getProject().getName()

		ParamsHelper.addParams(build, params);
	}

	// Funciones --------
	// Closure que obtiene el job
	def getBuild = { nombreJob, texto ->
		print "${nombreJob} "
		def ret = null
		try {
			def matcher =  texto =~ /(?s).*${nombreJob}[^#]*#([0-9]+) completed.*/
			if (matcher.matches()){
				def buildNumber = matcher [0] [1]
				log ": ${buildNumber}"
				ret = Hudson.instance.getJob(nombreJob).getBuildByNumber(Integer.valueOf(buildNumber))
			}
		}
		catch(Exception e) {
			log(e.getMessage());
			ret = null;
		}
		return ret
	}

	// Devuelve la URL de las pizarras sobre el cuadro de mando de Checking
	//	definida en las propiedades  
	def getUrlCheckin(build){
		def pizarras = build.getEnvironment(null).get("CHECKING_PIZARRAS")
		log "Análisis estático: " + pizarras
		return pizarras
	}

	def getVersion(build){
		def version = new File("${build.workspace}/version.txt")
		log "Recuperando la versión de ${version.canonicalPath} ..."
		try {
			if (version.exists()) {
				def config = new ConfigSlurper().parse(version.toURL())
				def v = config.getProperty("version")
				return (v instanceof String?v:"")
			}
		}
		catch (Exception e) {
			log e.getMessage()
		}
		return ""
	}

	def getBuildLog (build){
		def res = ""
		if (build!=null)
			res = build.getLog()
		return res
	}

	def getChangeSet(build,changeLogFile){
		def ret = null
		try {
			if (changeLogFile!=null && changeLogFile.exists()){
				def jazzChangeLogReader = new JazzChangeLogReader()
				ret = jazzChangeLogReader.parse(build,changeLogFile)
			}
		}
		catch(Exception e) {
			// Error de parseo: ¿puede ser que el fichero no exista?
			// Nos lo tomamos como un warning y avanzamos
			log e.getMessage()
		}
		return ret;
	}


}