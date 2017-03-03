package vs;

class C_VS60_MSDEV_OutputParser implements OutputParser {
	
	def PATRON_RESULTADO = /([0-9]*) error\(s\), ([0-9]*) warning\(s\)/
	
	/**
	 * Realiza el parseo de las salidas del compilador y determina
	 * si se ha producido algún error de compilación a partir de las mismas
	 * @param stdout Salida estándar del comando
	 * @param stderr Salida de error del comando
	 */
	boolean validate(String stdout, String stderr) {
		boolean ret = true;
		// Buscamos el patrón del resultado en la stdout
		BufferedReader outReader = new BufferedReader(new StringReader(stdout))
		boolean continuar = true;
		while (continuar) {
			String line = outReader.readLine();
			if (line == null) {
				continuar = false;
			}
			else {
			def matcherResultado = (line =~ PATRON_RESULTADO)					
				for (int i = 0; i < matcherResultado.size(); i++) {
					String errores = matcherResultado[i][1];
					Integer intErrores = Integer.valueOf(errores);
					if (intErrores > 0) {
						ret = false;
						continuar = false;
					}
					// Hace caso omiso de las advertencias
				}
			}
		}
		return ret;
	}
}