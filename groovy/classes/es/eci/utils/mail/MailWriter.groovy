package es.eci.utils.mail

import java.util.concurrent.TimeUnit

import buildtree.BuildBean
import es.eci.utils.ParamsHelper;
import es.eci.utils.StringUtil
import es.eci.utils.base.Loggable
import hudson.model.Result

/**
 * Superclase para las implementaciones de escritura de líneas
 * en el correo de notificación.
 */
abstract class MailWriter extends Loggable {

	/**
	 * Añade líneas al resumen HTML destinado al correo de notificación.
	 * @param resultsTable Tabla con la correspondencia de resultado a cadena
	 * @param html Resumen HTML del correo de notificación
	 * @param build Bean con la información del paso de ejecución
	 * @param numeroLineas Líneas de profundidad a mostrar si hay un error
	 * @param enviaSiempre 
	 * @param addLog 
	 * @return
	 */
	public abstract void addHTML (
			Map resultsTable,
			StringBuilder html, 
			BuildBean build, 
			int linesNumber, 
			boolean sendAlways, 
			boolean addLog);
		
	// Implementación real de la escritura de una línea en el correo
	protected void writeLine(Map resultsTable,
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
		int depth = build.getDepth() - 2;
		String indentation = "";
		if (depth > 0) {
			indentation = "style=\"text-indent:${2*depth}em\"";
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
		String ret = "";
		long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
		long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - 
		      TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
		if (minutes > 0) {
			ret = String.format("%d min %d seg", minutes, seconds);
		}
		else {
			ret = String.format("%d seg", seconds);
		}
		return ret;
	}	
	
	/**
	 * Devuelve la instancia correcta de escritor para cada tipo de
	 * job.
	 * @param buildType Tipo de job (group/component)
	 * @return Instancia de escritor necesaria
	 */
	public static MailWriter writer(String buildType) {
		MailWriter writer = null;
		if ("group".equals(buildType)) {
			writer = new GroupMailWriter();
		}
		else if ("component".equals(buildType)) {
			writer = new SingleMailWriter();
		}
		return writer;
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
