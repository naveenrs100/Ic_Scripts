package rtc
import java.util.regex.Matcher

import es.eci.utils.ScmCommand
import es.eci.utils.Stopwatch;


/**
 * Esta clase agrupa funciones de utilidad para acceso a RTC.  Se apoya en el
 * SimpleSCMCommand
 */
class RTCHelper {
	
	//--------------------------------------------------------------
	// Propiedades del helper
	
	// Credenciales
	private String user
	private String pwd
	private String urlRTC
	
	//--------------------------------------------------------------
	// Métodos del helper
	
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
	
	/** 
	 * Constructor con las credenciales
	 * @param user Usuario RTC
	 * @param user Password RTC
	 * @param user URL de repositorio RTC
	 */
	public RTCHelper(String user, String pwd, String urlRTC) {
		this.user = user
		this.pwd = pwd
		this.urlRTC = urlRTC
	}
	
	/**
	 * Descarga un componente completo a un directorio
	 * @param stream Corriente RTC
	 * @param component Nombre del componente
	 * @param dir Directorio sobre el que se descarga
	 * @param clean Si es cierto, limpia los directorios .metadata y .jazz5
	 */
	public void downloadComponent(String stream, String component, File dir, boolean clean=false) {
		long tiempo = Stopwatch.watch {
			ScmCommand scm = new ScmCommand(ScmCommand.Commands.LSCM)
			// Nos apoyamos en un ws temporal
			Date now = new Date()
			String timestamp = Long.toString(now.getTime())
			String ws = stream + timestamp
			scm.initLogger { this.logger }
			try {
				// Crear ws temporal
				scm.ejecutarComando("create workspace -e \"${ws}\"", user, pwd, urlRTC, dir)
				// Añadir el componente al ws temporal
				scm.ejecutarComando("workspace add-components \"${ws}\" \"${component}\" -s \"${stream}\"", user, pwd, urlRTC, dir)
				// Descargar el componente
				scm.ejecutarComando("load \"${ws}\" -d \"${dir.canonicalPath}\" \"${component}\" --force", user, pwd, urlRTC, dir)
			}
			finally {
				// Eliminar ws temporal
				scm.ejecutarComando("workspace delete \"${ws}\"", user, pwd, urlRTC, dir)
				log("Intentando detener el demonio sobre ${dir.canonicalPath} ...")
				scm.detenerDemonio(dir)
			}
			if (clean) {
				// Limpiar todos los ficheros que empiezan por .
				File[] ficheros = dir.listFiles()
				ficheros.each { fichero ->
					if (fichero.getName().startsWith(".")) {
						if (fichero.isDirectory()) {
							fichero.deleteDir()
						}
						else {
							fichero.delete()
						}
					}
				}
			}
		}
		log ("Tiempo para descargar el componente: $tiempo mseg.")
	}
	
	/**
	 * Descarga un fichero concreto a un directorio
	 * @param stream Corriente RTC
	 * @param component Nombre del componente
	 * @param filePath Ruta del fichero que se quiere descargar
	 * @param dir Directorio sobre el que se descarga
	 */
	public void downloadSingleFile(String stream, String component, String filePath, File dir) {
		long tiempo = Stopwatch.watch {
			ScmCommand scm = new ScmCommand(ScmCommand.Commands.LSCM)
			// Nos apoyamos en un ws temporal
			Date now = new Date()
			String timestamp = Long.toString(now.getTime())
			String ws = stream + timestamp
			scm.initLogger { this.logger }
			try {
				// Crear ws temporal
				scm.ejecutarComando("create workspace -e \"${ws}\"", user, pwd, urlRTC, dir)
				// Añadir el componente al ws temporal
				scm.ejecutarComando("workspace add-components \"${ws}\" \"${component}\" -s \"${stream}\"", user, pwd, urlRTC, dir)
				// Descargar el componente
				scm.ejecutarComando("load \"${ws}\" -d \"${dir.canonicalPath}\" \"${component}/${filePath}\" --force", user, pwd, urlRTC, dir)
			}
			finally {
				// Eliminar ws temporal
				scm.ejecutarComando("workspace delete \"${ws}\"", user, pwd, urlRTC, dir)
				log("Intentando detener el demonio sobre ${dir.canonicalPath} ...")
				scm.detenerDemonio(dir)
			}
		}
		log ("Tiempo para descargar el fichero: $tiempo mseg.")
	}
	
	/**
	 * Da el listado de componentes de una corriente
	 * @param stream Corriente RTC
	 * @param baseDir Directorio sobre el que se ejecuta el comando
	 */
	public List<String> listComponents(String stream, File baseDir) {
		List<String> ret = []
		long tiempo = Stopwatch.watch {
			ScmCommand scm = new ScmCommand(ScmCommand.Commands.LSCM)
			scm.initLogger(logger)
			// Crear ws temporal
			try {
				String salida = scm.ejecutarComando("list components \"${stream}\"", user, pwd, urlRTC, baseDir)
				def patron = /Component: [^"]*"([^"]*)"/
				salida.eachLine { line ->
					Matcher m = line.trim() =~ patron
					if (m.matches()) {
						ret << m[0][1]
					}
				}
			}
			finally {
				log("Intentando detener el demonio sobre ${baseDir.canonicalPath} ...")
				scm.detenerDemonio(baseDir)
			}
		}
		log ("Tiempo para listar los componentes: $tiempo mseg.")
		log ret
		return ret
	}
}