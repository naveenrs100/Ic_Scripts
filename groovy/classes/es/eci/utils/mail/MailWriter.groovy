package es.eci.utils.mail

import java.util.Map
import java.util.concurrent.TimeUnit

import buildtree.BuildBean
import es.eci.utils.ParamsHelper;
import es.eci.utils.StringUtil
import es.eci.utils.base.Loggable
import hudson.model.AbstractBuild
import hudson.model.Result

/**
 * Clase para la escritura de líneas
 * en el correo de notificación.
 */
class MailWriter extends Loggable {
	
	//---------------------------------------------------------------------------
	// Constantes de la clase
	
	private final static List JOB_EXCLUSIONS = [ "controller", "workflow" ] 
	
	//---------------------------------------------------------------------------
	// Métodos de la clase	

	/**
	 * Añade líneas al resumen HTML destinado al correo de notificación.
	 * @param resultsTable Tabla con la correspondencia de resultado a cadena
	 * @param isHeader Cierto si la línea es una cabecera con el nombre de 
	 * 	componente.
	 * @param html Resumen HTML del correo de notificación
	 * @param build Bean con la información del paso de ejecución
	 * @param numeroLineas Líneas de profundidad a mostrar si hay un error
	 * @param enviaSiempre 
	 * @param addLog 
	 * @return
	 */
	public void addHTML(
			Map resultsTable,
			boolean isHeader,
			StringBuilder html, 
			BuildBean build, 
			int linesNumber, 
			boolean sendAlways, 
			boolean addLog) {
		// Aplicar la lista de exclusiones
		boolean exclude = false;
		
		JOB_EXCLUSIONS.each { String exclusion ->
			if (build.getName().toLowerCase().startsWith(exclusion)) {
				exclude = true;
			}
		}
		
		if (!exclude) {
			writeLine(resultsTable, isHeader, html, build, linesNumber, sendAlways, addLog);
		}
	}	
		
	// Implementación real de la escritura de una línea en el correo
	protected void writeLine(
			Map resultsTable,
			boolean isHeader,
			StringBuilder html, 
			BuildBean build, 
			int linesNumber, 
			boolean sendAlways, 
			boolean addLog) {
		def noExecTxt = "NOT_EXECUTED"
			
		def resultTmp = build!=null?build.getResult():noExecTxt
		def isError = resultTmp!=Result.SUCCESS.toString() && resultTmp!=noExecTxt
		def result = resultTmp
		if (resultsTable[resultTmp.toString()] != null) {
			result = resultsTable[resultTmp.toString()]
		}
		String duration = getDuration(build.duration);
		def shouldWrite = sendAlways
		log "addHTML-> ${build.name} - ${build.description}: ${result} -> profundidad ${build.depth}"
		int depth = build.getDepth();
		String indentation = "";
		if (!isHeader &&  depth > 0) {
			indentation = "style=\"text-indent:2em\"";
			log "Aplicando sangría: $indentation"
		}
		String htmlTmp = "<p ${indentation} class='${resultTmp}'><span>${componentResolution(build)}:</span> ${result} ${duration}</p>"
		if (isError && addLog){
			htmlTmp += "<br/>Last ${linesNumber} lines:<br/><br/>"
			build.getLogTail().each(){ htmlTmp += "${it}<br/>" }
			htmlTmp += "<br/>"
			shouldWrite = true;
		}
		if (shouldWrite) html.append htmlTmp
	}
		
	// Construye una descripción de la duración legible
	protected String getDuration(Long duration) {
		String ret = "0 seg";
		if (duration != null) {
			long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
			long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - 
			      TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
			if (minutes > 0) {
				ret = String.format("%d min %d seg", minutes, seconds);
			}
			else {
				ret = String.format("%d seg", seconds);
			}
		}
		return ret;
	}	
	
	/**
	 * Este método añade al estado de construcción el log
	 * del build principal. Se usa cuando se muestra un error ocurrido
	 * durante el job de corriente, mientras no se han ejecutado todavía pasos.
	 * @param status StringBuilder donde se acumula el mensaje
	 * @param linesNumber Número de líneas a las que limitar el log
	 * @param build Construcción en jenkins
	 */
	public void appendMainBuildLog(
			StringBuilder status, 
			int linesNumber, 
			AbstractBuild build) {
		def noExecTxt = "NOT_EXECUTED"
		def resultTmp = build!=null?build.getResult():noExecTxt
		def isError = resultTmp!=Result.SUCCESS.toString() && resultTmp!=noExecTxt
		def result = resultTmp
		String duration = getDuration(build.duration);
		String description = build.getProject().getName()
		if (!StringUtil.isNull(build.description)) {
			description = build.description
		}
		String htmlTmp = "<p class='FAILURE'><span>${description}:</span> ${result} ${duration}</p>"
		htmlTmp += "<br/>Last ${linesNumber} lines:<br/><br/>"
		build.getLog(linesNumber).each(){ htmlTmp += "${it}<br/>" }
		htmlTmp += "<br/>"
		status.append(htmlTmp)
	}
	
	//Devuelve el nombre del componente o la descripción de la corriente
	private String componentResolution(BuildBean build) {
		
		if (!build.getName().contains('-COMP-') && !build.getName().contains('COMPNew')) {
			return "${build.description}"
		} else {
			StringBuilder stb = new StringBuilder()
			
			if (!StringUtil.isNull(build.getComponent())) {
				stb.append(build.getComponent()).append(' ')
			} else if (build.getName().contains('-COMP-')) {
				String[] parts = build.getName().split('-COMP-')
				
				if (parts.length == 2) {
					stb.append(parts[1]).append(' ')
				}
			}
			
			if (!StringUtil.isNull(build.getBuiltVersion())) {
				stb.append('[')
				stb.append(build.builtVersion)
				stb.append(']')
			}
			
			return stb.toString()
		}
	}
}
