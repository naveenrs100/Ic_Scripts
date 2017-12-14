/**
 * Esta clase permite ejecutar MSBuild en cada una de las soluciones que encuentre
 * en el componente.
 */
package layer;

import es.eci.utils.base.Loggable;
import es.eci.utils.commandline.CommandLineHelper;
import es.eci.utils.pom.MavenCoordinates
import es.eci.utils.NexusHelper
import es.eci.utils.Stopwatch;
import es.eci.utils.ParameterValidator;
import es.eci.utils.ZipHelper;
import groovy.lang.Closure;

class MSBuildExecutorCommand extends Loggable {
	
	private String msbuildPath;				// Ruta del ejecutable MSBuild
	private String componentHome;			// Raíz del componente
	private String [] ficheros;				// Lista de ficheros de soluciones
	
	public void execute() {

		long millis = Stopwatch.watch {
			
			try {
				
				ficheros = searchComponent(new File(getComponentHome()))
					
				ficheros.each {
					log "------------------------------------------------------"
					log "--- Compilando " + it + "..."
					log "------------------------------------------------------"
					CommandLineHelper execCommand = new CommandLineHelper( getMsbuildPath() + " " + it)
					execCommand.initLogger(this)
					execCommand.execute()
					log "------------------------------------------------------"
					log "--- Timepo de ejecucion: " + execCommand.lastExecutionTime
					log "------------------------------------------------------"
				}
				
			} catch (Exception e) {
				log "### ERROR: Error al recorrer el componente"
			}
						
		}
		
		log "Tiempo total: ${millis} mseg."
	
	}
	
	private String [] searchComponent(File directory) {
		
		def files = []
		
		log "directory: " + directory.canonicalPath
		
		directory.traverse (
			type: groovy.io.FileType.FILES,
			nameFilter: ~/.*\.sln$/,
			maxDepth: -1
		) {
			files << it
		}
		
		return files
		
	}

	/**
	 * @return the msbuildPath
	 */
	public String getMsbuildPath() {
		return msbuildPath;
	}

	/**
	 * @param msbuildPath the msbuildPath to set
	 */
	public void setMsbuildPath(String msbuildPath) {
		this.msbuildPath = msbuildPath;
	}

	/**
	 * @return the componentHome
	 */
	public String getComponentHome() {
		return componentHome;
	}

	/**
	 * @param componentHome the componentHome to set
	 */
	public void setComponentHome(String componentHome) {
		this.componentHome = componentHome;
	}

}
