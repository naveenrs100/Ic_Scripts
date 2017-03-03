package vs;

class C_VS_OutputParser implements OutputParser {
	
	/**
	 * Realiza el parseo de las salidas del compilador y determina
	 * si se ha producido algún error de compilación a partir de las mismas
	 * @param stdout Salida estándar del comando
	 * @param stderr Salida de error del comando
	 */
	boolean validate(String stdout, String stderr) {
		// Busca los mensajes de error del compilador en la stderr
		// NMAKE : fatal error
		boolean ret = true;
		if (stderr.contains("NMAKE : fatal error")) {
			ret = false;
		}
		return ret;
	}
}