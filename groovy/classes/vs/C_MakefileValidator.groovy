package vs;

import java.io.File;


/**
 * Esta clase implementa un validador de makefiles.  Busca un makefile (.vcn) en 
 * el directorio y su fichero de proyecto equivalente (.vcp).  Si hay más de un .vcn,
 * o bien hay un .vcp y tiene fecha posterior a la del .vcn, da un error  
 */
class C_MakefileValidator implements C_Validator {
	/**
	 * Aplica validaciones a un directorio con fuentes.
	 */
	void validate(File workspace) {
		// Buscar candidatos a .vcn
		File makefile = null;
		List<File> candidatos = new LinkedList<File>()
		File[] ficheros = workspace.listFiles()
		for (File fich: ficheros) {
			if (fich.isFile() && fich.getName().toLowerCase().endsWith(".vcn")) {
				candidatos << fich
			}
		}
		// Si hay más de un .vcn o no hay ninguno, saltar un error
		if (candidatos.size() == 0) {
			throw new Exception("No puedo encontrar un makefile en ${workspace.getCanonicalPath()}")
		}
		if (candidatos.size() > 1) {
			throw new Exception("Hay más de un makefile en ${workspace.getCanonicalPath()}")
		}
		// Buscar candidatos a .vcp
		makefile = candidatos[0]
		File fichProyecto = new File([workspace.getCanonicalPath(), makefile.getName().substring(0, makefile.getName().lastIndexOf('.')) + ".vcp"].join(System.getProperty("file.separator")))
		// Si la fecha de última modificación del .vcp es posterior, saltar un
		//	error
		if (fichProyecto.exists()) {
			if (fichProyecto.lastModified() > makefile.lastModified()) {
				throw new Exception("El fichero del proyecto ha sufrido modificaciones no reflejadas en el Makefile")
			}
		}
	}
}