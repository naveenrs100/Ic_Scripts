package es.eci.utils.transfer

import es.eci.utils.Utiles
import es.eci.utils.base.Loggable
import groovy.io.FileVisitResult

/**
 * Esta clase copia un fichero o directorio completo a una máquina remota
 * por medio de un cliente FTP.
 */
class FileDeployer extends Loggable {
	
	//--------------------------------------------------------
	// Propiedades de la clase
	
	// Implementación simple de cliente FTP
	private FTPClient client;

	//--------------------------------------------------------
	// Métodos de la clase
	
	
	/**
	 * Inicializa el cliente FTP del deployer.
	 * @param user Usuario FTP de la máquina remota.
	 * @param password Contraseña del usuario FTP en la máquina remota.
	 * @param targetAddress URL de la máquina remota.
	 */
	public FileDeployer(String user, String password, String targetAddress) {
		this.client = new FTPClient(user, password, targetAddress);
	}	
	
	/**
	 * Despliega el fichero o directorio indicado con su contenido en la máquina
	 * remota.
	 * @param source Directorio o fichero de origen.
	 * @param targetDirectory Directorio de destino en la máquina remota.
	 * @param includes Opcional.  Si viene informado, es un filtro que se aplica a 
	 * 	los nombres de los ficheros, como patrón de inclusión.
	 */
	public void deploy(File source, String targetDirectory, String includes = null) {
		client.initLogger(this);
		if (source.isFile()) {
			// Caso trivial
			if (includes == null || source.getName() =~ /${includes}/) {
				client.copy(source, targetDirectory);
			}
		}
		else if (source.isDirectory()) {
			def filter = null;
			if (includes != null && includes.trim().size() > 0) {
				filter = ~/${includes}/;
			}
			source.traverse(
				type: groovy.io.FileType.FILES,
				preDir: { if (it.name.startsWith(".") || it.name == 'target') return FileVisitResult.SKIP_SUBTREE},
				nameFilter: filter,
				maxDepth: -1
			){ File it ->
				client.copy(it, targetDirectory + '/' + Utiles.rutaRelativa(source, it));
			}
		}
		client.flush();
	}
}
