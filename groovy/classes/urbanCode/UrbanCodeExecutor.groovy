package urbanCode

import es.eci.utils.NexusHelper
import es.eci.utils.TmpDir
import es.eci.utils.ZipHelper
import es.eci.utils.base.JSONBean
import es.eci.utils.base.Loggable
import es.eci.utils.commandline.CommandLineHelper
import groovy.json.JsonSlurper

/**
 * Esta clase ejecuta comandos contra UrbanCode valiéndose de la instalación 
 * local de udclient.
 * Documentación de udclient:
 * https://www-01.ibm.com/support/knowledgecenter/SS4GSP_6.1.2/com.ibm.udeploy.reference.doc/topics/cli_commands.html
 */
class UrbanCodeExecutor extends Loggable {
	
	//----------------------------------------------------------------------
	// Variables de la clase
	
	// Home del cliente udclient
	private String udClientCommand = null;
	// URL de udeploy
	private String urlUdeploy;
	// Credenciales
	private String user;
	private String password;
	
	//----------------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Construye un ejecutor de tareas Urban Code
	 * @param udclientHome Localización del cliente udclient
	 * @param urlUdeploy URL del servidor de uDeploy
	 * @param user Usuario de udeploy
	 * @param password Password de udeploy
	 */
	public UrbanCodeExecutor(String udClientCommand, String urlUdeploy, String user, String password) {
		this.udClientCommand = udClientCommand;
		this.urlUdeploy = urlUdeploy;
		this.user = user;
		this.password = password;
	}
	
	/**
	 * Este método obtiene un objeto snapshot a partir de unas coordenadas en nexus
	 * @param groupId Coordenadas en nexus: grupo
	 * @param artifact Coordenadas en nexus: artefacto
	 * @param snapshot Coordenadas en nexus: versión
	 * @param nexusURL URL de nexus del que leer la snapshot
	 * @return Objeto snapshot de urban code
	 */
	public UrbanCodeSnapshot downloadSnapshot(String groupId, String artifact, String snapshot, String nexusURL) {
		UrbanCodeSnapshot ret = null;
		TmpDir.tmp { File dir ->
			try {
				File zipFile = NexusHelper.downloadLibraries(groupId, artifact, snapshot, dir.getCanonicalPath(), "zip", nexusURL)
				ZipHelper.unzipFile(zipFile, dir)
				File jsonFile = new File(dir.getCanonicalPath() + System.getProperty("file.separator") + "descriptor.json")
				log "Leyendo descriptor.json..."
				ret = UrbanCodeSnapshot.parseJSON(jsonFile.text)
			}
			catch (FileNotFoundException fnf) {
				// El fichero todavía no se encuentra en Nexus
				ret = null
				log "No se ha encontrado el fichero descriptor.json"
			}
		}
		log("downloadSnapshot devuelve -> ${ret}")
		return ret;
	}
	
	/**
	 * Este método ejecuta un determinado comando en Urban Code a partir
	 * de un objeto expresable como json
	 * @param command Comando a ejecutar en udclient
	 * @param bean Objeto interpretable como json (si no es nulo, se crea un fichero
	 * json temporal y se pasa al comando)
	 */
	private Object executeCommand(String command, JSONBean bean = null) {
		def ret = null		
		TmpDir.tmp { File dir ->
			CommandLineHelper helper = null;
			if (bean != null) {
				log("Parametro \"bean\" != null.");
				File tmpJson = new File(dir.getCanonicalPath() + System.getProperty("file.separator") + "tmp.json")
				tmpJson.createNewFile()
				tmpJson.text = bean.toJSON()
				log "Parámetros a urban code:"
				log tmpJson.text 
				helper =
					new CommandLineHelper(
						"${udClientCommand} -username ${user} -password ${password} -weburl ${urlUdeploy} ${command} ${tmpJson.canonicalPath}")
			}
			else {
				log("Parametro \"bean\" == null.");
				helper = new CommandLineHelper(
						"${udClientCommand} -username ${user} -password ${password} -weburl ${urlUdeploy} ${command}")
			}
			helper.initLogger(this)
			// Ejecutar la creación de la instantánea contra udeploy
			int result = helper.execute(dir)
			String output = helper.getStandardOutput()
			String err = helper.getErrorOutput()
			if (err != null && !err.trim().equals("")) {
				log err
			}
			if (result != 0) {
				throw new Exception("[${result}] -> " + err)
			}
			if (output != null) {
				try {
					// La salida debe capturarse
					ret = new JsonSlurper().parseText(output)
				}
				catch(Throwable t) {
					ret = null;
					log "No se pudo parsear la salida"
				}
			}
		}
		return ret;
	}
	
	/**
	 * Este método invoca al cliente udclient para crear un snapshot adecuado en UrbanCode
	 * @param snapshot Versión de instantánea a crear
	 * @return Objeto parseado a partir del json devuelto por urbanCode, p. ej.: {
		  "id": "ff9274d4-bc3d-493c-90aa-5020e2fda56d",
		  "name": "My snapshot",
		  "description": "JPetStore snapshot",
		  "created": 1391451659050,
		  "active": true,
		  "locked": false
		}
	 */
	public Object createSnapshot(UrbanCodeSnapshot snapshot) {
		return executeCommand("createSnapshot", snapshot)
	}
	
	/**
	 * Este método invoca al cliente udclient para borrar un snapshot de UrbanCode
	 * @param application Nombre de la aplicación urban code
	 * @param snapshot Versión de instantánea a eliminar
	 */
	public void deleteSnapshot(String application, String snapshot) {
		executeCommand("deleteSnapshot -application \"${application}\" -snapshot \"${snapshot}\"");
	}	
		
	/**
	 * Este método lanza el despliegue de una instantánea parametrizada por
	 * el objeto 
	 * 
	{
		  "application": "00010-moonshine",
		  "applicationProcess": "Despliegue APP",
		  "environment": "NFT",
		  "onlyChanged": "false",
		  "snapshot": "ENTORNO-VERSION"
	}

	 * @param process Objeto con las propiedades necesarias para solicitar el lanzamiento
	 * del proceso
	 * @return Objeto con el resultado (parseado del json devuelto por el proceso), p. ej.: {
		  "requestId": "f7e7b00d-8ea6-4a95-ad74-0ff853125232"
		}
	 */
	public Object requestApplicationProcess(UrbanCodeApplicationProcess process) {
		return executeCommand("requestApplicationProcess", process)
	}
	
	/**
	 * Este método crea una versión de un componente en UrbanCode
	 * @param componentVersion Objeto con la información de versión
	 * @return Objeto con la información de retorno
	 */
	public Object createVersion(UrbanCodeComponentVersion componentVersion) {
		return executeCommand(
			"createVersion -component \"${componentVersion.component}\" -name \"${componentVersion.name}\"", null)
	}
	
	/**
	 * Este método solicita a Urban Code que actualice la última versión 
	 * disponible de un componente contra Nexus
	 * @param component Objeto con la información de componente
	 * @return No especificado en la documentación, si es posible se recoge
	 * y se parsea como json
	 */
	public Object importVersions(UrbanCodeComponent component) {
		return executeCommand("importVersions", component)
	}
	
	/**
	 * Devuelve la información de un componente, parseada como objeto json
	 * @param component Nombre de componente urban code
	 * @return Objeto json con la información del componente
	 */
	public Object getComponentInformation(String component) {
		return executeCommand("getComponent -component \"$component\"");
	}
}