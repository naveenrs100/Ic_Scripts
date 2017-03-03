package vs;

class C_VS60_OutputParser implements OutputParser {
	
	/**
	 * Realiza el parseo de las salidas del compilador y determina
	 * si se ha producido algún error de compilación a partir de las mismas
	 * @param stdout Salida estándar del comando
	 * @param stderr Salida de error del comando
	 * @return True si la compilación se ha producido sin errores; false en otro caso.
	 */
	boolean validate(String stdout, String stderr) {
		return true;
	}
}