package es.eci.utils.base

import es.eci.utils.LogUtils

/**
 * Superclase para objetos groovy que definan y propaguen una determinada
 * closure para emitir logs 
 */
class Loggable {

	//-------------------------------------------------------
	// Propiedades de la clase
	
	// Logger (por defecto, println)
	protected LogUtils logger = null;
	
	//-------------------------------------------------------
	// Métodos de la clase		
	
	/**
	 * Define una closure que utilizar para emitir logs
	 * @param printer Closure para emitir logs
	 */
	public void initLogger(Closure printer) {
		logger = new LogUtils(printer);
	}
	
	/**
	 * Permite definir como closure para emitir logs la de otro
	 * componente Loggable, permitiendo así propagarla
	 * @param loggable Componente loggable cuya closure se nos propaga
	 */
	protected void initLogger(Loggable loggable) {
		if (loggable != null && loggable.logger != null) {
			initLogger(loggable.logger.logger)
		}
	}
	
	/**
	 * Si el log está configurado, lo utiliza para imprimir el mensaje.
	 * @param message Mensaje a imprimir
	 */
	protected void log(String message) {
		if (logger != null) {
			logger.log(message)
		}
	}
	
	/**
	 * Si el log está configurado, se utiliza para escribir la salida de una excepcion.
	 * @param message Mensaje a imprimir
	 */
	protected void logException(Throwable t) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos, true, "utf-8");
		t.printStackTrace(ps);
		println "Stack trace completa:"
		String stackTrace = new String(baos.toByteArray(), "utf-8")
		if (logger != null) {
			logger.log(stackTrace)
		}
	}
	
}