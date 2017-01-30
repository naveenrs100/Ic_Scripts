package rtc.commands

import rtc.RTCUtils
import es.eci.utils.ParameterValidator
import es.eci.utils.ScmCommand
import es.eci.utils.Stopwatch
import es.eci.utils.TmpDir;


/**
 * Esta clase modela un comando de etiquetado de código sobre un 
 * repositorio RTC (comando RTC create baseline o create snapshot)
 * <br/>
 * Parámetros de entrada:<br/>
 * <br/>
 * --- OBLIGATORIOS<br/>
 * <b>scmToolsHome</b> Directorio raíz de las herramientas RTC <br/>
 * <b>userRTC</b> Usuario RTC<br/>
 * <b>pwdRTC</b> Password RTC<br/>
 * <b>urlRTC</b> URL de repositorio RTC<br/>
 * <b>parentWorkspace</b> Directorio de ejecución<br/>
 * <b>stream</b> Corriente RTC sobre la que se hace el etiquetado<br/>
 * <b>tagType</b> Tipo de etiquetado (snapshot/baseline)<br/>
 * <br/>
 * --- OPCIONALES<br/>
 * <b>light</b> Indica si se debe usar o no la versión ligera del comando scm.  VALOR POR DEFECTO: true<br/>
 * <b>version</b> Versión de la que se hace línea base (en su caso).  Si viene
 * informada, se toma tal cual; si vale 'local', debe leerse del version.txt; si
 * no viene informada en absoluto, se toma el version.txt con un número de build.  
 * (Valor por defecto: local)<br/>
 * <b>component</b> Componente del cual se hace línea base (en su caso)<br/>
 * <b>workspaceRTC</b> Workspace de repositorio RTC del que se hace la línea base,
 * si es el caso<br/>
 * <b>instantanea</b> Nombre de la instantánea a crear (en su caso)<br/>
 * <b>description</b> Descripción de la instantánea (en su caso)<br/>
 * <b>RTCVersionFile</b> Fichero con la versión (Valor por defecto: version.txt)<br/>
 * <b>compJobNumber</b> Variable informada por jenkins con el número actual de build<br/>
 * <b>streamInVersion</b> Si es cierto, incluye el nombre de la corriente en la versión.
 * (Valor por defecto: false)<br/>
 * <b>makeSnapshot</b> Si es cierto, hace la etiqueta.  Puede informarse en momentos
 * puntuales para no hacerla (algunos casos de deploy, p. ej.)<br/>
 * 
 */
class RTCTaggerCommand extends AbstractRTCCommand {

	//--------------------------------------------------------------
	// Propiedades de la clase
	
	//-----------------------> Obligatorios
	private String stream; 
	private String tagType;
	//-----------------------> Opcionales
	private String version = "local";
	private String component;
	private String workspaceRTC;
	private String instantanea;
	private String description;
	private String RTCVersionFile = "version.txt";
	private String compJobNumber;
	private Boolean streamInVersion = false;
	private Boolean makeSnapshot = Boolean.TRUE;
	
	//--------------------------------------------------------------
	// Métodos de la clase
	
	// Busca la versión en un fichero con la siguiente estructura:
	//
	// version=XXXXX
	// groupId=XXXXX
	private String findVersion(File versionTxtFile) {
		String version = null;
		String text = versionTxtFile.text
		text.eachLine { line ->
			if (line.startsWith("version=")) {
				version = line.substring(line.indexOf("=") + 1).trim();
			}
		}
		return version;
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
					.add("RTCVersionFile", RTCVersionFile)
					.add("parentWorkspace", parentWorkspace)
					.add("stream", stream)
					.add("tagType", tagType)
					.add("light", light)
						.build().validate();
			
			long millis = Stopwatch.watch {
				ScmCommand command = null;
				try {
					command = new ScmCommand(light, scmToolsHome, daemonsConfigDir.getCanonicalPath());
					command.initLogger(this);
					
					String versionLocal = "";
					File versionTxtFile = RTCUtils.findFile(parentWorkspace, RTCVersionFile);
					// Trata de recuperar la versión
					if (RTCUtils.isSet(instantanea) && tagType == "snapshot") {
						// Si viene informado el parámetro de instantánea y además se está haciendo la
						//  instantánea y NO una línea base, se usa este parámetro
						versionLocal = instantanea;
					}
					else if (versionTxtFile != null) {
						versionLocal = findVersion(versionTxtFile);
					}
					else {
						throw new Exception("No existe la variable INSTANTANEA ni tampoco el fichero $RTCVersionFile");
					}
					
					// Descripción por defecto para instantáneas
					if (!RTCUtils.isSet(description)) {
						description = "JENKINS BASELINE";
					}
					
					// Si version viene vacía, se toma la versión local con el número de
					//	build; en cambio, si vale 'local', se toma solo la versión local.
					if (!RTCUtils.isSet(version)) {
						version="${versionLocal}-build:${compJobNumber}"
					}
					else if(version == "local") {
						version = versionLocal;
					}
					
					String versionTxt = version;
					
					if (tagType == "snapshot" && streamInVersion) {
						versionTxt = "${stream} - ${version}";
					}
					if (tagType == "baseline") {
						if (makeSnapshot) {
							log "baseline: ${versionTxt}"						
							command.ejecutarComando("create baseline \"${workspaceRTC}\" \"${versionTxt}\" \"${component}\" --overwrite-uncommitted" , userRTC, pwdRTC, urlRTC, parentWorkspace); 
							RTCUtils.exitOnError(command.getLastResult(), "Creating baseline");
						}
						else {
							log "¡¡NO CIERRA baseline!!";
						}
					}
					else {
						command.ejecutarComando("create snapshot \"${stream}\" -n \"${versionTxt}\" -d \"${description}\"" , userRTC, pwdRTC, urlRTC, parentWorkspace);
						RTCUtils.exitOnError(command.getLastResult(), "Creating snapshot");
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
			log "stepRTCTagger: ${millis} mseg.";
		}
	}

	/**
	 * @param makeSnapshot Si es cierto, hace la etiqueta.
	 */
	public void setMakeSnapshot(Boolean makeSnapshot) {
		this.makeSnapshot = makeSnapshot;
	}
	
	/**
	 * @param stream Corriente RTC sobre la que se hace el etiquetado.
	 */
	public void setStream(String stream) {
		this.stream = stream;
	}

	/**
	 * @param tagType Tipo de etiquetado (snapshot/baseline).
	 */
	public void setTagType(String tagType) {
		this.tagType = tagType;
	}

	/**
	 * @param version Versión de la que se hace línea base (en su caso).  Si viene
	 * informada, se toma tal cual; si vale 'local', debe leerse del version.txt; si
	 * no viene informada en absoluto, se toma el version.txt con un número de build.
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * @param component Componente del cual se hace línea base (en su caso).
	 */
	public void setComponent(String component) {
		this.component = component;
	}

	/**
	 * @param description Descripción de la instantánea (en su caso).
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param rTCVersionFile Fichero con la versión.
	 */
	public void setRTCVersionFile(String rTCVersionFile) {
		RTCVersionFile = rTCVersionFile;
	}

	/**
	 * @param instantanea Nombre de la instantánea a crear (en su caso)
	 */
	public void setInstantanea(String instantanea) {
		this.instantanea = instantanea;
	}

	/**
	 * @param compJobNumber Variable informada por jenkins con el 
	 * número actual de build.
	 */
	public void setCompJobNumber(String compJobNumber) {
		this.compJobNumber = compJobNumber;
	}
	
	/**
	 * @param streamInVersion Si es cierto, incluye el nombre de la 
	 * corriente en la versión.
	 */
	public void setStreamInVersion(Boolean streamInVersion) {
		this.streamInVersion = streamInVersion;
	}

	/**
	 * @param workspaceRTC Workspace de repositorio de RTC al que reincorporar
	 * los cambios.
	 */
	public void setWorkspaceRTC(String workspaceRTC) {
		this.workspaceRTC = workspaceRTC;
	}
	
	
}
