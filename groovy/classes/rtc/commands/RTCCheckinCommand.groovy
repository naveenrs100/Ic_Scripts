package rtc.commands

import rtc.RTCUtils
import es.eci.utils.ParameterValidator
import es.eci.utils.Retries
import es.eci.utils.ScmCommand
import es.eci.utils.Stopwatch
import es.eci.utils.TmpDir
import groovy.json.JsonSlurper

/**
 * Esta clase modela un comando de reincorporación de código desde local a un
 * workspace de repositorio RTC (comando RTC check-in)
 * <br/>
 * Parámetros de entrada:<br/>
 * <br/>
 * --- OBLIGATORIOS<br/>
 * <b>scmToolsHome</b> Directorio raíz de las herramientas RTC <br/>
 * <b>userRTC</b> Usuario RTC<br/>
 * <b>pwdRTC</b> Password RTC<br/>
 * <b>urlRTC</b> URL de repositorio RTC<br/>
 * <b>description</b> Descripción del check-in<br/>
 * <b>workItem</b> Tarea/defecto/etc. de RTC a la que asociar el conjunto de cambios<br/>
 * <b>parentWorkspace</b> Directorio de ejecución<br/>
 * <b>workspaceRTC</b> Nombre del workspace de repositorio al que reincorporar los cambios<br/>
 * <b>ignoreErrorsWithoutChanges</b> Indica si se debe omitir los errores derivados de 
 * 		intentar hacer check-in de algo que no tiene cambios (true/false)<br/>
 * --- OPCIONALES<br/>
 * <b>light</b> Indica si se debe usar o no la versión ligera del comando scm.  VALOR POR DEFECTO: true<br/>
 */
class RTCCheckinCommand extends AbstractRTCCommand {

	//---------------------------------------------------------------
	// Propiedades de la clase
	
	// Obligatorios
	private String workItem;
	private Boolean ignoreErrorsWithoutChanges = true;
	private String workspaceRTC;
	private String description;
	private String rtcFilter;
	
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
					.add("description", description)
					.add("workItem", workItem)
					.add("parentWorkspace", parentWorkspace)
					.add("ignoreErrorsWithoutChanges", ignoreErrorsWithoutChanges)
					.add("workspaceRTC", workspaceRTC)
					.add("light", light).build().validate();
			
			long millis = Stopwatch.watch {
				ScmCommand command = null;
				try {
					command = new ScmCommand(light, scmToolsHome, daemonsConfigDir.getCanonicalPath());
					command.initLogger(this);
					// Check-in de los cambios en la fila del fichero changed.txt si existe
					File changed = new File(parentWorkspace, "changed.txt");
					if (changed.exists()) {
						changed.eachLine { line ->
							command.ejecutarComando("checkin \"${line}\" -n", userRTC, pwdRTC, null, parentWorkspace);
							// El 30 se da en aquellos casos particulares en los que se intenta hacer checkin
							//	de un fichero que no está en el componente (p. ej., una instalación local de ATG)
							RTCUtils.exitOnError(command.getLastResult(), "Checkin code", [30])
						}
					}
					// INI - GDR - 29/11/2016 - Modificación para subir a RTC únicamente los ficheros relevantes en base
					// al parámetro rtcFilter
					else if ( this.rtcFilter != null ) {
						if ( !this.rtcFilter.isEmpty() ) {
							log "### Filtro recibido! Unicamente suben a RTC los ficheros con la mascara: " + this.rtcFilter
							File folder = new File("${this.parentWorkspace}")
							folder.traverse (
								preDir : { if (it.name == 'node_modules' || it.name == 'target') 
										return groovy.io.FileVisitResult.SKIP_SUBTREE },
								type: groovy.io.FileType.FILES,
								nameFilter: ~/${this.rtcFilter}/
								) {
								command.ejecutarComando("checkin -n \"" + it.canonicalPath + "\"", userRTC, pwdRTC, null, parentWorkspace)
								if ( (command.getLastResult() == 30) && (command.getLastErrorOutput().contains("is not shared.") ) ) {
									RTCUtils.exitOnError(command.getLastResult(), "Checkin code")
								} else
									RTCUtils.exitOnError(command.getLastResult(), "Checkin code", [30])
							}
						}
					// Si el filtro viene vacío, se añade todo.
					} else {
						log "### Aviso, no hay filtro, todos los ficheros suben a RTC"
						command.ejecutarComando("checkin . -n", userRTC, pwdRTC, null, parentWorkspace);
						RTCUtils.exitOnError(command.getLastResult(), "Checkin code")
					}
					// FIN - GDR - 29/11/2016					
					// UUID del cambio
					// Se añade retries debido a un problema observado en contenedores Docker
					// En algunos casos se corrompe un fichero .iteminfo.dat dentro del directorio .jazz5
					// Se repite el comando status en tanto que esto parece reparar el error
					String jsonChange = 
						Retries.retry(5,500, 
							{ 
								String json = command.ejecutarComando("status -j", userRTC, pwdRTC, null, parentWorkspace)
								// Repetir la ejecución si devuelve un estado distinto de cero
								int result = command.getLastResult();
								if (result != 0) {
									throw new Exception();
								}
								return json;			
							});
					RTCUtils.exitOnError(command.getLastResult(), "Status")
					def objChange = new JsonSlurper().parseText(jsonChange);
					String changeSet = null;
					if (objChange.workspaces[0].components[0]["outgoing-changes"] != null && objChange.workspaces[0].components[0]["outgoing-changes"].size() > 0) {	
						changeSet = objChange.workspaces[0].components[0]["outgoing-changes"][0].uuid;	
					}

					// Entregar todos los cambios pendientes
					if (objChange.workspaces[0].components[0]["outgoing-changes"] != null && objChange.workspaces[0].components[0]["outgoing-changes"].size() > 0) {	
							objChange.workspaces[0].components[0]["outgoing-changes"].each { change ->
								changeSet = change.uuid;
								log "El changeset es: ${changeSet}"
								command.ejecutarComando("changeset comment ${changeSet} \"${description}\"", userRTC, pwdRTC, urlRTC, parentWorkspace);
								RTCUtils.exitOnError(command.getLastResult(), "Adding comment")
								command.ejecutarComando("changeset associate ${changeSet} ${workItem}", userRTC, pwdRTC, urlRTC, parentWorkspace);
								RTCUtils.exitOnError(command.getLastResult(), "Associating workItem")
								command.ejecutarComando("set changeset --complete -w \"${workspaceRTC}\" ${changeSet}" , userRTC, pwdRTC, urlRTC, parentWorkspace); 
								RTCUtils.exitOnError(command.getLastResult(), "Closing changeSet")
							}
					}
					else if (ignoreErrorsWithoutChanges) {
						log "NO SE HA MODIFICADO NADA, NO SE HA ENCONTRADO CHANGE SET. Aunque no haya cambios no se considera error."
					}
					else {
						throw new Exception("NO SE HA MODIFICADO NADA, NO SE HA ENCONTRADO CHANGE SET")
					}
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
			log "stepRTCCheckin: ${millis} mseg."
		}
	}

	/**
	 * @param description Descripción del conjunto de cambios para RTC.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param workItem Tarea/defecto/etc. de RTC a la que asociar el cambio.
	 */
	public void setWorkItem(String workItem) {
		this.workItem = workItem;
	}

	/**
	 * @param ignoreErrorsWithoutChanges Si es true, hace caso omiso de errores
	 * por intentar hacer check-in de un sandbox sin cambios.
	 */
	public void setIgnoreErrorsWithoutChanges(Boolean ignoreErrorsWithoutChanges) {
		this.ignoreErrorsWithoutChanges = ignoreErrorsWithoutChanges;
	}

	/**
	 * @param workspaceRTC Workspace de repositorio de RTC al que reincorporar
	 * los cambios.
	 */
	public void setWorkspaceRTC(String workspaceRTC) {
		this.workspaceRTC = workspaceRTC;
	}
	
	/**
	 * @param rtcFilter Filtro de los ficheros que harán check-in en RTC.
	 */
	public void setRtcFilter(String rtcFilter) {
		this.rtcFilter = rtcFilter;
	}	
	
}
