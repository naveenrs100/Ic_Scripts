package es.eci.utils.commandline

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import es.eci.utils.Stopwatch
import es.eci.utils.StreamGobbler
import es.eci.utils.TmpDir
import es.eci.utils.base.Loggable

/**
 * Esta clase intenta solucionar la invocación de comandos indistintamente desde
 * windows y unix, así como incorporar la solución StreamGobbler para problemas
 * de tratamiento de salida de texto estándar y de error
 */
class CommandLineHelper extends Loggable {
	
	//---------------------------------------------------------------
	// Propiedades de la clase
	
	// Comando a ejecutar
	private String command;
	// Salida estándar
	private StreamGobbler std = null;
	// Salida de error
	private StreamGobbler err = null;
	// Cadena de ejecución
	private List<String> executionChain;
	// Tiempo de la última ejecución
	private Long millis = null;
	// Variables de entorno
	private Map<String, String> envVars;
		
	//---------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Tiempo de la última ejecución
	 * @return null si no se ha ejecutado todavía; tiempo de la última ejecución
	 * en otro caso
	 */
	public Long getLastExecutionTime() {
		return millis;
	}
	
	/**
	 * Salida estándar de la última ejecución
	 * @return null si no se ha ejecutado todavía; salida estándar de la última ejecución
	 * en otro caso
	 */
	public String getStandardOutput() {
		String ret = null;
		if (std != null) {
			ret = std.getOut(true);
		}
		return ret;
	}
	
	/**
	 * Salida de error de la última ejecución
	 * @return null si no se ha ejecutado todavía; salida de error de la última ejecución
	 * en otro caso
	 */
	public String getErrorOutput() {
		String ret = null;
		if (err != null) {
			ret = err.getOut(true);
		}
		return ret;
	}
	
	/**
	 * Construye un helper con la línea de comandos a ejecutar
	 * @param command Comando a ejecutar
	 */
	public CommandLineHelper(String command) {
		envVars = new HashMap<String, String>(System.getenv());
		Process p = null
		if (System.getProperty('os.name').toLowerCase().contains('windows')) {
			executionChain = ['cmd' , '/c' , command]
		}
		else {
			executionChain = ['sh' , '-c' , command]
		}
	}
	
	/**
	 * Ejecuta el comando sobre un directorio temporal, y lo destruye al final
	 * @return Resultado de la ejecución
	 */
	public Integer execute() {
		TmpDir.tmp { File dir ->
			return execute(dir)
		}
	}
	
	/**
	 * Devuelve una lista de variables de entorno de la forma clave=valor
	 * @return Null o bien una lista de variables de entorno expresadas como
	 * 		clave=valor
	 */
	private List getEnvVars() {
		List ret = null;
		if (envVars.keySet().size() > 0) {
			ret = [];			
			for (String variable: envVars.keySet()) {
				String value = envVars[variable];
				ret << "$variable=$value"
			}
		}
		//log "environment= " + ret.toString();
		return ret;
	}
	
	/**
	 * Asigna una variable de entorno a la ejecución.
	 * @param variable Variable de entorno.
	 * @param value Valor de la variable.
	 */
	public void setEnvVar(String variable, String value) {
		envVars[variable] = value;
	}
	
	/**
	 * Ejecuta el comando sobre el directorio indicado
	 * @param baseDir Directorio de ejecución (no puede ser nulo)
	 * @return Resultado de la ejecución
	 */
	public Integer execute(File baseDir) {
		if (baseDir == null) {
			throw new NullPointerException("El comando no puede ejecutarse sobre un directorio nulo")
		}
		int ret = -1;
		log "[${baseDir.canonicalPath}] ${executionChain[2]}"
		millis = Stopwatch.watch {
			Process p = executionChain.execute(getEnvVars(), baseDir)
			CountDownLatch latch = new CountDownLatch(2);
			std = new StreamGobbler(p.getInputStream(), true, latch)
			err = new StreamGobbler(p.getErrorStream(), true, latch)
			std.start()
			err.start()
			ret = p.waitFor()
			latch.await(450, TimeUnit.MILLISECONDS);
		}
		log std.getOut(true)
		log "Execution time (result: $ret) -> ${millis} mseg."
		log err.getOut(true)
		return ret;
	}
}