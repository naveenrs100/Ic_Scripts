/**
 * Esta clase permite parsear un número de version contra el Dockerfile proporcionado por desarrollo.
 * Así como lanzar posteriormente la construcción de la imagen y la subida al repositorio final. 
 */
package docker;

import es.eci.utils.base.Loggable;
import es.eci.utils.commandline.CommandLineHelper
import es.eci.utils.pom.MavenCoordinates
import es.eci.utils.TmpDir;
import es.eci.utils.Stopwatch;
import es.eci.utils.StringUtil;
import es.eci.utils.ParameterValidator;

class DockerBuildImage extends Loggable {
	
	private String component;		// Nombre del componente
	private String registry;		// Registry docker
	private String builtVersion;	// Versión que se está construyendo
	private String proxy;			// Para descargas externas dentro de la imagen
	private String ws;				// Para el lanzamiento del build en el directorio correcto
	private String stream;			// RTC
	private String gitGroup;		// GIT
	
	private String imagenId;		// Para el borrado simultáneo

	// private final String registry = "10.252.80.41:5000" // http://10.252.80.41:5000/v2/_catalog PRE
	
	public void execute() {
		
		// Validación de obligatorios
		ParameterValidator.builder()
			.add("component", component)
			.add("builtVersion", builtVersion)
			.add("proxy", proxy)
			.add("ws", ws);
	
		long millis = Stopwatch.watch {
			
			try {
				
				File fichero = new File("Dockerfile")
				File dir_execute = new File (ws);
				String dockerFile = fichero.getText()
				String ficheroTmp = ""
				// Obtener del stream o el gitGroup un nombre final
				String app = decodeApp(stream, gitGroup)
				
				log "--- INFO: Reemplazando version en Dockerfile"				
				if (dockerFile != null && dockerFile.size() > 0) {
					
					// Reemplazo en dockerfile de información sensible
					ficheroTmp = dockerFile.replaceAll("##HTTP_PROXY##", proxy)
					fichero.setText(ficheroTmp.replaceAll("##BUILD_VERSION##", builtVersion))
					
					// Nombre final de la imagen
					String nombreImg = component.toLowerCase()
					
					log "--- INFO: OK"
					
					long millis_two = Stopwatch.watch {
						
						log "--- INFO: Lanzando construccion"
						CommandLineHelper comando = new CommandLineHelper("docker build -t ${nombreImg} .")
						comando.initLogger { println it }
						
						if (comando.execute(dir_execute) == 0)
							log "--- INFO: OK"
						else {
							log "### ERROR: Error al construir la imagen"
							throw new Exception("Error en construccion")
						}
					}
					
					log "Tiempo de generacion: ${millis_two} mseg."
					
					long millis_three = Stopwatch.watch {
						
						log "--- INFO: Publicando imagen"
						
						CommandLineHelper comando_tag = new CommandLineHelper(
							"docker tag ${nombreImg}:latest ${registry}/${app}/${nombreImg}:${builtVersion.toLowerCase()}")
						comando_tag.initLogger { println it }
						
						if (comando_tag.execute(dir_execute) == 0) {
							log "--- INFO: Tag OK"
							
							CommandLineHelper comando_push = new CommandLineHelper(
								"docker push ${registry}/${app}/${nombreImg}:${builtVersion.toLowerCase()}")
							comando_push.initLogger { println it }
							
							if (comando_push.execute(dir_execute) == 0) {
								log "--- INFO: Push OK"
								
								CommandLineHelper comando_rmi = new CommandLineHelper("docker rmi -f \$(docker images -q ${nombreImg})")
								comando_rmi.initLogger { println it }
								
								if (comando_rmi.execute(dir_execute) == 0) {
									log "--- INFO: Borrado OK de la imagen generada"
								} else {
									log "### ERROR: Error en el borrado de la imagen temporal" 
									throw new Exception("Error en publicacion")
								}
							} else {
								log "### ERROR: Error en el push de la imagen"
								throw new Exception("Error en publicacion")
							}							
						} else {
							log "### ERROR: Error al taggear la imagen"
							throw new Exception("Error en publicacion")
						}
					}
			
					log "Tiempo de publicacion: ${millis_three} mseg."
					
				} else {
					log "!!! WARNING: El fichero Dockerfile no es correcto"
				}
				
			} catch (FileNotFoundException fnfe) {
				log "--- INFO: No se ha podido abrir el Dockerfile, no se genera imagen docker"
			}			
						
		}
		
		log "Tiempo total: ${millis} mseg."
	
	}
	
	/**
	 * Método que utiliza la corriente de RTC o el grupo de GIT para generar el nombre "funcional" que agrupará
	 * las imágenes docker generadas. Siempre estará informado uno de los dos parámetros, ya sea git o rtc.
	 * @param rtc: Corriente de RTC
	 * @param git: Grupo de GIT
	 * @return Cadena tratada, el grupo de git se deja igual, pero al de RTC se le quitan los espacios.
	 */
	private String decodeApp (String rtc, String git) {
		
		if (!StringUtil.isNull(git))
			return git.toLowerCase();
		else {
			return StringUtil.cleanBlank(rtc.toLowerCase());	
		}		
	}
	
	/**
	 * @return the builtVersion
	 */
	public String getBuiltVersion() {
		return builtVersion;
	}
	
	/**
	 * @param builtVersion the builtVersion to set
	 */
	public void setBuiltVersion(String builtVersion) {
		this.builtVersion = builtVersion;
	}

	/**
	 * @return the component
	 */
	public String getComponent() {
		return component;
	}

	/**
	 * @param component the component to set
	 */
	public void setComponent(String component) {
		this.component = component;
	}

	/**
	 * @return the registry
	 */
	public String getRegistry() {
		return registry;
	}

	/**
	 * @param registry the registry to set
	 */
	public void setRegistry(String registry) {
		this.registry = registry;
	}

	/**
	 * @return the ws
	 */
	public String getWs() {
		return ws;
	}

	/**
	 * @param ws the ws to set
	 */
	public void setWs(String ws) {
		this.ws = ws;
	}

	/**
	 * @return the stream
	 */
	public String getStream() {
		return stream;
	}

	/**
	 * @param stream the stream to set
	 */
	public void setStream(String stream) {
		this.stream = stream;
	}

	/**
	 * @return the gitGroup
	 */
	public String getGitGroup() {
		return gitGroup;
	}

	/**
	 * @param gitGroup the gitGroup to set
	 */
	public void setGitGroup(String gitGroup) {
		this.gitGroup = gitGroup;
	}

	/**
	 * @return the proxy
	 */
	public String getProxy() {
		return proxy;
	}

	/**
	 * @param proxy the proxy to set
	 */
	public void setProxy(String proxy) {
		this.proxy = proxy;
	}

}
