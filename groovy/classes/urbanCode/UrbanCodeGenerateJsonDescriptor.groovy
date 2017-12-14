package urbanCode

import buildtree.BuildBean
import es.eci.utils.base.Loggable;
import es.eci.utils.jenkins.GetJobsUtils
import es.eci.utils.pom.MavenCoordinates
import es.eci.utils.CheckSnapshots;
import es.eci.utils.NexusHelper;
import es.eci.utils.ParameterValidator;
import es.eci.utils.ParamsHelper;
import es.eci.utils.ScmCommand;
import es.eci.utils.TmpDir;
import es.eci.utils.Stopwatch;
import es.eci.utils.ZipHelper;
import es.eci.utils.StringUtil;
import git.GitUtils
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.lang.Closure;
import hudson.model.AbstractBuild

/**
 * Esta clase genera un descriptor completo, a partir de la última linea base 
 * de RTC o Git y lo sube a Nexus.<br/>
 * Notar que es <b>imprescindible</b> contar con una instalación local del cliente.  Se puede provisionar
 * desde:<br/>
 * <a href="http://nexus.elcorteingles.int/service/local/repositories/GC/content/ibm/urbanCode/udclient/6.1.0/udclient-6.1.0.zip">Cliente udclient en Nexus</a>
 * <br/> 
 * @see <a href="https://www-01.ibm.com/support/knowledgecenter/SS4GSP_6.1.2/com.ibm.udeploy.reference.doc/topics/cli_commands.html">Documentación del cliente udclient en IBM</a>
 */
class UrbanCodeGenerateJsonDescriptor extends Loggable {
	
	//---------------------------------------------------------------
	private String urlNexus;
	private String uDeployUser;
	private String uDeployPass;
	private String maven;
	private String parentWorkspace;
	private String systemWorkspace;
	
	// Acceso a Urban
	private String udClientCommand;
	private String urlUrbanCode;
	private String urbanUser;
	private String urbanPassword;
	
	// Información Urban
	private String nombreAplicacionUrban;
	private String instantaneaUrban;
	private String nuevaInstantaneaUrban;
	private String groupIdUrbanCode;
	private String actionDescUrban;
	
	// Stream target de RTC
	private String streamTarget;
	private String stream;
	private String streamFicha;
	
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
	private String targetBranch;
	
	//Mail
	private String managersMail;
	
	//---------------------------------------------------------------
	
	private boolean sinDescriptor = false;
	String jsonComplete = ""
	List<String> components = [];
	// Lista de componentes para Kiuwan, generaran una ficha con los nombres de los componentes en vez
	// de los componentes UrbanCode.
	List<String> componentsKiuwan = [];
	String jsonCompleteKiuwan = ""
	
	List<BuildBean> beanListTree = null;
	
	// Nueva funcionalidad para lanzamiento individual
	private boolean forceLaunch;
	
	def isNull = { String s ->
		return s == null || s.trim().length() == 0;
	}
	
	public String execute() {
		
		long millis = Stopwatch.watch {

			// Construcción del artifactId
			String artifactId = StringUtil.normalize(nombreAplicacionUrban)
			
			// Obtener el parámetro builtVersion. La lista de bean vendrá informada si partimos de un deploy
			if (beanListTree != null) {
				beanListTree.each {
					// Cargamos la versión de cada componente que entra en la ficha.
					if (it.name.contains("-COMP-")) {
						incluirVersion(it.name, it.builtVersion)
					}
				}
				// Establecemos el nombre de la ficha
				if (isNull(gitGroup)) {
					// Aquí se utiliza streamFicha que es el stream original sin utilizar la carga inicial, porque el
					// nombre de la nightly no tiene que llevar el stream modificado: Si existe streamInicial, en esta
					// clase, "stream" lleva el valor de "streamCargaInicial" y aquí no debe usarse.
					instantaneaUrban = "nightly_${StringUtil.normalize(streamFicha)}"
				} else
					instantaneaUrban = "nightly_${StringUtil.normalize(gitGroup)}"
			} else { // Estamos en release, fix o hotfix
				// Si no hay gitGroup, asumimos que estamos en RTC
				if (isNull(gitGroup)) {
				
					CheckSnapshots chk = new CheckSnapshots();
					chk.initLogger(this);
					boolean existsStream = chk.checkRTCstreams(streamTarget, rtcUser, rtcPass, rtcUrl);
					
					if(existsStream) {
					
						String baselinesJson = returnBaseLinesJson(streamTarget, instantaneaUrban, rtcUser, rtcPass, rtcUrl);
						
						// log "--- INFO: Lineas base: " + baselinesJson
						
						if(baselinesJson != null && !baselinesJson.contains("<title></title>")) {
							// Parseamos las baselines
							JsonSlurper jsonSlurper = new JsonSlurper()
							Object object = jsonSlurper.parseText(baselinesJson)
							
							object.each {
								String jobName = "${stream} -COMP- ${it.name}";
								String compVersion = it.baselines.name;

								incluirVersion(jobName, compVersion)
							}
						} else { // si existe baseline
							log "!!! WARNING: No hay linea base. No se generara el descriptor."
							sinDescriptor = true
						}
					} else {
						log "!!! WARNING: No existe el streamTarget \"${streamTarget}\". No se generara el descriptor."
						sinDescriptor = true
					}
				
				} else { // fin bloque RTC, inicio del bloque GIT
					
					List<String> gitGrouopComponentsList = [];
					
					File jenkinsComponentsFile = new File("${parentWorkspace}/jenkinsComponents.txt");
					// Si es un lanzamiento manual, el fichero lo generamos en el momento y no estará 
					// en el parentWorkspace
					if (isForceLaunch()) {
						jenkinsComponentsFile = new File("${systemWorkspace}/jenkinsComponents.txt");
					}
					jenkinsComponentsFile.eachLine { String line ->
						// log "CompoGit: " + line
						gitGrouopComponentsList.add(line);
					}
					
					gitGrouopComponentsList.each { String componentName ->
						String compVersion = new GitUtils(gitUser, gitHost, gitCommand).getRepositoryLastTag(gitGroup, 
							componentName, "RELEASE");
														
						if(compVersion != null) {
							String jobName = "${gitGroup} -COMP- ${componentName}";
							incluirVersion(jobName, compVersion);
						}							
					}
				}
			} // Fin de método de release, fix o hotfix
			
			// Traza para clarificar el log
			if (components.empty) {
				log "### ERROR: Ningun componente ha entrado en la ficha."
			}
			
			if ( !sinDescriptor && !components.empty ) {
				
				// Si viene informado un nuevo nombre de instantanea, posiblemente estamos en lanzamiento manual
				// por lo que lo utilizamos para crear una nueva ficha
				if (!isNull(nuevaInstantaneaUrban)) {
					instantaneaUrban = nuevaInstantaneaUrban
				}
				
				jsonComplete = JsonOutput.toJson(["name": "${instantaneaUrban}", "application": "${nombreAplicacionUrban}" ,
					"description": getActionDescUrban(), "versions" : components])
				
				log ""
				log "--- INFO: descriptor: " + jsonComplete
				log ""
				
				// Descriptor Kiuwan
				if (isNull(gitGroup)) {
					jsonCompleteKiuwan = JsonOutput.toJson(["source": "${streamFicha}", "managersMail": "${managersMail}", 
						"versions" : componentsKiuwan])
				} else {
					jsonCompleteKiuwan = JsonOutput.toJson(["source": "${gitGroup}", "managersMail": "${managersMail}", 
						"branch": "${targetBranch}", "versions" : componentsKiuwan])
				}				
			
				// Se sube el nuevo descriptor a Nexus.			
				TmpDir.tmp { tmpDir ->
					File tmp = new File(tmpDir, "descriptor.json")
					tmp.text = jsonComplete
					// Se añade la parte de Kiuwan
					File tmpK = null
					if (isNull(gitGroup)) {
						tmpK = new File(tmpDir, "rtc.json")
					} else {
						tmpK = new File(tmpDir, "git.json")
					}
					tmpK.text = jsonCompleteKiuwan
					File zip = ZipHelper.addDirToArchive(tmpDir);
					
					try {
						log "--- INFO: Subiendo el descriptor a: G:[${groupIdUrbanCode}] A:[${artifactId}] V:[${instantaneaUrban}]"
						
						NexusHelper.uploadToNexus(
							maven,
							groupIdUrbanCode,
							artifactId,
							"${instantaneaUrban}",
							zip.getCanonicalPath(),
							urlNexus,
							"zip",
							this.logger.logger,
							uDeployUser,
							uDeployPass)
						
						log "--- INFO: OK"
					} catch (Exception e) {
						log "### ERROR: Ha habido un problmema subiendo el descriptor completo a Nexus"
						e.printStackTrace();
					}
					finally {
						zip.delete()
					}
				} // fin tmpDir
			} // fin comprobación del descriptor
			
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
	private void incluirVersion (String jobName, String version) {
		
		def job = hudson.model.Hudson.instance.getJob(jobName);
		ParamsHelper pHelper = new ParamsHelper();
		String builtVersion = ""
			
		// Si el job existe
		if(job != null) {
			
			// Obtener el parámetro componenteUrbanCode
			String compoUrbanValue = pHelper.getDefaultParameterValue(job, "componenteUrbanCode");
			// Obtener el parámetro documentacion
			String compoUrbanValueDoc = pHelper.getDefaultParameterValue(job, "documentacion");
			// Para Kiuwan, obtener el nombre del componente
			String compoValue = pHelper.getDefaultParameterValue(job, "component");
					
			componentsKiuwan.add(["${compoValue}": "${version}".replace(']','').replace('[','')])
			
			if ( !isNull(compoUrbanValue) ) {
				
				// Se consulta el builtVersion para comprobar si estás generando un versión abierta
				//log "Comprobando si la versión está abierta..."
				if (version.contains("SNAPSHOT")) {
					builtVersion = getTimeStampVersion(compoUrbanValue, version)
				} else {
					builtVersion = version
				}
				
				log "--- INFO: Se incluye en la ficha el componente: ${compoUrbanValue}"
				components.add(["${compoUrbanValue}": "${builtVersion}".replace(']','').replace('[','')])				
				
				if( !isNull(compoUrbanValueDoc)) {
					if(compoUrbanValueDoc.toString().trim().equals("true")) {
						log "--- INFO: El componente [${compoUrbanValue}] genera datos ficticios para la documentacion..."
						components.add(["${compoUrbanValue}.doc":"${builtVersion}".replace(']','').replace('[','')]);
					}
				}
				
			} else
				log "!!! WARNING: El componente no tiene informacion de Urban, no ira a la ficha."
		} else {
			log "!!! WARNING: El job [${jobName}] no existe en Jenkins."
		}
	}
	
	/**
	 * Recupera de Nexus el timestamp de las versiones snapshot
	 * @param ver
	 * @return
	 */
	private String getTimeStampVersion (String comp, String version) {
		
		UrbanCodeExecutor urbanExecutor = new UrbanCodeExecutor(udClientCommand, urlUrbanCode, urbanUser, urbanPassword);
		UrbanCodeComponentInfoService service = new UrbanCodeComponentInfoService(urbanExecutor);
		NexusHelper nexusExecutor = new NexusHelper(urlNexus)
		
		service.initLogger(this);
		
		MavenCoordinates coords = service.getCoordinates(comp);
		coords.setVersion(version);
		
		// Si el repo es privado
		if ( coords.getRepository() != "public") {
			nexusExecutor.setNexus_user(uDeployUser)
			nexusExecutor.setNexus_pass(uDeployPass)
		}
						
		// Fix para cuando en Urban no existan los componentes
		if ( StringUtil.isNull(coords.groupId) || StringUtil.isNull(coords.artifactId) ) {
			log "--- INFO: Se utiliza versión SNAPSHOT sin resolver para el componente: " + comp
			return coords.version
		} else {
			return nexusExecutor.resolveSnapshot(coords);	
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
	 * @return the uDeployUser
	 */
	public String getuDeployUser() {
		return uDeployUser;
	}

	/**
	 * @param uDeployUser the uDeployUser to set
	 */
	public void setuDeployUser(String uDeployUser) {
		this.uDeployUser = uDeployUser;
	}

	/**
	 * @return uDeployPass the uDeployPass to set
	 */
	public String getuDeployPass() {
		return uDeployPass;
	}

	/**
	 * @param uDeployPass the uDeployPass to set
	 */
	public void setuDeployPass(String uDeployPass) {
		this.uDeployPass = uDeployPass;
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
	 * @return the udClientCommand
	 */
	public String getUdClientCommand() {
		return udClientCommand;
	}

	/**
	 * @return the systemWorkspace
	 */
	public String getSystemWorkspace() {
		return systemWorkspace;
	}

	/**
	 * @param systemWorkspace the systemWorkspace to set
	 */
	public void setSystemWorkspace(String systemWorkspace) {
		this.systemWorkspace = systemWorkspace;
	}

	/**
	 * @param udClientCommand the udClientCommand to set
	 */
	public void setUdClientCommand(String udClientCommand) {
		this.udClientCommand = udClientCommand;
	}

	/**
	 * @return the urlUrbanCode
	 */
	public String getUrlUrbanCode() {
		return urlUrbanCode;
	}

	/**
	 * @param urlUrbanCode the urlUrbanCode to set
	 */
	public void setUrlUrbanCode(String urlUrbanCode) {
		this.urlUrbanCode = urlUrbanCode;
	}

	/**
	 * @return the urbanUser
	 */
	public String getUrbanUser() {
		return urbanUser;
	}

	/**
	 * @param urbanUser the urbanUser to set
	 */
	public void setUrbanUser(String urbanUser) {
		this.urbanUser = urbanUser;
	}

	/**
	 * @return the urbanPassword
	 */
	public String getUrbanPassword() {
		return urbanPassword;
	}

	/**
	 * @param urbanPassword the urbanPassword to set
	 */
	public void setUrbanPassword(String urbanPassword) {
		this.urbanPassword = urbanPassword;
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
	 * @return the actionDescUrban
	 */
	public String getActionDescUrban() {
		return actionDescUrban;
	}

	/**
	 * @param actionDescUrban the actionDescUrban to set
	 */
	public void setActionDescUrban(String actionDescUrban) {
		this.actionDescUrban = actionDescUrban;
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
	 * @return the streamFicha
	 */
	public String getStreamFicha() {
		return streamFicha;
	}

	/**
	 * @param streamFicha the streamFicha to set
	 */
	public void setStreamFicha(String streamFicha) {
		this.streamFicha = streamFicha;
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
	 * @return the targetBranch
	 */
	public String getTargetBranch() {
		return targetBranch;
	}

	/**
	 * @param targetBranch the targetBranch to set
	 */
	public void setTargetBranch(String targetBranch) {
		this.targetBranch = targetBranch;
	}

	/**
	 * @return the beanListTree
	 */
	public List<BuildBean> getBeanListTree() {
		return beanListTree;
	}

	/**
	 * @param beanListTree the beanListTree to set
	 */
	public void setBeanListTree(List<BuildBean> beanListTree) {
		this.beanListTree = beanListTree;
	}

	/**
	 * @return the forceLaunch
	 */
	public boolean isForceLaunch() {
		return forceLaunch;
	}

	/**
	 * @param forceLaunch the forceLaunch to set
	 */
	public void setForceLaunch(boolean forceLaunch) {
		this.forceLaunch = forceLaunch;
	}

	/**
	 * @return the managersMail
	 */
	public String getManagersMail() {
		return managersMail;
	}

	/**
	 * @param managersMail the managersMail to set
	 */
	public void setManagersMail(String managersMail) {
		this.managersMail = managersMail;
	}

	/**
	 * @return the nuevaInstantaneaUrban
	 */
	public String getNuevaInstantaneaUrban() {
		return nuevaInstantaneaUrban;
	}

	/**
	 * @param nuevaInstantaneaUrban the nuevaInstantaneaUrban to set
	 */
	public void setNuevaInstantaneaUrban(String nuevaInstantaneaUrban) {
		this.nuevaInstantaneaUrban = nuevaInstantaneaUrban;
	}
	
}
