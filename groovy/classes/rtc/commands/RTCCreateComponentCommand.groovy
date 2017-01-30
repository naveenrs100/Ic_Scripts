package rtc.commands

import rtc.RTCUtils
import es.eci.utils.ParameterValidator
import es.eci.utils.ScmCommand
import es.eci.utils.Stopwatch
import es.eci.utils.TmpDir
import groovy.json.JsonSlurper

/**
 * Esta clase crea un nuevo componente en una corriente. <br/>
 * Parámetros de entrada: <br/>
 * <br/>
 * --- OBLIGATORIOS <br/>
 * <b>scmToolsHome</b> Directorio raíz de las herramientas RTC <br/>
 * <b>userRTC</b> Usuario RTC <br/>
 * <b>pwdRTC</b> Password RTC <br/>
 * <b>urlRTC</b> URL de repositorio RTC<br/>
 * <b>stream</b> Corriente destino <br/>
 * <b>component</b> Componente a crear <br/>
 * <b>projectArea</b> Area de proyecto para acotar el filtro de componentes existentes <br/>
 * --- OPCIONALES<br/>
 * <b>light</b> Indica si se debe usar o no la versión ligera del comando scm. VALOR POR DEFECTO: true <br/>
 */
class RTCCreateComponentCommand extends AbstractRTCCommand {

	//---------------------------------------------------------------
	// Propiedades de la clase
	
	// Obligatorios	
	private String stream;
	private String component;
	private String projectArea;
	
	//---------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Asigna el nombre de la corriente.
	 * @param stream Corriente destino para el componente a crear.
	 */
	public void setStream(String stream) {
		this.stream = stream;
	}
	
	/**
	 * Asigna el nombre del componente.
	 * @param stream Nombre del componente a crear.
	 */
	public void setComponent(String component) {
		this.component = component;
	}
	
	/**
	 * Asigna el nombre del Area de proyecto.
	 * @param stream Area de proyecto para filtrar.
	 */
	public void setProjectArea(String projectArea) {
		this.projectArea = projectArea;
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
					.add("component", component)
					.add("projectArea", projectArea)
					.add("light", light).build().validate();
			
			long millis = Stopwatch.watch {
				ScmCommand command = null;
				TmpDir.tmp { File dir ->
					try {
						command = new ScmCommand(light, scmToolsHome, daemonsConfigDir.getCanonicalPath());
						command.initLogger(this);
						
						// Revisamos antes si el componente existe (Si no se indica el nuḿero máximo, 99999,
						// sólo devuelve 10)
						boolean existComponent = false;
						String jsonComponents = executeScmCommand(command, "list components --projectarea \"${projectArea}\" -j -m 99999", dir);
						def objComponents = new JsonSlurper().parseText(jsonComponents);
						objComponents.components.each { tmp ->
							if (tmp.name == component) {
								existComponent = true;
							}
						}
						
						if ( existComponent ) {
							command.log("### El componente indicado ya existe, abortando creacion ###");
						} else {
							executeScmCommand(command, "create component \"${component}\" \"${stream}\" -j", dir);
							// "status-code": 9, cuando la corriente está duplicada en RTC
							if (command.getLastResult() == 9) {
								throw new Exception("Nombre de corriente ambiguo (posiblemente duplicada)");
							}
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
			log "stepRTCCreateComponent: ${millis} mseg."
		}		
	}
}

