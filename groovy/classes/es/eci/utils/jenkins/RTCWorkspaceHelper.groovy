package es.eci.utils.jenkins

/**
 * Esta clase da soporte a los nombres de workspace RTC, calculados según
 * el tipo de acción de job.
 */
class RTCWorkspaceHelper {

	//----------------------------------------------------------------
	// Constantes de la clase
	
	// Tabla de sustituciones
	private static final Map<String, String> ACTION_PATTERNS = [
			"build":'WSR - %s - BUILD - IC',
			"deploy":'WSR - %s - DEPLOY - IC',
			"release":'WSR - %s - RELEASE - IC',
			"addFix":'WSR - %s - ADDFIX - IC',
			"addHotfix":'WSR - %s - ADDHOTFIX - IC'
		]
	
	//----------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Devuelve el nombre apropiado del workspace de RTC para la corriente
	 * indicada.
	 * @param stream Nombre de la corriente.
	 * @return Nombre del workspace de RTC adecuado
	 */
	public static String getWorkspaceRTC(String action, String stream) {
		String ret = "";
		String pattern = ACTION_PATTERNS[action]
		if (pattern != null) {
			ret = String.format(pattern, stream)
		}
		return ret;
	}
}
