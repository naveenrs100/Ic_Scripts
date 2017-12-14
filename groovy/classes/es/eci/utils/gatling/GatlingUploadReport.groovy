package es.eci.utils.gatling;

/**
 * Esta clase prepara el informe y lo sube a la ruta de publicación
 */

import java.io.File;

import es.eci.utils.ZipHelper;
import es.eci.utils.base.Loggable;

class GatlingUploadReport extends Loggable {
	
	public void execute() {
		
		// Eliminar el log de la simulación
		def deleteFileNames = new FileNameFinder().getFileNames('reports', '**/*.log')
		deleteFileNames.each {
			new File (it).delete();
			log "Fichero borrado: " + it
		}
		
		// Crear fichero zip
		File reportDir = new File ("reports")
		File zip = ZipHelper.addDirToArchive(reportDir, new File("informe.zip"))
		log "Zip creado: ${zip.getCanonicalPath()}"
	
	}
	
}