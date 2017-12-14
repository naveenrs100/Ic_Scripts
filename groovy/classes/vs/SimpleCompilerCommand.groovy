package vs

import es.eci.utils.StreamGobbler

/**
 * Esta clase encapsula la funcionalidad para llamar a un compilador groovy. 
 * Está configurada por defecto para Windows.
 */
class SimpleCompilerCommand {
	// Logging
	Closure logger = null	
	def initLogger( Closure _logger ) {
		logger = _logger
	}	
	// log de la clase haciendo uso de closure
	def log(def msg) {
	  if (logger != null) {
		  logger(msg)
	  }
	}
	
	/** Constructor por defecto. */
	public SimpleCompilerCommand() {
		
	}
	
	/**
	 * Lanza un compilador de microsoft con las opciones habituales
	 * @param compilador Nombre del compilador a utilizar
	 * @param fichero .VCPROJ, .MAK, .VCN, etc.
	 * @param target Objetivo de compilación.  P. ej. un valor podría ser "Debug|Pocket PC 2003 (ARMV4)", que
	 * supondría compilar en modo debug para la plataforma Pocket PC 2003 (ARMV4).  El fichero de compilación
	 * utilizado debe reconocer este target.
	 * @param directorio Directorio base de ejecución
	 * @param envp Variables de entorno que se inyectan a la ejecución
	 */
	public String[] compilar(String compilador, String fichero, String target, File directorio, List<String> envp = null) {
		def cadena = "$compilador $fichero $target"
		List<String> command = [ 'cmd.exe' , '/C' , cadena ] 
		def toArray = { List<String> lista ->
			String[] ret = null
			if (lista != null && lista.size() > 0) {
				ret = new String[lista.size()]
				int index = 0
				lista.each { obj ->
					ret[index++] = (String) obj
				}
			}
			return ret
		}
		
		Process p = Runtime.getRuntime().exec(toArray(command), toArray(envp), directorio);
		log cadena
		
		
		StreamGobbler cout = new StreamGobbler(p.getInputStream(), true)
		StreamGobbler cerr = new StreamGobbler(p.getErrorStream(), true)
		cout.start()
		cerr.start()
		p.waitFor()
		
		return [cout.getOut(), cerr.getOut()]
	}
}