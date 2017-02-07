package rtc.commands

import es.eci.utils.ParameterValidator
import es.eci.utils.ScmCommand
import es.eci.utils.Stopwatch
import es.eci.utils.TmpDir

/**
 * Esta clase modela un comando de obtención de componentes tras comparar un
 * workspace de repositorio RTC (comando RTC check-in) con un elemento destino
 * <br/>
 * Parámetros de entrada:<br/>
 * <br/>
 * --- OBLIGATORIOS<br/>
 * <b>scmToolsHome</b> Directorio raíz de las herramientas RTC <br/>
 * <b>userRTC</b> Usuario RTC<br/>
 * <b>pwdRTC</b> Password RTC<br/>
 * <b>urlRTC</b> URL de repositorio RTC<br/>
 * <b>fileOut</b> Fichero en el que se almacenan los resultados<br/>
 * <b>parentWorkspace</b> Directorio de ejecución<br/>
 * <b>typeTarget</b> Tipo de elemento del destino de la comparación; normalmente una corriente<br/>
 * <b>nameTarget</b> Destino de la comparación (nombre del elemento definido en typeTarget)<br/>
 * --- OPCIONALES<br/>
 * <b>light</b> Indica si se debe usar o no la versión ligera del comando scm.  VALOR POR DEFECTO: true<br/>
 * <b>typeOrigin</b> Tipo de elemento del origen de la comparación; normalmente un workspace<br/>
 * <b>nameOrigin</b> Origen de la comparación (nombre del elemento definido en typeOrigin)<br/>
 * <b>onlyChanges</b> Indicador todo / sólo cambios (por defecto true)<br/>
 */
class RTCgetComponentsCommand extends AbstractRTCCommand {

	//---------------------------------------------------------------
	// Propiedades de la clase
	
	// Obligatorios
	private Boolean onlyChanges = true;
	private File fileOut;
	private String nameTarget;
	private String typeOrigin;
	private String nameOrigin;
	private String typeTarget;
	
	//---------------------------------------------------------------
	// Métodos de la clase
	
	// Método principal
	public void execute() { 
		TmpDir.tmp { File daemonsConfigDir ->
			// Validación de obligatorios
			ParameterValidator.builder()
					.add("scmToolsHome", scmToolsHome)
					.add("userRTC", userRTC)
					.add("pwdRTC", pwdRTC)
					.add("urlRTC", urlRTC)
					.add("typeTarget", typeTarget)
					.add("nameTarget", nameTarget)
					.add("onlyChanges", onlyChanges)
					.add("fileOut", fileOut)
					.add("light", light)
						.build().validate();
			
			if (onlyChanges == true){
				// Validar el resto de parámetros, requeridos cuando
				//	onlyChanges == true
				ParameterValidator.builder()
					.add("typeOrigin", typeOrigin)
					.add("nameOrigin", typeOrigin).build().validate();
			}
			
			long millis = Stopwatch.watch {
				ScmCommand command = null;
				try {
					command = new ScmCommand(light, scmToolsHome, daemonsConfigDir.getCanonicalPath());
					command.initLogger(this);
					Boolean error = false;
					String ret = "";
					if (onlyChanges == true){	
						log "Comparando cambios de ${typeOrigin} \"${nameOrigin}\" frente a ${typeTarget} \"${nameTarget}\""
						ret = command.ejecutarComando("compare ${typeOrigin} \"${nameOrigin}\" ${typeTarget} \"${nameTarget}\" -f i -I dcbsw -C \"|{name}|{email}|\" -D \"|yyyy-MM-dd-HH:mm:ss|\" ", userRTC, pwdRTC, urlRTC, parentWorkspace)
						if (command.getLastErrorOutput().size() > 0){
							error = true;
							log "No se puede realizar la comparación contra el WSR: \"${nameOrigin}\". Se procede a listar todos los componentes"
							ret = this.getComponents(command, nameTarget);
						}
					}else {
						ret = this.getComponents(command, nameTarget);
					}
	
					fileOut.write(this.parseComponents(ret, onlyChanges, error));
					
				}
				catch(Exception e) {
					e.printStackTrace();
					throw e;
				}
				finally {
					if (light) {
						command.detenerDemonio(parentWorkspace);
					}
				}
			}
			log "getComponents: ${millis} mseg."
		}
	}
	
	private String parseComponents (String comps, Boolean onlyChanges, Boolean error){
		String[] componentes = comps.tokenize("\r\r\n");
		String ret = "";
		for (int i=0; i< componentes.size(); i++){
			String comp = componentes[i];
			// Excluir cambios realizados por JENKINS_RTC
			if (!comp.contains("|${userRTC}|")) {
				// Asegurarse de que si el componente no está en el WSR, se dispare la construcción
				//	forzando un cambio
				if (comp.endsWith("added)")){
					ret = ret + "\n" + comp + "\n" + "(1007) |Forzar cambio|forzarCambio@gexterno.es| 00000: Detectado cambio se fuerza construcción |2015-07-31-09:43:38|"
				} else if (onlyChanges && error){
					if (comp.startsWith("  Component")){
						ret = ret + "\n" + comp + "\n" + "(1007) |Forzar cambio|forzarCambio@gexterno.es| 00000: Detectado cambio se fuerza construcción |2015-07-31-09:43:38|"
					}
				} else {
					ret = ret + "\n" + comp;
				}
			}
		}
		return ret;
	}
	
	private String getComponents (ScmCommand command, String nameTarget){
		log "Obteniendo componentes de " + nameTarget
		return command.ejecutarComando("list components \"${nameTarget}\" ", 
			userRTC, pwdRTC, urlRTC, parentWorkspace);
	}
	

	/**
	 * Informa si se invoca en modo de 'solo cambios'.
	 * @param onlyChanges Si se informa como true, devolverá solo los componentes
	 * con cambios.
	 */
	public void setOnlyChanges(Boolean onlyChanges) {
		this.onlyChanges = onlyChanges;
	}

	/**
	 * Informa el fichero donde se almacenarán los resultados.
	 * @param fileOut Fichero de salida del comando.
	 */
	public void setFileOut(File fileOut) {
		this.fileOut = fileOut;
	}

	/**
	 * Informa el nombre del destino de la comparación. 
	 * @param nameTarget Nombre del destino de la comparación.
	 */
	public void setNameTarget(String nameTarget) {
		this.nameTarget = nameTarget;
	}

	/**
	 * Informa el tipo del destino de la comparación (stream, workspace, etc.)
	 * @param typeTarget workspace/stream/baseline/snapshot
	 */
	public void setTypeTarget(String typeTarget) {
		this.typeTarget = typeTarget;
	}

	/**
	 * Informa el tipo del origen de la comparación (stream, workspace, etc.)
	 * @param typeTarget workspace/stream/baseline/snapshot
	 */
	public void setTypeOrigin(String typeOrigin) {
		this.typeOrigin = typeOrigin;
	}

	/**
	 * Informa el nombre del origen de la comparación. 
	 * @param nameTarget Nombre del origen de la comparación.
	 */
	public void setNameOrigin(String nameOrigin) {
		this.nameOrigin = nameOrigin;
	}
	
}
