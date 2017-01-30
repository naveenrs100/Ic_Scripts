package rtc.commands

import rtc.RTCUtils
import es.eci.utils.ParameterValidator
import es.eci.utils.ScmCommand
import es.eci.utils.Stopwatch
import es.eci.utils.TmpDir
import groovy.json.JsonSlurper

/**
 * Esta clase crea un workspace temporal con todos los componentes de un stream <br/>
 * Parámetros de entrada: <br/>
 * <br/>
 * --- OBLIGATORIOS <br/>
 * <b>scmToolsHome</b> Directorio raíz de las herramientas RTC <br/>
 * <b>userRTC</b> Usuario RTC <br/>
 * <b>pwdRTC</b> Password RTC <br/>
 * <b>urlRTC</b> URL de repositorio RTC<br/>
 * <b>stream</b> Corriente a utilizar <br/>
 * <b>wsName</b> Nombre del workspace <br/>
 * <b>light</b> Indica si se debe usar o no la versión ligera del comando scm. VALOR POR DEFECTO: true <br/>
 */
class RTCCreateWorkspaceCommand extends AbstractRTCCommand {

	//---------------------------------------------------------------
	// Propiedades de la clase
	
	// Obligatorios	
	private String stream;
	private String wsName;
	
	//---------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Asigna el nombre de la corriente.
	 * @param stream Corriente a utilizar.
	 */
	public void setStream(String stream) {
		this.stream = stream;
	}
	
	/**
	 * Asigna el nombre del workspace.
	 * @param stream Workspace a crear.
	 */
	public void setWsName(String wsName) {
		this.wsName = wsName;
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
					.add("stream", stream)
					.add("wsName", wsName)
					.add("light", light).build().validate();
			
			long millis = Stopwatch.watch {
				ScmCommand command = null;
				TmpDir.tmp { File dir ->
					try {
						command = new ScmCommand(light, scmToolsHome, daemonsConfigDir.getCanonicalPath());
						command.initLogger(this);
						
						executeScmCommand(command, "create workspace --stream \"${stream}\" \"${wsName}\"", dir);
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
			log "stepRTCCreateWorkspace: ${millis} mseg."
		}		
	}
}
