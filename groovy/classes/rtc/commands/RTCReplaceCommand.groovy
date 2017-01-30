package rtc.commands

import rtc.RTCUtils
import es.eci.utils.ParameterValidator
import es.eci.utils.ScmCommand
import es.eci.utils.Stopwatch
import es.eci.utils.TmpDir
import groovy.json.JsonSlurper

/**
 * Esta clase modela un comando por el cual se reemplaza la versión existente
 * de un componente en una corriente por una línea base determinada.<br/>
 * Parámetros de entrada:<br/>
 * <br/>
 * --- OBLIGATORIOS<br/>
 * <b>scmToolsHome</b> Directorio raíz de las herramientas RTC <br/>
 * <b>userRTC</b> Usuario RTC<br/>
 * <b>pwdRTC</b> Password RTC<br/>
 * <b>urlRTC</b> URL de repositorio RTC<br/>
 * <b>stream</b> Corriente RTC de destino<br/>
 * <b>component</b> Nombre del componente<br/>
 * <b>baseline</b> Línea base del componente <br/>
 * --- OPCIONALES<br/>
 * <b>light</b> Indica si se debe usar o no la versión ligera del comando scm.  VALOR POR DEFECTO: true<br/>
 */
class RTCReplaceCommand extends AbstractRTCCommand {

	//--------------------------------------------------------------
	// Propiedades de la clase
	
	//-----------------------> Obligatorios
	private String stream; 
	private String component;
	private String baseline;
	
	//--------------------------------------------------------------
	// Métodos de la clase
	
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
					.add("stream", stream)
					.add("component", component)
					.add("baseline", baseline)
					.add("light", light)
						.build().validate();
			
			// Todo sobre directorio temporal
			long millis = Stopwatch.watch {
				TmpDir.tmp { File dir ->
					ScmCommand command = null;
					try {
						command = new ScmCommand(light, scmToolsHome, daemonsConfigDir.getCanonicalPath());
						command.initLogger(this);
						
							// Verificar que la corriente existe
							command.ejecutarComando(
								"get attributes -w \"$stream\" --name -j", 
								userRTC, pwdRTC, urlRTC, dir);
							RTCUtils.exitOnError(command.getLastResult(), "Consultando la corriente");
							if (command.getLastResult() == 33) {
								// La corriente no existe
								throw new Exception("La corriente $stream no existe");
							}
							// Verificar que la línea base del componente existe
							command.ejecutarComando(
								"get attributes -b \"$baseline\" --baseline-component \"$component\" --name -j", 
								userRTC, pwdRTC, urlRTC, dir);
							RTCUtils.exitOnError(command.getLastResult(), "Consultando el componente");
							if (command.getLastResult() == 25) {
								// O bien el componente o bien la línea base no existen
								throw new Exception("No se puede encontrar la baseline $baseline del componente $component");
							}
							// Verificar que la corriente tiene el componente
							String jsonComponents = 
								command.ejecutarComando(
									"list components \"$stream\" -j", 
									userRTC, pwdRTC, urlRTC, dir);
							RTCUtils.exitOnError(command.getLastResult(), "Consultando los componentes de la corriente");
							if (existsComponent(jsonComponents)) {	
								// Si lo tiene -> eliminarlo
								command.ejecutarComando(
									"remove component --ignore-uncommitted \"$stream\" \"$component\"", 
									userRTC, pwdRTC, urlRTC, dir);
								RTCUtils.exitOnError(command.getLastResult(), "Eliminando el componente");
							}
							
							// Añadir la línea base específica del componente a la corriente
							// add component -b $baseline $stream $component
							command.ejecutarComando(
									"add component -b \"$baseline\" \"$stream\" \"$component\"", 
									userRTC, pwdRTC, urlRTC, dir);
							RTCUtils.exitOnError(command.getLastResult(), "Añadiendo la línea base de componente");
						
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
			log "stepRTCReplace: ${millis} mseg."
		}
	}

	// Determina si el componente está incluido en la lista
	private boolean existsComponent(String jsonComponents) {
		def componentsObject = new JsonSlurper().parseText(jsonComponents);
		boolean ret = false;
		int numberOfComponents = componentsObject.workspaces[0].components.size(); 
		for (int i = 0; i < numberOfComponents; i++) {
			String tmpName = componentsObject.workspaces[0].components[i].name;
			if (component.equals(tmpName)) {
				ret = true;
			}
		}
		return ret;
	}
	
	/**
	 * @param stream the stream to set
	 */
	public void setStream(String stream) {
		this.stream = stream;
	}

	/**
	 * @param component the component to set
	 */
	public void setComponent(String component) {
		this.component = component;
	}

	/**
	 * @param baseline the baseline to set
	 */
	public void setBaseline(String baseline) {
		this.baseline = baseline;
	}

	
	
}

