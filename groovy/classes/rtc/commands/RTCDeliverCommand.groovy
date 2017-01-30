package rtc.commands

import rtc.RTCUtils
import es.eci.utils.ComponentVersionHelper
import es.eci.utils.ParameterValidator
import es.eci.utils.ScmCommand
import es.eci.utils.Stopwatch
import es.eci.utils.TmpDir
import es.eci.utils.base.Loggable

/**
 * Esta clase modela la entrega de código a una corriente de RTC (comando RTC
 * deliver)
 * <br/>
 * Parámetros de entrada:<br/>
 * <br/>
 * --- OBLIGATORIOS<br/>
 * <b>scmToolsHome</b> Directorio raíz de las herramientas RTC<br/> 
 * <b>userRTC</b> Usuario RTC<br/>
 * <b>pwdRTC</b> Password RTC<br/>
 * <b>urlRTC</b> URL de repositorio RTC<br/>
 * <b>workspaceRTC</b> Nombre del workspace de repositorio al que reincorporar los cambios<br/>
 * <b>component</b> Nombre de componente RTC<br/>
 * <b>streamTarget</b> Corriente de entrega de los cambios<br/>
 * <b>parentWorkspace</b> Directorio de ejecución<br/>
 * <br/>
 * --- OPCIONALES<br/>
 * <b>light</b> Indica si se debe usar o no la versión ligera del comando scm.  VALOR POR DEFECTO: true<br/>
 * <b>force</b> Si es true, se fuerza el reemplazo completo del componente
 * 	en la corriente de destino.<br/>
 */
class RTCDeliverCommand extends AbstractRTCCommand {

	//-----------------------------------------------------------------------
	// Propiedades de la clase	
	
	//---------------> Parámetros obligatorios
	private String workspaceRTC;
	private String component;
	private String streamTarget;
	//---------------> Parámetros opcionales
	private Boolean force;
	
	//-----------------------------------------------------------------------
	// Métodos de la clase
	
	// Comando de SCM.
	private String executeScmCommand(ScmCommand theCommand, String str) {
		return theCommand.ejecutarComando(str, userRTC, pwdRTC, urlRTC, parentWorkspace)
	}
	
	// Método principal
	public void execute() {		
		TmpDir.tmp { File daemonsConfigDir ->
			// Validación de obligatorios
			ParameterValidator.builder()
					.add("scmToolsHome", scmToolsHome)
					.add("userRTC", userRTC)
					.add("pwdRTC", pwdRTC)
					.add("urlRTC", urlRTC)
					.add("workspaceRTC", workspaceRTC)
					.add("component", component)
					.add("streamTarget", streamTarget)
					.add("parentWorkspace", parentWorkspace)
					.add("light", light)
						.build().validate();
			
			long millis = Stopwatch.watch {
				ScmCommand command = null 
				try {
					command = new ScmCommand(light, scmToolsHome, daemonsConfigDir.getCanonicalPath());
					command.initLogger(this);
					
					ComponentVersionHelper helper = new ComponentVersionHelper(scmToolsHome)
					helper.initLogger(this);
					boolean exists = helper.getComponents(parentWorkspace, streamTarget, userRTC, pwdRTC, urlRTC).contains(component);
					boolean isNew = false;
					if (!exists) {
						log "Adding component: \"${component}\""
						executeScmCommand(command, "workspace add-components \"${streamTarget}\" \"${component}\" -s \"${workspaceRTC}\"");
						RTCUtils.exitOnError(command.getLastResult(), "Adding componet to stream target");
						isNew = true;	
					}
					
					// Forzar reemplazo si ya existía
					if (force && !isNew) {		
						executeScmCommand(command,"workspace replace-components -o \"${streamTarget}\" workspace \"${workspaceRTC}\" \"${component}\"");
						RTCUtils.exitOnError(command.getLastResult(), "Replacing componet to stream target");
					}
					else if (!isNew) {
						executeScmCommand(command,"deliver -C \"${component}\" -s \"${workspaceRTC}\" -t \"${streamTarget}\" --overwrite-uncommitted");
						RTCUtils.exitOnError(command.getLastResult(), "Delivering to stream target");
					}	
				}
				catch(Exception e) {
					e.printStackTrace();
					throw e;
				}
				finally {
					if (light) {
						command?.detenerDemonio(parentWorkspace);
					}
				}
			}
			log "stepRTCDeliver: ${millis} mseg."
		}
	}

	/**
	 * @param workspaceRTC Workspace de repositorio de RTC al que reincorporar
	 * los cambios.
	 */
	public void setWorkspaceRTC(String workspaceRTC) {
		this.workspaceRTC = workspaceRTC;
	}

	/**
	 * @param component Componente de la corriente RTC a la 
	 * que se entregan los cambios.
	 */
	public void setComponent(String component) {
		this.component = component;
	}

	/**
	 * @param streamTarget Corriente de RTC a la que se entregan los cambios.
	 */
	public void setStreamTarget(String streamTarget) {
		this.streamTarget = streamTarget;
	}

	/**
	 * @param force Si es true, se fuerza el reemplazo completo del componente
	 * en la corriente de destino.
	 */
	public void setForce(Boolean force) {
		this.force = force;
	}
	
}
