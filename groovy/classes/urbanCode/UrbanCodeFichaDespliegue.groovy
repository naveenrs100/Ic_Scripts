
package urbanCode

import es.eci.utils.base.Loggable;
import es.eci.utils.pom.MavenCoordinates
import es.eci.utils.NexusHelper
import es.eci.utils.ParameterValidator;
import es.eci.utils.TmpDir;
import es.eci.utils.Stopwatch;
import es.eci.utils.StringUtil
import es.eci.utils.ZipHelper
import groovy.json.JsonSlurper

/**
 * Esta clase genera la ficha para urban, a partir de un grupo de repositorios, o bien una
 * corriente de RTC. Se encarga de dar de alta las versiones de los
 * componentes en Urban y posteriormente genera la ficha. Puede ser invocada durante una
 * construcción, en cuyo caso utilizará el descriptor de la misma, o a petición, en este
 * caso se descargará el descriptor de Nexus a partir del nombre de la instantánea y del
 * nombre de la aplicación en Urban.
 * <br/>
 * Notar que es <b>imprescindible</b> contar con una instalación local del cliente.  Se puede provisionar
 * desde:<br/>
 * <a href="http://nexus.elcorteingles.int/service/local/repositories/GC/content/ibm/urbanCode/udclient/6.1.0/udclient-6.1.0.zip">Cliente udclient en Nexus</a>
 * <br/> 
 * @see <a href="https://www-01.ibm.com/support/knowledgecenter/SS4GSP_6.1.2/com.ibm.udeploy.reference.doc/topics/cli_commands.html">Documentación del cliente udclient en IBM</a>
 */
class UrbanCodeFichaDespliegue extends Loggable {
	
	//---------------------------------------------------------------	
	private String udClientCommand;
	private String urlUrbanCode;
	private String urlNexus;
	private String urbanUser;
	private String urbanPassword;
	
	// Propiedades de la clase
	UrbanCodeSnapshot componentSnapshots = new UrbanCodeSnapshot();
	
	// Variables opcionales
	// El descriptor viene informado cuando esta clase se invoca dentro de una construcción.
	private String descriptor;
	// Cuando se invoca a petición, para generar una ficha concreta es necesario que la
	// aplicación y la instantánea estén informados.
	private String nombreAplicacionUrban; 
	private String instantaneaUrban;
	// Componentes que no existen en UrbanCode
	private List<String> errorComp = [];
	// Si se ha lanzado desde componente es necesario conocer el entorno de despliegue
	private String entornoUrban;
	// Si el despliegue conlleva parada de servicio
	private boolean serviceStop;
	
	// Identificador del proceso si la construcción ha sido iniciada por Clarive
	private String permisoClarive;
	
	//---------------------------------------------------------------
	// Métodos de la clase
	
	def isNull = { String s ->
		return s == null || s.trim().length() == 0;
	}
	
	public void execute() {
		
		TmpDir.tmp { File tmpDir ->			
			long millis = Stopwatch.watch {
				
				log ""
				log "--- INFO: Inicio de tratamiento con Urban"
				log ""

				if (isNull(descriptor)) {
					if ( (isNull(nombreAplicacionUrban)) || (isNull(instantaneaUrban)) ) {
						log "### ERROR: Parametros de construccion insuficientes"
						throw new Exception("Error al ejecutar la ficha")
					} else {
						// Obtener el descriptor de Nexus
						log "--- INFO: Obteniendo descriptor para [${instantaneaUrban}] de Nexus..."
						
						String artifactId = StringUtil.normalize(nombreAplicacionUrban);
						MavenCoordinates coordenadasMaven = new MavenCoordinates("es.eci.fichas_urbancode", artifactId, 
							instantaneaUrban)
						coordenadasMaven.setRepository("fichas_despliegue")
						coordenadasMaven.setPackaging("zip")
						descriptor = loadDescriptorFromNexus (coordenadasMaven, tmpDir)
					}
				}
					
				// Si hemos llegado hasta aquí, se ha resuelto correctamente el descriptor
				log "--- INFO: Descriptor: " + descriptor
				errorComp = checkAndCreateComponentVersions(descriptor)
				
				if ( (errorComp != null) && (errorComp.size() > 0) ) {
					log "--- INFO: Los siguientes componentes deben darse de alta en UrbanCode antes de continuar"
					errorComp.each {
						log "--- " + it
					}
					//throw new Exception("Error al ejecutar la ficha")
				// Todos los componentes implicados existen en Urban
				} else {
				
					// Creación de la snapshot en Urban
					long millis_two = Stopwatch.watch {						
						executeUrbanSnapshot(descriptor)
					}

					log "Tiempo de ejecucion snapshot: ${millis_two} mseg."
					
					// Lanzamiento del deploy en Urban
					long millis_three = Stopwatch.watch {
						// ¿Hay entorno? - Una # indica que no se desplegará
						if ( isNull(entornoUrban) || entornoUrban.contains("#") ) {
							log "--- INFO: No está activado el despliegue automático en entorno desde QUVE"
						} else if( !isNull(permisoClarive) && permisoClarive.equals("true") ) {
							log "--- INFO: Proceso iniciado desde Clarive (permisoClarive = ${permisoClarive}). El encargado del despliegue va a ser Clarive."
						} else {
							log "--- INFO: Se procede a lanzar el despliegue en Urban"
							executeUrbanDeploy(descriptor, isServiceStop());
						}
					}
					
					log "Tiempo de ejecucion deploy Urban: ${millis_three} mseg."
					
				} // fin else (errorComp != null) && (errorComp.size() > 0)
			} // fin watch principal
			
			log "Tiempo total ejecucion: ${millis} mseg."
		}
	}
	
	/**
	 * Método que da de alta la versión de cada componente en UrbanCode. Si ya existe dicha
	 * versión, informa de ello. En caso de otro error, continuará de todas maneras.
	 * @param desc: Descriptor generado durante la compilación con las versiones de todos
	 * los componentes UrbanCode.
	 * @return Devuelve los componentes de los que no ha podido informarse la version. 
	 */	
	public List<String> checkAndCreateComponentVersions (String desc) {
		
		// Componentes que no existen en Urban
		List<String> notVersioned = []
		
		def versions;
		String componente = "";
		String version = "";
		String result = "";
		
		log "--- INFO: Revisando componentes del descriptor contra Urban..."
		JsonSlurper jsonSlurper = new JsonSlurper();
		def jsonObject = jsonSlurper.parseText(desc);
		versions = jsonObject.versions;
			
		// Por cada versión/elemento del descriptor llamamos a la comprobación.
		versions.each {
			componente = it.keySet().iterator().next();
			version = it[componente];
				
			log "--- INFO: Creando version del componente [${componente}] - [${version}] en UrbanCode..."
				
			// El resultado será OK si ya existe o se ha creado la versión.
			result = checkAndCreateComponentVersion(componente,version);
				
			// Si no es OK, hubo problemas con Urban
			if ( result != "OK") {
				notVersioned.add(result)
			}
		}				
		
		return notVersioned;
		
	}
	
	/**
	 * Método que descarga el zip del descriptor.json desde Nexus en base a las coordenadas
	 * Maven que se le facilitan, descomprime el zip en el directorio de lanzamiento.
	 * @param coords: Coordenadas maven con la ubicación del descriptor.json en Nexus.
	 * @return Develve un String con el contenido del fichero descriptor.json
	 */
	private String loadDescriptorFromNexus (MavenCoordinates coords, File dir) {
				
		try {
			// Llamada a Nexus para recuperar el fichero descriptor.json almacenado
			NexusHelper nexusExecutor = new NexusHelper(urlNexus)
			nexusExecutor.initLogger(this)
			
			// Fichero descriptor.json de destino
			File targetDescriptor = nexusExecutor.download(coords, dir)
			
			// Descompresión del zip de Nexus
			ZipHelper zipExecutor = new ZipHelper()
			zipExecutor.unzipFile(targetDescriptor, dir)
		} catch (FileNotFoundException fnfe) {
			log "### ERROR: No se ha encontrado el descriptor en Nexus o no se ha podido escribir en destino"
			throw new Exception("Error al ejecutar la ficha")
		}
		
		// Devuelve el nombre final del descriptor
		return new File (dir, "descriptor.json").getText()
	}
	
	/**
	 * Genera un objeto de tipo UrbanCodeSnapshot a partir del descriptor. Este objeto tiene la
	 * información necesaria para crear la ficha en UrbanCode
	 * @param desc Descriptor obtenido desde Nexus o de la propia construcción.
	 */
	private void executeUrbanSnapshot (String desc) {	
		UrbanCodeExecutor urbanExecutor = new UrbanCodeExecutor(udClientCommand,
			urlUrbanCode, urbanUser, urbanPassword);
		log "--- INFO: Creando Snapshot contra UrbanCode..."
		
		// En el descriptor esta toda la informacion del despliegue
		def report = UrbanCodeSnapshot.parseJSON(desc)
		
		// Borramos la nightly anterior
		if (report.name.contains("nightly")) {
			try {
				urbanExecutor.deleteSnapshot(report.application, report.name)
				log "--- INFO URBANCODE: [" + report.name + "] del aplicativo [" + report.application + "] eliminada"
			}
			catch(Exception e) {
				log "--- INFO: No se ha encontrado nightly anterior, por lo tanto, no se elimina"
			}
			
		}
		
		// Parseo del descriptor a formato snapshot y lanzamiento en UrbanCode
		try {
			HashMap jsonResponse = urbanExecutor.createSnapshot(UrbanCodeSnapshot.parseJSON(desc))
			log "--- INFO URBANCODE: OK - id de snapshot: " + jsonResponse.get("id");
		} catch (Exception e) {
			if (e.getMessage().contains("already exists for this application.")) {
				log "--- INFO URBANCODE: La snapshot ya existe en UrbanCode"
			} else if (e.getMessage().contains("Invalid UUID string:")) {
				log "### ERROR URBANCODE: La aplicacion [${UrbanCodeSnapshot.parseJSON(desc).application}] no existe en UrbanCode"
				//throw new Exception("Error al ejecutar la ficha")
			} else {
				log "!!! WARNING URBANCODE: Error no controlado. ${e.getMessage()}"
				throw new Exception("Error al ejecutar la ficha")
			}
		}
	}
	
	/**
	 * Ejecuta el despliegue en UrbanCode, si te trata de una versión nightly agrega el modificador de despliegue SNAPSHOT
	 * @param desc Descriptor obtenido desde Nexus o de la propia construcción.
	 * @param stop Indica si el despliegue provoca pérdida de servicio.
	 */
	private void executeUrbanDeploy (String desc, boolean stop) {
		
		UrbanCodeExecutor urbanExecutor = new UrbanCodeExecutor(udClientCommand,
			urlUrbanCode, urbanUser, urbanPassword);
		
		// En el descriptor esta toda la informacion del despliegue
		def report = UrbanCodeSnapshot.parseJSON(desc)
		
		try {
			
			if(stop) {
				log "--- INFO: El siguiente despliegue implica parada de servicio"
			}
			
			UrbanCodeApplicationProcess process = new UrbanCodeApplicationProcess (
				report.application,
				stop?Constants.DEPLOY_PROCESS_STOP:Constants.DEPLOY_PROCESS,
				entornoUrban,
				true,
				report.name,
				report.name.contains("nightly")?["ETIQUETA":"-SNAPSHOT"]:[:]
			);
		
			log "Comando a Urban: " + process.toJSON()
			
			// Recuperación de la respuesta del comando
			HashMap jsonResponse = urbanExecutor.requestApplicationProcess(process)
			
			log "--- INFO URBANCODE: OK - requestId: " + jsonResponse.get("requestId")
			
		} catch (Exception e) {
			if (e.getMessage().contains("The value given for \"environment\"")) {
				log "### ERROR URBANCODE: El entorno [${entornoUrban}] no existe en UrbanCode"
				//throw new Exception("Error al ejecutar la ficha")
			} else {
				log "!!! WARNING URBANCODE: Error no controlado. ${e.getMessage()}"
				throw new Exception("Error al ejecutar la ficha")
			}
		}
	}
	
	/**
	 * Comprueba si un componente existe en Urban y si no es así lo crea. Controla los posibles errores.
	 * @param comp Componente Urban
	 * @param ver Version del componente
	 * @return OK: Si el componente existe con esa versión en Urban o se ha sido creado / actualizado. El
	 * nombre del componente: dicho componente no existe en Urban. El nombre del componente - ESTADO 
	 * DESCONOCIDO: Hubo un problema no controlado en Urban al consultar el componente indicado. 
	 */
	private String checkAndCreateComponentVersion (String comp, String ver) {
		
		try {
			UrbanCodeExecutor urbanExecutor = new UrbanCodeExecutor(udClientCommand,
				urlUrbanCode, urbanUser, urbanPassword);
			
			UrbanCodeComponentVersion componentVersion = new UrbanCodeComponentVersion(
				comp, ver, null, null)
			
			urbanExecutor.createVersion(componentVersion)
			
			log "--- INFO URBANCODE: OK"
			return "OK"
		} catch (Exception e) {
			if (e.getMessage().contains("already exists for Component")) {
				log "--- INFO URBANCODE: Version ya dada de alta en UrbanCode para ese componente"
				return "OK"
			} else if (e.getMessage().contains("No component for ${comp}")) {
				log "### ERROR URBANCODE: No existe el componente [${comp}] en UrbanCode"
				return comp;
			} else {
				log "!!! WARNING URBANCODE: Error no controlado. ${e.getMessage()}"
				return "${comp} - ESTADO DESCONOCIDO"
			}
		}
		
	}
	
	/**
	 * @return the descriptor
	 */
	public String getDescriptor() {
		return descriptor;
	}
	
	/**
	 * @param descriptor the descriptor to set
	 */
	public void setDescriptor(String descriptor) {
		this.descriptor = descriptor;
	}
	
	/**
	 * @return the nombreAplicacionUrban
	 */
	public String getNombreAplicacionUrban() {
		return nombreAplicacionUrban;
	}
	
	/**
	 * @param nombreAplicacionUrban the nombreAplicacionUrban to set
	 */
	public void setNombreAplicacionUrban(String nombreAplicacionUrban) {
		this.nombreAplicacionUrban = nombreAplicacionUrban;
	}
	
	/**
	 * @return Nombre de la instantánea en Urban Code
	 */
	public String getInstantaneaUrban() {
		return instantaneaUrban;
	}
	
	/**
	 * @param instantaneaUrban Nombre de la instantánea en Urban Code
	 */
	public void setInstantaneaUrban(String instantaneaUrban) {
		this.instantaneaUrban = instantaneaUrban;
	}

	/**
	 * @return Ruta completa del ejecutable del cliente udclient en disco duro
	 */
	public String getUdClientCommand() {
		return udClientCommand;
	}

	/**
	 * @param udClientCommand Ruta completa del ejecutable del cliente udclient en disco duro
	 */
	public void setUdClientCommand(String udClientCommand) {
		this.udClientCommand = udClientCommand;
	}

	/**
	 * @return URL de Urban Code
	 */
	public String getUrlUrbanCode() {
		return urlUrbanCode;
	}

	/**
	 * @param urlUrbanCode URL de Urban Code
	 */
	public void setUrlUrbanCode(String urlUrbanCode) {
		this.urlUrbanCode = urlUrbanCode;
	}
	
	/**
	 * @return URL de Nexus corporativo
	 */
	public String getUrlNexus() {
		return urlNexus;
	}

	/**
	 * @param urlNexus URL de Nexus corporativo
	 */
	public void setUrlNexus(String urlNexus) {
		this.urlNexus = urlNexus;
	}

	/**
	 * @return Usuario Urban Code
	 */
	public String getUrbanUser() {
		return urbanUser;
	}

	/**
	 * @param urbanUser Usuario Urban Code
	 */
	public void setUrbanUser(String urbanUser) {
		this.urbanUser = urbanUser;
	}

	/**
	 * @return Contraseña de Urban Code
	 */
	public String getUrbanPassword() {
		return urbanPassword;
	}

	/**
	 * @param urbanPassword Contraseña de Urban Code
	 */
	public void setUrbanPassword(String urbanPassword) {
		this.urbanPassword = urbanPassword;
	}

	/**
	 * @return Nombre lógico del entorno de despliegue, en caso de ir informado
	 */
	public String getEntornoUrban() {
		return entornoUrban;
	}

	/**
	 * @param entornoUrban Nombre lógico del entorno de despliegue, 
	 * 	en caso de ir informado
	 */
	public void setEntornoUrban(String entornoUrban) {
		this.entornoUrban = entornoUrban;
	}

	/**
	 * @return Cierto si el despliegue conlleva parada de servicio
	 */
	public boolean isServiceStop() {
		return serviceStop;
	}

	/**
	 * @param serviceStop Cierto si el despliegue conlleva 
	 * 	parada de servicio
	 */
	public void setServiceStop(boolean serviceStop) {
		this.serviceStop = serviceStop;
	}

	/**
	 * 
	 * @return
	 */
	public String getPermisoClarive() {
		return permisoClarive;
	}

	/**
	 * @param permisoClarive True si la construcción ha sido llevada a cabo por Clarive
	 */
	public void setPermisoClarive(String permisoClarive) {
		this.permisoClarive = permisoClarive;
	}

}
