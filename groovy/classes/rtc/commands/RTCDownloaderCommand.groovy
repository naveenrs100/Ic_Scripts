package rtc.commands

import java.nio.channels.FileLock

import rtc.RTCUtils
import es.eci.utils.ParameterValidator
import es.eci.utils.ScmCommand
import es.eci.utils.Stopwatch
import es.eci.utils.TmpDir
import es.eci.utils.base.Loggable
import groovy.json.JsonSlurper

/**
 * Esta clase modela un comando de carga de código desde RTC hacia local (comando
 * RTC load + accept)
 * 
 * Parámetros de entrada:<br/>
 * <br/>
 * --- OBLIGATORIOS<br/>
 * <b>scmToolsHome</b> Directorio raíz de las herramientas RTC<br/>
 * <b>userRTC</b> Usuario RTC<br/>
 * <b>pwdRTC</b> Password RTC<br/>
 * <b>urlRTC</b> URL de repositorio RTC<br/>
 * <b>workspaceRTC</b> Nombre del workspace de repositorio al que reincorporar los cambios<br/>
 * <b>component</b> Nombre de componente RTC<br/>
 * <b>parentWorkspace</b> Directorio de ejecución<br/>
 * <br/>
 * --- OPCIONALES<br/>
 * <b>light</b> Indica si se debe usar o no la versión ligera del comando scm.  VALOR POR DEFECTO: true<br/>
 * <b>stream</b> Corriente de origen<br/>
 * <b>baseline</b> Línea base del componente.  Si se indica, toma el contenido de dicha línea base.<br/>
 * <b>snapshot</b> Instantánea de la que extraer la línea base del componente<br/>
 * <b>recreateWS</b> Indica si se debe recrear por fuerza el WSR, aunque exista<br/>
 * <b>nosinc</b> Poner true si, a pesar de indicar baseline, no se desea sincronizar la versión con el local<br/>
 * <b>autoresolve</b> Si es cierto, resuelve cualquier posible conflicto <br/>
	 * a favor de la corriente (--proposed, es decir, cambios entrantes)<br/>
 */
class RTCDownloaderCommand extends AbstractRTCCommand {

	//---------------------------------------------------------------
	// Propiedades de la clase
	
	//---------------> Parámetros obligatorios
	private String workspaceRTC;
	private String component;
	//---------------> Parámetros opcionales
	private String stream;
	private String baseline;
	private String snapshot;
	private Boolean recreateWS;
	private Boolean nosinc;
	private Boolean autoResolve;
	
	//---------------------------------------------------------------
	// Métodos de la clase
	
	// Script principal
	public void execute() {
		
		// Validación de obligatorios
		ParameterValidator.builder()
				.add("scmToolsHome", scmToolsHome)
				.add("userRTC", userRTC)
				.add("pwdRTC", pwdRTC)
				.add("urlRTC", urlRTC)
				.add("workspaceRTC", workspaceRTC)
				.add("component", component)
				.add("parentWorkspace", parentWorkspace)
				.add("light", light)
					.build().validate();
					
		TmpDir.tmp { File daemonsConfigDir ->
			// Ejecutar el comando
			long millis = Stopwatch.watch {
				ScmCommand command = null
				String aliasfin = component;
				try {
					command = new ScmCommand(light, scmToolsHome, 
						daemonsConfigDir.getCanonicalPath());
					command.initLogger(this);
					
					if (RTCUtils.isSet(snapshot)) {
						// Buscar la línea base correspondiente
						baseline = setBaselineRTC(command);
					}
					
					if (!RTCUtils.isSet(baseline)) {
						// Sincronizar sin línea base
						sincroniza(command, daemonsConfigDir);
					}
					else {
						// Baja la línea base
						// ¿Existe la línea base?
						String jsonBaseline = executeScmCommand(command, "get attributes -b \"${baseline}\" --baseline-component \"${component}\" -j --name");
						RTCUtils.exitOnError(command.getLastResult(), "Querying baseline");
						if (command.getLastErrorOutput()?.contains("Unable to find baseline")) {
						//if (command.getLastResult() == 1) {
							log "Baseline $baseline does not exist"
							// Sincronizar sin línea base y luego ya la creamos
							sincroniza(command, daemonsConfigDir);
							boolean changes = new File(parentWorkspace.getCanonicalPath() + System.getProperty("file.separator") + "changesetAccept.txt").text.contains("Workspace unchanged")
							if (!changes || recreateWS) {
								log "creating baseline.................."
								executeScmCommand(command, "create baseline \"$workspaceRTC\" \"${baseline}\" \"${component}\"");
								RTCUtils.exitOnError(command.getLastResult(), "Creating baseline");
								executeScmCommand(command, "deliver --source \"$workspaceRTC\" -t \"${stream}\"");
								RTCUtils.exitOnError(command.getLastResult(), "Delivering changes");
							}
							else {
								throw new Exception("No changes in sandbox, CANNOT CREATE A NEW VERSION")
							}
						}
						else {
							// Si existe sincronizo la versión con los archivos locales
							log "${baseline} EXISTS!!!"
							if (!nosinc) {
								log "updating local folder...."
								sincroniza(command, daemonsConfigDir)
							}
						}
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
			log "stepRTCDownloader: ${millis} mseg."
		}
	}	
	
	/**
	 * Gestiones relativas al workspace de repositorio: lo crea si no existe, lo
	 * recrea en cualquier caso si está marcado recreateWS, etc.
	 * @param command Comando RTC
	 */
	private void setWorkspace(ScmCommand command, File daemonsConfigDir) {	
		// Sección crítica
		log "Trying to lock critical section..."
		FileLock lock = null;
		boolean exist = true;
		boolean existComp = true;
		try {
			lock = RTCUtils.getLock(workspaceRTC);
			log "Critical section: locked ${lock}"
			// ¿Existe el WSR?
			// "status-code": 25, cuando no lo encuentra
			String jsonWSR = executeScmCommand(command, "get attributes -w \"${workspaceRTC}\" --description -j");
			RTCUtils.exitOnError(command.getLastResult(), "Querying repository workspace");
			def objWSR = new JsonSlurper().parseText(jsonWSR);
			if (objWSR.workspaces[0]["status-code"] == 25) {
				exist = false;
			}
			if (exist && recreateWS) {
				// Se desea recrearlo: hay que borrarlo antes
				executeScmCommand(command, "workspace delete \"${workspaceRTC}\"");
				RTCUtils.exitOnError(command.getLastResult(), "Deleting workspace");
				log "workspace \"${workspaceRTC}\" deleted!"
				exist = false;
			}
			log "exists workspace \"${workspaceRTC}\"?: $exist"
			if (!exist) {
				// Crear el WSR desde cero
				log "creating workspace \"${workspaceRTC}\""
				executeScmCommand(command, "create workspace -e \"${workspaceRTC}\"");
				RTCUtils.exitOnError(command.getLastResult(), "Creating workspace");
				existComp = false;
			}
			else {
				existComp = false;
				// ¿El componente deseado forma parte del WSR?		
				String jsonComponents = executeScmCommand(command, "list components \"${workspaceRTC}\" -j");
				RTCUtils.exitOnError(command.getLastResult(), "Listing components");
				def objComponents = new JsonSlurper().parseText(jsonComponents);
				objComponents.workspaces[0].components.each { tmp ->
					if (tmp.name == component) {
						existComp = true;
					}
				}
			}
		
			if (!existComp) {
				log "Adding component: \"$component\""
				if (RTCUtils.isSet(baseline)) {
					executeScmCommand(command, "workspace add-components \"${workspaceRTC}\" \"${component}\" -b \"${baseline}\"");
					RTCUtils.exitOnError(command.getLastResult(), "Adding component baseline ${baseline}");
				}
				else if (RTCUtils.isSet(stream)){
					executeScmCommand(command, "workspace add-components \"${workspaceRTC}\" -s \"$stream\" \"$component\" ");
					RTCUtils.exitOnError(command.getLastResult(), "Adding component");
				}
				else {
					log "Component \"${component}\" does not exist in the repository workspace!"
				}
			}
			else if (RTCUtils.isSet(baseline)) {
				log "Replacing component baseline (${baseline}): \"$component\""
				String dumpReplace = executeScmCommand(command, "workspace replace-components -b \"${baseline}\" -o \"${workspaceRTC}\" workspace \"${workspaceRTC}\" \"${component}\"");
				RTCUtils.dumpExit(parentWorkspace, "replace.txt", dumpReplace)
				RTCUtils.exitOnError(command.getLastResult(), "Replacing component");
			}
		}
		catch(Exception e) {
			log e.getMessage();
			e.printStackTrace();
		}
		finally {
			log "Critical section: releasing $lock ..."
			lock.release()
		}
		// Fin de la sección crítica
	}
	
	/**
	 * Si se ha informado una snapshot, tiene como objetivo encontrar la línea base correspondiente del componente
	 * @param command Comando RTC
	 */
	private String setBaselineRTC(ScmCommand command) {
		String ret = null;
		String jsonBaselines = executeScmCommand(command, "list baselines -s \"${snapshot}\" -j");
		RTCUtils.exitOnError(command.getLastResult(), "Querying component baseline");
		if (jsonBaselines.contains("Ambiguous selector")) {
			throw new Exception("ERROR: too may snapshots by name ${snapshot}")
		}
		// Buscar la línea base del componente
		def componentsSnapshot = new JsonSlurper().parseText(jsonBaselines);
		componentsSnapshot.each { tmpComp ->
			if (tmpComp.name == component) {
				ret = tmpComp.baselines[0].name
			}
		}
		if (ret != null) {
			log "Baseline \"${ret}\" for component \"${component}\" in snapshot \"${snapshot}\" found"
		}
		else {
			throw new Exception("UNABLE TO FIND ${component} IN SNAPSHOT: ${snapshot}")
		}
		return ret;
	}
		
	/**
	 * Realiza la bajada de código.  Inicializa los ficheros changesetCompare.txt y 
	 *  changesetAccept.txt, fundamentales para el resto del workflow.
	 * @param command Comando RTC
	 * @param daemonsConfigDir 
	 */
	private void sincroniza(ScmCommand command, File daemonsConfigDir) {
		log "sincroniza (${baseline}).................."
		// Configurar el WSR (puede que haya que crearlo, recrearlo, etc.)
		setWorkspace(command, daemonsConfigDir)
		// ¿La release anterior acabó mal?  En ese caso, habrá cambios salientes que descartar
		boolean replaced = false;
		if (RTCUtils.isSet(stream)) {
			String dumpSalientes = executeScmCommand(command, 
				"compare workspace \"$workspaceRTC\" stream \"$stream\" -I sw -C \"|{name}|{email}|\" -D \"|yyyy-MM-dd-HH:mm:ss|\" -f o -j");
			Object objSalientes = new JsonSlurper().parseText(dumpSalientes);		
			if (objSalientes["direction"].size() > 0) {
				objSalientes["direction"].each { direction ->
					if (direction["outgoing-changes"] == true) {
						direction.components.each { outGoingComponent ->
							if (outGoingComponent.name == component 
									&& (outGoingComponent.changesets != null || outGoingComponent.baselines != null)) {
								// Reemplazar con la corriente
								replaced = true;
								log "Outgoing changes! Last release was surely wrong.  Replacing component ${component} with latest from ${stream} ..."
								TmpDir.tmp { File dirRemove ->
									ScmCommand removeCommand = 
									new ScmCommand(false, scmToolsHome, daemonsConfigDir.getCanonicalPath());
									removeCommand.initLogger(this);
									executeScmCommand(removeCommand, 
										"remove component --ignore-uncommitted \"${workspaceRTC}\" \"$component\" ", 
										dirRemove);
									RTCUtils.exitOnError(
										removeCommand.getLastResult(), "Removing component");
									executeScmCommand(removeCommand, 
										"workspace add-components \"${workspaceRTC}\" -s \"$stream\" \"$component\" ", 
										dirRemove);
									RTCUtils.exitOnError(
										removeCommand.getLastResult(), "Adding component");
								}
							}
						}
					}
				}
			}
		}
		// Descarga del contenido 
		executeScmCommand(command, "load \"${workspaceRTC}\" -f \"${component}\"");
		RTCUtils.exitOnError(command.getLastResult(), "loading workspace from RTC");
		if (RTCUtils.isSet(stream) && !RTCUtils.isSet(baseline)) {
			// No hay línea base
			String dumpCompare = executeScmCommand(command, "compare workspace \"$workspaceRTC\" stream \"$stream\" -I sw -C \"|{name}|{email}|\" -D \"|yyyy-MM-dd-HH:mm:ss|\" -f i");
			RTCUtils.dumpExit(parentWorkspace, "changesetCompare.txt", dumpCompare);
			RTCUtils.exitOnError(command.getLastResult(), "comparing workspace with stream");
			if (!replaced) {
				// Traer los cambios		
				String dumpAccept = executeScmCommand(command, "accept -C \"$component\" --flow-components -o -v --target \"$workspaceRTC\" -s \"${stream}\"");
				File fileAccept = RTCUtils.dumpExit(parentWorkspace, "changesetAccept.txt", dumpAccept);
				RTCUtils.exitOnError(command.getLastResult(), "accepting changes");
				if (autoResolve) {
					if (fileAccept.text.contains("#")) {
						// Resolver en masa, pisando los cambios con los que entren de la corriente
						String jsonConflicts = executeScmCommand(command, "show conflicts -j");
						RTCUtils.exitOnError(command.getLastResult(), "listing conflicts");
						def conflicts = new JsonSlurper().parseText(jsonConflicts);
						conflicts.conflicts.each { conflict ->
							executeScmCommand(command, "resolve conflict -o --proposed ${conflict.uuid}");
							RTCUtils.exitOnError(command.getLastResult(), "Resolving conflicts");
						}
					}
				}
			}
		}
		else if (RTCUtils.isSet(baseline)) {
			// Comparar la línea base con el WSR
			String dumpCompare = executeScmCommand(command, "compare workspace \"$workspaceRTC\" baseline \"$baseline\" -c \"${component}\" -I sw -C \"|{name}|{email}|\" -D \"|yyyy-MM-dd-HH:mm:ss|\" -f i");				
			RTCUtils.dumpExit(parentWorkspace, "changesetCompare.txt", dumpCompare);
			RTCUtils.exitOnError(command.getLastResult(), "comparing workspace with stream");
		}
		
	}
	
	// Comando de SCM.
	private String executeScmCommand(ScmCommand theCommand, String str, File dir = null) {
		return theCommand.ejecutarComando(
			str, 
			userRTC, 
			pwdRTC, 
			urlRTC, 
			dir==null?parentWorkspace:dir)
	}	

	/**
	 * @param workspaceRTC Workspace de repositorio RTC correspondiente a la
	 * corriente de la que se desea cargar el código.
	 */
	public void setWorkspaceRTC(String workspaceRTC) {
		this.workspaceRTC = workspaceRTC;
	}

	/**
	 * @param component Componente de la corriente RTC origen del código.
	 */
	public void setComponent(String component) {
		this.component = component;
	}

	/**
	 * @param stream Corriente RTC origen del código.
	 */
	public void setStream(String stream) {
		this.stream = stream;
	}

	/**
	 * @param baseline Línea base del componente.  Si se indica, 
	 * toma el contenido de dicha línea base.
	 */
	public void setBaseline(String baseline) {
		this.baseline = baseline;
	}

	/**
	 * @param snapshot Instantánea de la que extraer la línea base del componente.
	 */
	public void setSnapshot(String snapshot) {
		this.snapshot = snapshot;
	}

	/**
	 * @param recreateWS Indica si se debe recrear por fuerza el WSR, aunque exista.
	 */
	public void setRecreateWS(Boolean recreateWS) {
		this.recreateWS = recreateWS;
	}

	/**
	 * @param nosinc Informar a true si, a pesar de indicar baseline, 
	 * no se desea sincronizar la versión con el local.
	 */
	public void setNosinc(Boolean nosinc) {
		this.nosinc = nosinc;
	}

	/**
	 * @param autoResolve Si es cierto, resuelve cualquier posible conflicto 
	 * a favor de la corriente (--proposed, es decir, cambios entrantes).
	 */
	public void setAutoResolve(Boolean autoResolve) {
		this.autoResolve = autoResolve;
	}
	
	
}
