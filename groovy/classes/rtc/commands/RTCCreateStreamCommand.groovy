package rtc.commands

import rtc.RTCUtils
import es.eci.utils.ParameterValidator
import es.eci.utils.ScmCommand
import es.eci.utils.Stopwatch
import es.eci.utils.TmpDir
import groovy.json.JsonSlurper

/**
 * Esta clase crea una nueva corriente. <br/>
 * Parámetros de entrada: <br/>
 * <br/>
 * --- OBLIGATORIOS <br/>
 * <b>scmToolsHome</b> Directorio raíz de las herramientas RTC <br/>
 * <b>userRTC</b> Usuario RTC <br/>
 * <b>pwdRTC</b> Password RTC <br/>
 * <b>urlRTC</b> URL de repositorio RTC<br/>
 * <b>stream</b> Corriente a crear <br/>
 * <b>projectArea</b> Area de proyecto donde se quiere crear la corriente <br/>
 * --- OPCIONALES<br/>
 * <b>light</b> Indica si se debe usar o no la versión ligera del comando scm. VALOR POR DEFECTO: true <br/>
 */
class RTCCreateStreamCommand extends AbstractRTCCommand {

	//---------------------------------------------------------------
	// Propiedades de la clase
	
	// Obligatorios	
	private String stream;
	private String projectArea;
	
	//---------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Asigna el nombre de la corriente.
	 * @param stream Corriente a crear.
	 */
	public void setStream(String stream) {
		this.stream = stream;
	}
	
	/**
	 * Asigna el nombre del Area de proyecto.
	 * @param stream Area de proyecto donde crear la corriente.
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
					.add("projectArea", projectArea)
					.add("light", light).build().validate();
			
			long millis = Stopwatch.watch {
				ScmCommand command = null;
				TmpDir.tmp { File dir ->
					try {
						command = new ScmCommand(light, scmToolsHome, daemonsConfigDir.getCanonicalPath());
						command.initLogger(this);
						
						// Revisamos antes si el stream existe (Si no se indica el nuḿero máximo, 99999,
						// sólo devuelve 10)
						boolean existStream = false;
						String jsonStreams = executeScmCommand(command, "list streams -j -m 99999", dir);
						def objStreams = new JsonSlurper().parseText(jsonStreams);
						objStreams.each { tmp ->
							if (tmp.name == stream) {
								existStream = true;
							}
						}
						
						if ( existStream ) {
							command.log("### La corriente indicada ya existe, abortando creacion ###");
						} else {
							// "status-code": 25, cuando el area de proyecto no existe
							executeScmCommand(command, "create stream \"${stream}\" --projectarea \"${projectArea}\"", dir);
							if (command.getLastResult() == 25) {
								throw new Exception("El area de proyecto indicada no existe");
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
			log "RTCCreateStreamCommand: ${millis} mseg."
		}		
	}
}
