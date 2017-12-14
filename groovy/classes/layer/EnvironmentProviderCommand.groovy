/**
 * Esta clase permite descargar de Nexus las librerias necesarias para el entorno de
 * compilación de Windows para Layer, en base a las librerías que se indiquen en el
 * fichero entorno_windows.comp
 */
package layer;

import es.eci.utils.base.Loggable;
import es.eci.utils.pom.MavenCoordinates
import es.eci.utils.NexusHelper
import es.eci.utils.Stopwatch;
import es.eci.utils.ParameterValidator;
import es.eci.utils.ZipHelper;
import groovy.lang.Closure;

class EnvironmentProviderCommand extends Loggable {
	
	private String urlNexus;				// URL de Nexus
	private String finalLocation;			// Destino de las descargas
	
	public void execute() {
			
		long millis = Stopwatch.watch {
			
			try {
				
				File ficheroEntorno = new File("entorno_windows.comp")
				
				log "--- INFO: Descargando librerias..."
							
				if (ficheroEntorno != null && ficheroEntorno.size() > 0) {
					
					def libs = new XmlSlurper().parse(ficheroEntorno)
					def librerias = libs.lib
					for ( int i = 0; i < librerias.size(); i++ ) {
						String groupId 		= librerias[i].groupId.text().replace(".", "/");
						String artifactId 	= librerias[i].artifactId.text()
						String version 		= librerias[i].version.text();
						String type 		= librerias[i].type.text();
						
						File ficheroNexus = null;
						
						long millis_two = Stopwatch.watch {
						
							ficheroNexus = new NexusHelper(urlNexus).download(
							new MavenCoordinates(groupId, artifactId, version, type),
							new File(finalLocation))
						
						}
					
						log "Tiempo de descarga desde Nexus: ${millis_two} mseg."
						
						log "--- INFO: Descomprimiendo librerias..."
						
						long millis_three = Stopwatch.watch {
							
							ZipHelper.unzipFile(ficheroNexus, new File(finalLocation))
							ficheroNexus.delete()
							
						}
						
						log "Tiempo de instalacion: ${millis_three} mseg."
					
					}
										
				} else {
					log "!!! WARNING: El fichero entorno_windows.comp no existe o no es correcto"
					throw new Exception("Error en apertura del fichero entorno_windows.comp")
				}
				
			} catch (Exception e) {
				log "### ERROR: Error al provisionar el entorno"
			}			
						
		}
		
		log "Tiempo total: ${millis} mseg."
	
	}
	
	/**
	 * @return the urlNexus
	 */
	public String getUrlNexus() {
		return urlNexus;
	}

	/**
	 * @param urlNexus the urlNexus to set
	 */
	public void setUrlNexus(String urlNexus) {
		this.urlNexus = urlNexus;
	}

	/**
	 * @return the finalLocation
	 */
	public String getFinalLocation() {
		return finalLocation;
	}

	/**
	 * @param finalLocation the finalLocation to set
	 */
	public void setFinalLocation(String finalLocation) {
		this.finalLocation = finalLocation;
	}

}
