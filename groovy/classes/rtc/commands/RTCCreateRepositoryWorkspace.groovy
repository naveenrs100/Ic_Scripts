package rtc.commands

import rtc.RTCUtils
import es.eci.utils.ParameterValidator
import es.eci.utils.ScmCommand
import es.eci.utils.Stopwatch
import es.eci.utils.TmpDir
import groovy.json.JsonSlurper

/**
 * Esta clase agrupa la funcionalidad para comprobar si un WSR determinado
 * existe, y si no, crearlo al vuelo, asociado a una determinada corriente
 * y con un determinado componente.
 * <br/>
 * Parámetros de entrada:<br/>
 * <br/>
 * --- OBLIGATORIOS<br/>
 * <b>scmToolsHome</b> Directorio raíz de las herramientas RTC <br/>
 * <b>userRTC</b> Usuario RTC<br/>
 * <b>pwdRTC</b> Password RTC<br/>
 * <b>urlRTC</b> URL de repositorio RTC<br/>
 * <b>workspaceRTC</b> Nombre del workspace de repositorio que se quiere crear<br/>
 * <b>stream</b> Corriente a la que se asocia el workspace de repositorio<br/>
 * --- OPCIONALES<br/>
 * <b>component</b> Componente que se debe añadir al wsr.
 * <b>light</b> Indica si se debe usar o no la versión ligera del comando scm.  VALOR POR DEFECTO: true<br/>
 */
class RTCCreateRepositoryWorkspace extends AbstractRTCCommand {

	//---------------------------------------------------------------
	// Propiedades de la clase
	
	// Obligatorios	
	private String workspaceRTC;
	private String stream;
	private String component;
	
	//---------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Asigna el nombre del wsr.
	 * @param workspaceRTC Nombre del workspace de repositorio que se quiere crear.
	 */
	public void setWorkspaceRTC(String workspaceRTC) {
		this.workspaceRTC = workspaceRTC;
	}
	
	/**
	 * Asigna el nombre de la corriente.
	 * @param stream Corriente a la que se asocia el workspace de repositorio.
	 */
	public void setStream(String stream) {
		this.stream = stream;
	}

	/**
	 * Asigna el nombre del componente.
	 * @param component Componente que se debe añadir al wsr.
	 */
	public void setComponent(String component) {
		this.component = component;
	}


	// Ejecución del comando con las credenciales y directorio indicado
	private String executeScmCommand(ScmCommand theCommand, String str, File dir) {
		return theCommand.ejecutarComando(
			str, 
			userRTC, 
			pwdRTC, 
			urlRTC, 
			dir)
	}	
	
	@Override
	public void execute() {
		TmpDir.tmp { File daemonsConfigDir ->
			// Validación de obligatorios
			ParameterValidator.builder()
					.add("scmToolsHome", scmToolsHome)
					.add("userRTC", userRTC)
					.add("pwdRTC", pwdRTC)
					.add("urlRTC", urlRTC)
					.add("workspaceRTC", workspaceRTC)
					.add("stream", stream)
					.add("light", light).build().validate();
			
			long millis = Stopwatch.watch {
				ScmCommand command = null;
				TmpDir.tmp { File dir ->
					try {
						command = new ScmCommand(light, scmToolsHome, daemonsConfigDir.getCanonicalPath());
						command.initLogger(this);	
						
						// ¿Existe el WSR?
						boolean exist = true;
						// "status-code": 25, cuando no lo encuentra
						String jsonWSR = executeScmCommand(command, "get attributes -w \"${workspaceRTC}\" --description -j", dir);
						RTCUtils.exitOnError(command.getLastResult(), "Querying repository workspace");
						def objWSR = new JsonSlurper().parseText(jsonWSR);
						if (objWSR.workspaces[0]["status-code"] == 25) {
							exist = false;
						}	
						
						if (!exist) {
							// Crear el WSR desde cero
							log "creating workspace \"${workspaceRTC}\""
							executeScmCommand(command, "create workspace -e \"${workspaceRTC}\"", dir);
							RTCUtils.exitOnError(command.getLastResult(), "Creating workspace");
						}
						
						if (component != null) {
							
							// Ver si tiene el componente
							boolean existComp = false;
							// ¿El componente deseado forma parte del WSR?		
							String jsonComponents = executeScmCommand(command, "list components \"${workspaceRTC}\" -j", dir);
							RTCUtils.exitOnError(command.getLastResult(), "Listing components");
							def objComponents = new JsonSlurper().parseText(jsonComponents);
							objComponents.workspaces[0].components.each { tmp ->
								if (tmp.name == component) {
									existComp = true;
								}
							}		
							
							if (!existComp) {
								// Añadir el componente
								executeScmCommand(command, "workspace add-components \"${workspaceRTC}\" -s \"$stream\" \"$component\" ", dir);
								RTCUtils.exitOnError(command.getLastResult(), "Adding component");
							}
							
							// Actualizarlo con la corriente
							executeScmCommand(command, "accept -C \"$component\" --flow-components -o -v --target \"$workspaceRTC\" -s \"${stream}\"", dir);
							RTCUtils.exitOnError(command.getLastResult(), "accepting changes");
						
						}
					}
					catch(Exception e) {
						e.printStackTrace();
						throw e;
					}
					finally {
						if (light) {
							command.detenerDemonio(dir);
						}
					}
				}
			}
			log "stepRTCCreateRepositoryWorkspace: ${millis} mseg."
		}		
	}
}
