package es.eci.utils.npm

class NpmUtils {

	/** 
	 * Devuelve cierto si existe un gruntfile en el directorio
	 * @param directory Directorio de búsqueda
	 * @return Cierto si existe un fichero gruntfile.js (case unsensitive)
	 * en el directorio
	 */
	public static Boolean existsGruntfile(File directory) {
		Boolean ret = Boolean.FALSE;
		
		
		File[] files = directory.listFiles();
		for (File f: files) {
			if (f.getName().toLowerCase().equals("gruntfile.js")) {
				ret = true;
			}
		}
		
		
		return ret;
	}
}
