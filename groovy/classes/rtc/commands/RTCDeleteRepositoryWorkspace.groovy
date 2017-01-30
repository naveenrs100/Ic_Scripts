package rtc.commands

import es.eci.utils.ParameterValidator
import es.eci.utils.ScmCommand
import es.eci.utils.Stopwatch
import es.eci.utils.TmpDir

/**
 * Esta clase ofrece la funcionalidad para eliminar un workspace de repositorio.
 * <br/>
 * Parámetros de entrada:<br/>
 * <br/>
 * --- OBLIGATORIOS<br/>
 * <b>scmToolsHome</b> Directorio raíz de las herramientas RTC <br/>
 * <b>userRTC</b> Usuario RTC<br/>
 * <b>pwdRTC</b> Password RTC<br/>
 * <b>urlRTC</b> URL de repositorio RTC<br/>
 * <b>workspaceRTC</b> Nombre del workspace de repositorio que se quiere crear<br/>
 * --- OPCIONALES<br/>
 * <b>light</b> Indica si se debe usar o no la versión ligera del comando scm.  VALOR POR DEFECTO: true<br/>
 */
public class RTCDeleteRepositoryWorkspace extends AbstractRTCCommand {

	//---------------------------------------------------------------
	// Propiedades de la clase
	
	// Obligatorios	
	private String workspaceRTC;
	
	//---------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Asigna el nombre del wsr.
	 * @param workspaceRTC Nombre del workspace de repositorio que se quiere crear.
	 */
	public void setWorkspaceRTC(String workspaceRTC) {
		this.workspaceRTC = workspaceRTC;
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
	
	/* (non-Javadoc)
	 * @see rtc.commands.AbstractRTCCommand#execute()
	 */
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
					.add("light", light).build().validate();
			
			long millis = Stopwatch.watch {
				ScmCommand command = null;
				TmpDir.tmp { dir ->
					try {
						command = new ScmCommand(light, scmToolsHome, daemonsConfigDir.getCanonicalPath());
						command.initLogger(this);	
						
						executeScmCommand(command, "delete workspace \"${workspaceRTC}\" ", dir);
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
			log "stepRTCDeleteRepositoryWorkspace: ${millis} mseg."
		}		
	}
}
