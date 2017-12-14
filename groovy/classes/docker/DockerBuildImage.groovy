/**
 * Esta clase permite parsear un número de version contra el Dockerfile proporcionado por desarrollo.
 * Así como lanzar posteriormente la construcción de la imagen y la subida al repositorio final. 
 */
package docker;

import es.eci.utils.base.Loggable;
import es.eci.utils.commandline.CommandLineHelper
import es.eci.utils.pom.MavenCoordinates
import es.eci.utils.NexusHelper
import es.eci.utils.TmpDir;
import es.eci.utils.Stopwatch;
import es.eci.utils.StringUtil;
import es.eci.utils.ParameterValidator;
import es.eci.utils.ZipHelper;
import groovy.lang.Closure;

class DockerBuildImage extends Loggable {
	
	private String action;			// Acción que desencadena la construcción
	private String component;		// Nombre del componente
	private String registry;		// Registry docker
	private String builtVersion;	// Versión que se está construyendo
	private String proxy;			// Para descargas externas dentro de la imagen
	private String ws;				// Para el lanzamiento del build en el directorio correcto
	private String maven;			// Para la subida a Nexus de la configuración de DC/OS
	private String urlNexus;		// Idem anterior
	private String stream;			// RTC
	private String gitGroup;		// GIT
	
	private String imagenId;		// Para el borrado simultáneo

	// private final String registry = "10.252.80.41:5000" // http://10.252.80.41:5000/v2/_catalog PRE
	
	public void execute() {
		
		// Validación de obligatorios
		ParameterValidator.builder()
			.add("action", action)
			.add("component", component)
			.add("builtVersion", builtVersion)
			.add("proxy", proxy)
			.add("ws", ws)
			.add("maven", maven)
			.add("urlNexus", urlNexus).build().validate();
	
		long millis = Stopwatch.watch {
			
			try {
				
				File fichero = new File("Dockerfile")
				File dir_execute = new File (ws);
				String dockerFile = fichero.getText()
				String ficheroTmp = ""
				String app = ""
				// Obtener del stream o el gitGroup un nombre final
				if (StringUtil.isNull(stream))
					app = decodeName(gitGroup)
				else
					app = decodeName(stream)
				
				log "--- INFO: Reemplazando version en Dockerfile"				
				if (dockerFile != null && dockerFile.size() > 0) {
					
					// Reemplazo en dockerfile de información sensible
					ficheroTmp = dockerFile.replaceAll("##HTTP_PROXY##", proxy)
					fichero.setText(ficheroTmp.replaceAll("##BUILD_VERSION##", builtVersion))
					
					// Nombre final de la imagen
					String nombreImg = decodeName(component)
					
					// Sufijo para nombre de la imagen nightly/latest
					String sufijoNombreImg = action == "deploy"? "nightly" : "latest"
					
					log "--- INFO: OK"
					
					long millis_two = Stopwatch.watch {
						
						log "--- INFO: Lanzando construccion"
						CommandLineHelper comando = new CommandLineHelper("docker build -t ${nombreImg}:${sufijoNombreImg} .")
						comando.initLogger(this)
						
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
						
						int resultadoComando = 0
						
						CommandLineHelper comando_tag = new CommandLineHelper(
							"docker tag -f ${nombreImg}:${sufijoNombreImg} ${registry}/${app}/${nombreImg}:${builtVersion}")
						comando_tag.initLogger(this)
						
						resultadoComando = comando_tag.execute(dir_execute)
						
						if (resultadoComando == 0) {
							log "--- INFO: Tag OK"
							
							CommandLineHelper comando_push = new CommandLineHelper(
								"docker push ${registry}/${app}/${nombreImg}:${builtVersion}")
							comando_push.initLogger(this)
							
							resultadoComando = comando_push.execute(dir_execute)
							
							if (resultadoComando == 0) {
								log "--- INFO: Push OK"
								
								if (action == 'deploy') {
									comando_tag = new CommandLineHelper(
										"docker tag -f ${nombreImg}:nightly ${registry}/${app}/${nombreImg}:nightly")
									comando_tag.initLogger(this)
								
									resultadoComando = comando_tag.execute(dir_execute)
								
									if (resultadoComando == 0) log "--- INFO: Tag Nightly OK"
									
									comando_push = new CommandLineHelper(
										"docker push ${registry}/${app}/${nombreImg}:nightly")
									comando_push.initLogger(this)
									
									resultadoComando = comando_push.execute(dir_execute)
									
									if (resultadoComando == 0) log "--- INFO: Push Nightly OK"
								}
								
								CommandLineHelper comando_rmi = new CommandLineHelper(
									"docker rmi -f ${registry}/${app}/${nombreImg}:${builtVersion} \
									${registry}/${app}/${nombreImg}:nightly \
									${nombreImg}:latest ${nombreImg}:nightly")
								comando_rmi.initLogger(this)
								
								uploadDcosConfig()
								
								if (comando_rmi.execute(dir_execute) == 0) {
									log "--- INFO: Borrado OK de la imagen generada"
								} else {
									// Se ha hecho el mejor esfuerzo para limpiarla
									log "### WARNING: Error en el borrado de la imagen temporal" 
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
	 * las imágenes docker generadas.
	 * @param rtc: Corriente de RTC o grupo de GIT
	 * @return Cadena tratada.
	 */
	private String decodeName (String cadena) {
		String theString = StringUtil.trimStreamName(cadena).toLowerCase()
		return StringUtil.normalize(theString)
	}
	
	/**
	 * Método que crea un zip con todos los ficheros existentes en la ruta dcos/ en la raíz del repositorio. Si
	 * dicha ruta no existe no hace nada.
	 * @return N/A
	 */
	private void uploadDcosConfig () {
		
		NexusHelper nHelper = new NexusHelper(urlNexus);
		
		File dir_dcos = new File (ws + "/dcos");
		
		if ( dir_dcos != null && dir_dcos.exists() ) {
			
			File zip = ZipHelper.addDirToArchive(dir_dcos)
			String artifactId = decodeName(component) + "_dcos"
			String repository = ""
			
			if (builtVersion.contains("SNAPSHOT"))
				repository = urlNexus + "/content/repositories/fichas_dcos-snapshots"
			else
				repository = urlNexus + "/content/repositories/fichas_dcos-releases"
			
			try {
				log "--- INFO: Subiendo la configuracion de DC/OS a: G:[es.eci.dcos-config] A:[${artifactId}] V:[${builtVersion}]"
				
				// Maven: Se asume lanzamiento en linux con la instalación de maven en el path
				nHelper.uploadToNexus(
					"mvn",
					"es.eci.dcos-config",
					artifactId,
					"${builtVersion}",
					zip.getCanonicalPath(),
					repository,
					"zip"
				)
						
				log "--- INFO: OK Subida configuracion DC/OS"
			} catch (Exception e) {
				log "### ERROR: Ha habido un problmema subiendo la configuracion DC/OS a Nexus"
				e.printStackTrace();
			}
			finally {
				zip.delete()
			}
		} else {
			log "--- INFO: No hay directorio dcos, no se envian datos de configuracion a Nexus"
		}
	}
	
	/**
	 * 
	 * @return the action
	 */
	public String getAction() {
		return action;
	}

	/**
	 * 
	 * @param action the action to set
	 */
	public void setAction(String action) {
		this.action = action;
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
	 * @return the maven
	 */
	public String getMaven() {
		return maven;
	}

	/**
	 * @param maven the maven to set
	 */
	public void setMaven(String maven) {
		this.maven = maven;
	}

	/**
	 * @return the nexusUrl
	 */
	public String getUrlNexus() {
		return urlNexus;
	}

	/**
	 * @param nexusUrl the nexusUrl to set
	 */
	public void setUrlNexus(String urlNexus) {
		this.urlNexus = urlNexus;
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
