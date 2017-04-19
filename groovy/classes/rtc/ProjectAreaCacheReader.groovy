package rtc;

import java.beans.XMLDecoder;
import java.nio.file.ClosedWatchServiceException

import es.eci.utils.Stopwatch
import es.eci.utils.base.Loggable

public class ProjectAreaCacheReader extends Loggable {

	//----------------------------------------------------------
	// Propiedades de la clase
	
	// Referencia al fichero
	private Map<String, List<String>> map = null;
	
	//----------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Construye un lector sobre el fichero de áreas
	 * @param areasFile InputStream con la información de áreas de
	 * proyecto
	 */
	public ProjectAreaCacheReader(InputStream areasFile) {
		XMLDecoder decoder = new XMLDecoder(
			new BufferedInputStream(areasFile));
		map = (Map<String, List<String>>) decoder.readObject();
		decoder.close();
	}
	
	/**
	 * Obtiene el nombre área de proyecto correspondiente a una
	 * corriente. 
	 * @param stream Nombre de la corriente.
	 * @return Nombre del área de proyecto asociada si es posible, null
	 * 	en otro caso
	 */
	public String getProjectArea(String stream) {
		String ret = null;
		long millis = Stopwatch.watch {
			// Recorrer el mapa, indexado por área de proyecto
			Iterator<String> iterator = map.keySet().iterator();
			
			while (ret == null && iterator.hasNext()) {
				String area = iterator.next();
				List<String> streams = map.get(area);
				if (streams.contains(stream)) {
					ret = area;
				}
			}
		}
		log "Project area obtenida en $millis mseg."
		return ret;
	}
}
