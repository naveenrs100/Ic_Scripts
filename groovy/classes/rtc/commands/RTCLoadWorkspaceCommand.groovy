package rtc.commands

import rtc.RTCUtils
import es.eci.utils.ParameterValidator
import es.eci.utils.ScmCommand
import es.eci.utils.Stopwatch
import es.eci.utils.TmpDir
import groovy.json.JsonSlurper

/**
 * Esta clase carga un workspace en el sandbox indicado. Este job no se ejecuta en un <br/>
 * directorio temporal <br/>
 * Parámetros de entrada: <br/>
 * <br/>
 * --- OBLIGATORIOS <br/>
 * <b>scmToolsHome</b> Directorio raíz de las herramientas RTC <br/>
 * <b>userRTC</b> Usuario RTC <br/>
 * <b>pwdRTC</b> Password RTC <br/>
 * <b>urlRTC</b> URL de repositorio RTC<br/>
 * <b>sandbox</b> Directorio local a utilizar <br/>
 * <b>wsName</b> Nombre del workspace <br/>
 * <b>light</b> Indica si se debe usar o no la versión ligera del comando scm. VALOR POR DEFECTO: true <br/>
 */
class RTCLoadWorkspaceCommand extends AbstractRTCCommand {

	//---------------------------------------------------------------
	// Propiedades de la clase
	
	// Obligatorios	
	private String sandbox;
	private String wsName;
	
	//---------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Asigna el nombre del directorio a utilizar como sandbox.
	 * @param stream Sandbox a utilizar.
	 */
	public void setSandbox(String sandbox) {
		this.sandbox = sandbox;
	}
	
	/**
	 * Asigna el nombre del workspace.
	 * @param stream Workspace a cargar.
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
		// Validación de obligatorios
		ParameterValidator.builder()
				.add("scmToolsHome", scmToolsHome)
				.add("userRTC", userRTC)
				.add("pwdRTC", pwdRTC)
				.add("urlRTC", urlRTC)
				.add("sandbox", sandbox)
				.add("wsName", wsName)
				.add("light", light).build().validate();
			
		long millis = Stopwatch.watch {
			ScmCommand command = null;
			try {
				// El directorio se crea dentro del workspace del job
				command = new ScmCommand(light, scmToolsHome, sandbox);
				command.initLogger(this);
				
				log "Executing command on " + sandbox;
						
				executeScmCommand(command, "load \"${wsName}\" -i --force", new File(sandbox));
			}
			catch(Exception e) {
				e.printStackTrace();
				throw e;
			}
			finally {
				if (light) {
					command.detenerDemonio(new File(sandbox));
				}
				new File (sandbox).each { log it }
			}
		}
		log "stepRTCCreateWorkspace: ${millis} mseg."
	}		
}
