/**
 * Esta clase genera un descriptor completo, a partir de la última linea base de RTC o Git y lo sube a Nexus.
 */
package urbanCode

import es.eci.utils.base.Loggable;
import es.eci.utils.jenkins.GetJobsUtils
import es.eci.utils.CheckSnapshots;
import es.eci.utils.NexusHelper;
import es.eci.utils.ParameterValidator;
import es.eci.utils.ParamsHelper;
import es.eci.utils.ScmCommand;
import es.eci.utils.TmpDir;
import es.eci.utils.Stopwatch;
import es.eci.utils.ZipHelper;
import git.GitUtils
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import hudson.model.AbstractBuild

/**
 * @author gdrodriguez
 *
 */
class UrbanCodeGenerateJsonDescriptor extends Loggable {
	
	//---------------------------------------------------------------
	private String urlNexus;
	private String maven;
	private String parentWorkspace;
	
	// Información Urban
	private String nombreAplicacionUrban;
	private String instantaneaUrban;
	private String groupIdUrbanCode;
	
	// Stream target de RTC
	private String streamTarget;
	private String stream;
	
	// Conexión con RTC
	private String rtcUser;
	private String rtcPass;
	private String rtcUrl;
	
	// Conexión con GIT
	private String gitUser;
	private String gitCommand;
	private String gitHost;
	
	// Información GIT
	private String gitGroup;
	private String gitReleaseComp;	// Lista de todos los componentes git en el grupo
	private String gitDeployComp;	// Lista de los componentes que efectivamente componen la construcción
	
	// Lanzamiento desde componente
	private boolean componentLauch
	//---------------------------------------------------------------
	
	String jsonComplete = ""
	List<String> components = [];
	
	
	def isNull = { String s ->
		return s == null || s.trim().length() == 0;
	}
	
	public String execute() {
		
		long millis = Stopwatch.watch {

			// Construcción del artifactId
			String artifactId = nombreAplicacionUrban.replace(" - ", "_").replace(" -", "_").replace("- ", "_").replace(" ", "_");
			
			// Si no hay gitGroup, asumimos que estamos en RTC
			if (isNull(gitGroup)) {
				
				CheckSnapshots chk = new CheckSnapshots();
				chk.initLogger { println it };
				boolean existsStream = chk.checkRTCstreams(streamTarget, rtcUser, rtcPass, rtcUrl);
					
				if(existsStream) {
					String baselinesJson = returnBaseLinesJson(streamTarget, instantaneaUrban, rtcUser, rtcPass, rtcUrl);
					
					log "--- INFO: Lineas base: " + baselinesJson
					
					if(baselinesJson != null) {
						// Parseamos las baselines
						JsonSlurper jsonSlurper = new JsonSlurper()
						Object object = jsonSlurper.parseText(baselinesJson)
						
						object.each {
							String jobName = "${stream} -COMP- ${it.name}";
							incluirVersion(jobName, it.baselines.name)
						}
					} else { // si existe baseline
						log "!!! WARNING: No hay linea base. No se generara el descriptor."
					}	
				} else {
					log "!!! WARNING: No existe el streamTarget \"${streamTarget}\". No se generara el descriptor."
				}
				
			} else { // fin bloque RTC, inicio del bloque GIT
							
				List<String> gitGrouopComponentsList = [];
				
				File jenkinsComponentsFile = new File("${parentWorkspace}/jenkinsComponents.txt");
				jenkinsComponentsFile.eachLine { String line ->
					log "CompoGit: " + line
					gitGrouopComponentsList.add(line);
				}
			
				// Si es desde corriente
				/** if (!isComponentLauch()) {
					String[] componentsUrban = gitReleaseComp.split(",");
					componentsUrban.each { String componentPair ->
						String componentName = componentPair.split(":")[0];
						String componentVersion = new GitUtils(gitUser, gitHost, gitCommand).getRepositoryLastTag(gitGroup, componentName, "RELEASE");
						String jobName = "${gitGroup} -COMP- ${componentName}";
						
						incluirVersion(jobName, componentVersion)				
					}
				} else { // Desde componente
					def job = hudson.model.Hudson.instance.getJob(jobName);
					ParamsHelper pHelper = new ParamsHelper();
					String componentName = pHelper.getDefaultParameterValue(job, "component");
					String componentVersion = new GitUtils(gitUser, gitHost, gitCommand).getRepositoryLastTag(gitGroup, componentName, "RELEASE");
					String jobName = "${gitGroup} -COMP- ${componentName}";
					
					incluirVersion(jobName, componentVersion)
				} */
				
				
			}
			
			jsonComplete = JsonOutput.toJson(["name": "${instantaneaUrban}", "application": "${nombreAplicacionUrban}" ,
				"description": "Snapshot Urban Code", "versions" : components])
			
			log ""
			log "--- INFO: descriptor: " + jsonComplete
			log ""
		
			// Se sube el nuevo descriptor a Nexus.
			NexusHelper nHelper = new NexusHelper(urlNexus);
		
			TmpDir.tmp { tmpDir ->
				File tmp = new File(tmpDir, "descriptor.json")
				tmp.text = jsonComplete
				File zip = ZipHelper.addDirToArchive(tmpDir);
				
				try {
					log "--- INFO: Subiendo el descriptor a: G:[${groupIdUrbanCode}] A:[${artifactId}] V:[${instantaneaUrban}]"
					
					nHelper.uploadToNexus(
						maven,
						groupIdUrbanCode,
						artifactId,
						"${instantaneaUrban}",
						zip.getCanonicalPath(),
						"fichas_despliegue",
						"zip"
					)
					
					log "--- INFO: OK"
				} catch (Exception e) {
					log "### ERROR: Ha habido un problmema subiendo el descriptor completo a Nexus"
					e.printStackTrace();
				}
				finally {
					zip.delete()
				}
			} // fin tmpDir
			
		} // fin tiempo total
		
		return jsonComplete;
		
	} // fin execute
	
	/**
	* Devuelve el json con las baselines de cada componente segun el snapshot formado de
	* conjuntar streamTarget e instantaea.
	* @param streamTarget
	* @param instantanea
	* @param rtcUser
	* @param rtcPass
	* @param rtcUrl
	* @return (String) baselinesJson
	*/
	private String returnBaseLinesJson(streamTarget, instantanea, rtcUser, rtcPass, rtcUrl) {
		String baselinesJson = null;
		ScmCommand scm = new ScmCommand(ScmCommand.Commands.SCM);

		String command = "list baselines -s \"" + streamTarget + " - " + instantanea + "\" -j"
		
		log "Command: " + command

		TmpDir.tmp { File baseDir ->
			baselinesJson = scm.ejecutarComando(command, rtcUser, rtcPass, rtcUrl, baseDir);
		}

		return baselinesJson;
	}
	/**
	 * Puebla el mapa componenteUrban-version.
	 * @param jobName Nombre del job en Jenkins
	 * @param version Versión que se va a indicar a Urban
	 */
	private void incluirVersion (jobName, version) {
		
		def job = hudson.model.Hudson.instance.getJob(jobName);
		ParamsHelper pHelper = new ParamsHelper();
			
		// Si el job existe
		if(job != null) {
			
			// Obtener el parámetro componenteUrbanCode
			String compoUrbanValue = pHelper.getDefaultParameterValue(job, "componenteUrbanCode");
			// Obtener el paŕametro documentacion
			String compoUrbanValueDoc = pHelper.getDefaultParameterValue(job, "documentacion");
			
			if ( !isNull(compoUrbanValue) ) {
				log "--- INFO: Se incluye en la ficha el componente: ${compoUrbanValue}"
				components.add(["${compoUrbanValue}": "${version}".replace(']','').replace('[','')])
			} else
				log "!!! WARNING: El componenteUrban [${compoUrbanValue}] esta vacio o no existe, no ira a la ficha."
			
			if( !isNull(compoUrbanValueDoc)) {
				if(compoUrbanValueDoc.toString().trim().equals("true")) {
					log "--- INFO: El componente [${compoUrbanValue}] genera datos ficticios para la documentacion..."
					components.add(["${compoUrbanValue}.doc":"${version}".replace(']','').replace('[','')]);
				}
			}
		} else {
			log "!!! WARNING: El job [${jobName}] no existe en Jenkins."
		}
	}

	/**
	 * @return the urlNexus
	 */
	public String getUrlNexus() {
		return urlNexus;
	}

	/**
	 * @param urlNexus the urlNexus to set
	 */
	public void setUrlNexus(String urlNexus) {
		this.urlNexus = urlNexus;
	}

	/**
	 * @return the maven
	 */
	public String getMaven() {
		return maven;
	}

	/**
	 * @param maven the maven to set
	 */
	public void setMaven(String maven) {
		this.maven = maven;
	}

	/**
	 * @return the parentWorkspace
	 */
	public String getParentWorkspace() {
		return parentWorkspace;
	}

	/**
	 * @param parentWorkspace the parentWorkspace to set
	 */
	public void setParentWorkspace(String parentWorkspace) {
		this.parentWorkspace = parentWorkspace;
	}

	/**
	 * @return the nombreAplicacionUrban
	 */
	public String getNombreAplicacionUrban() {
		return nombreAplicacionUrban;
	}

	/**
	 * @param nombreAplicacionUrban the nombreAplicacionUrban to set
	 */
	public void setNombreAplicacionUrban(String nombreAplicacionUrban) {
		this.nombreAplicacionUrban = nombreAplicacionUrban;
	}

	/**
	 * @return the instantaneaUrban
	 */
	public String getInstantaneaUrban() {
		return instantaneaUrban;
	}

	/**
	 * @param instantaneaUrban the instantaneaUrban to set
	 */
	public void setInstantaneaUrban(String instantaneaUrban) {
		this.instantaneaUrban = instantaneaUrban;
	}

	/**
	 * @return the groupIdUrbanCode
	 */
	public String getGroupIdUrbanCode() {
		return groupIdUrbanCode;
	}

	/**
	 * @param groupIdUrbanCode the groupIdUrbanCode to set
	 */
	public void setGroupIdUrbanCode(String groupIdUrbanCode) {
		this.groupIdUrbanCode = groupIdUrbanCode;
	}

	/**
	 * @return the streamTarget
	 */
	public String getStreamTarget() {
		return streamTarget;
	}

	/**
	 * @param streamTarget the streamTarget to set
	 */
	public void setStreamTarget(String streamTarget) {
		this.streamTarget = streamTarget;
	}

	/**
	 * @return the stream
	 */
	public String getStream() {
		return stream;
	}

	/**
	 * @param stream the stream to set
	 */
	public void setStream(String stream) {
		this.stream = stream;
	}

	/**
	 * @return the rtcUser
	 */
	public String getRtcUser() {
		return rtcUser;
	}

	/**
	 * @param rtcUser the rtcUser to set
	 */
	public void setRtcUser(String rtcUser) {
		this.rtcUser = rtcUser;
	}

	/**
	 * @return the rtcPass
	 */
	public String getRtcPass() {
		return rtcPass;
	}

	/**
	 * @param rtcPass the rtcPass to set
	 */
	public void setRtcPass(String rtcPass) {
		this.rtcPass = rtcPass;
	}

	/**
	 * @return the rtcUrl
	 */
	public String getRtcUrl() {
		return rtcUrl;
	}

	/**
	 * @param rtcUrl the rtcUrl to set
	 */
	public void setRtcUrl(String rtcUrl) {
		this.rtcUrl = rtcUrl;
	}

	/**
	 * @return the gitUser
	 */
	public String getGitUser() {
		return gitUser;
	}

	/**
	 * @param gitUser the gitUser to set
	 */
	public void setGitUser(String gitUser) {
		this.gitUser = gitUser;
	}

	/**
	 * @return the gitCommand
	 */
	public String getGitCommand() {
		return gitCommand;
	}

	/**
	 * @param gitCommand the gitCommand to set
	 */
	public void setGitCommand(String gitCommand) {
		this.gitCommand = gitCommand;
	}

	/**
	 * @return the gitHost
	 */
	public String getGitHost() {
		return gitHost;
	}

	/**
	 * @param gitHost the gitHost to set
	 */
	public void setGitHost(String gitHost) {
		this.gitHost = gitHost;
	}

	/**
	 * @return the gitGroup
	 */
	public String getGitGroup() {
		return gitGroup;
	}

	/**
	 * @param gitGroup the gitGroup to set
	 */
	public void setGitGroup(String gitGroup) {
		this.gitGroup = gitGroup;
	}

	/**
	 * @return the gitReleaseComp
	 */
	public String getGitReleaseComp() {
		return gitReleaseComp;
	}

	/**
	 * @param gitReleaseComp the gitReleaseComp to set
	 */
	public void setGitReleaseComp(String gitReleaseComp) {
		this.gitReleaseComp = gitReleaseComp;
	}

	/**
	 * @return the gitDeployComp
	 */
	public String getGitDeployComp() {
		return gitDeployComp;
	}

	/**
	 * @param gitDeployComp the gitDeployComp to set
	 */
	public void setGitDeployComp(String gitDeployComp) {
		this.gitDeployComp = gitDeployComp;
	}

	/**
	 * @return the componentLauch
	 */
	public boolean isComponentLauch() {
		return componentLauch;
	}

	/**
	 * @param componentLauch the componentLauch to set
	 */
	public void setComponentLauch(boolean componentLauch) {
		this.componentLauch = componentLauch;
	}
	
}
