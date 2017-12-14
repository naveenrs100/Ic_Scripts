package rtc

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.eci.utils.LogUtils;
import es.eci.utils.ScmCommand;
import groovy.lang.Closure;
import java.text.Normalizer;

/**
 * Esta clase construye y serializa un Map indexado por área de proyecto, con las
 * corrientes de RTC accesibles al usuario pasado como parámetro
 */
class ProjectAreasMap {
	
	//-----------------------------------------------------
	// Propiedades de la clase
	
	private LogUtils log = new LogUtils(null);
	private ScmCommand command;
	
	//-----------------------------------------------------
	// Métodos de la clase
	
	// Logs de la clase
	public initLogger(Closure closure) {
		log = new LogUtils(closure);
		command.initLogger(closure);
	}
	
	/**
	 * @param toolsHome Ruta de las herramientas SCM en local
	 */  	
	public ProjectAreasMap(String toolsHome) {
		command = new ScmCommand(ScmCommand.Commands.LSCM, toolsHome, null);		
	}
	
	// Parsea la lista de áreas a partir de la salida del comando
	private List<String> parseStrings(String output) {
		List<String> ret = new LinkedList<String>();
		
		def p = /[^\"]*\"([^\"]+)\".*/

		output.eachLine { line ->
			Matcher m = (line =~ p);
			ret.add(m[0][1]);
		}
		
		return ret;
	}
	
	/**
	 * Elimina el (RTC) y recorta espacios del nombre del área
	 * @param projectAreaName Nombre de área de proyecto.
	 * @return Nombre de área de proyecto embellecido.
	 */
	public static String beautify(String projectAreaName){
		if (projectAreaName.endsWith("(RTC)")) {
			projectAreaName = projectAreaName.replace("(RTC)","");
		}
		if (projectAreaName.endsWith(" - RTC")) {
			projectAreaName = projectAreaName.replace(" - RTC","");
		}
		projectAreaName = Normalizer.normalize(projectAreaName, Normalizer.Form.NFD).
			replaceAll("[^\\p{ASCII}]", "");
		// Eliminar el identificador de área
		// Si queda algún guion, eliminar hasta el guion
		int index = projectAreaName.indexOf("-");
		if (index != -1) {
			projectAreaName = projectAreaName.substring(index + 1);
		}
		
		return projectAreaName.trim();

	}
	
	/**
	 * Parsea las corrientes de cada área de proyecto visible al 
	 * usuario en el repositorio indicado
	 * @param user Nombre de usuario RTC
	 * @param password Password del usuario RTC
	 * @param urlRTC Repositorio RTC
	 * @param baseDir Directorio donde se ejecuta el comando (lo 
	 * 	necesita para construir el .metadata, etc.)
	 */
	public Map<String, List<String>> map(String user, String password, String urlRTC, File baseDir) {
		Map<String, List<String>> ret = new HashMap<String, List<String>>();
		try {
			log.log "Listando áreas de proyecto de ${user} en ${urlRTC} ..."
			// Obtener las áreas de proyecto
			String salidaAreas = command.ejecutarComando("list projectareas", user, password, urlRTC, baseDir);
			// Parsear áreas de proyecto
			List<String> areas = parseStrings(salidaAreas);
			// Obtener las corrientes
			log.log "Listando las corrientes correspondientes a cada área de proyecto..."
			areas.each { String area ->
				String salidaCorrientes = command.ejecutarComando(
					"list streams --projectarea \"${area}\" --maximum 200", user, password, urlRTC, baseDir);
				List<String> streams = parseStrings(salidaCorrientes);
				ret.put(area, streams)
			}
		}
		finally {
			command.detenerDemonio(baseDir);
		}
		return ret;
	}
	
}