package es.eci.utils.jenkins

import components.MavenComponent
import es.eci.utils.GitBuildFileHelper
import es.eci.utils.RTCBuildFileHelper
import es.eci.utils.ScmCommand
import es.eci.utils.TmpDir
import es.eci.utils.base.Loggable
import es.eci.utils.pom.SortGroupsStrategy
import es.eci.utils.versioner.PomXmlOperations;
import git.commands.GitCloneCommand
import git.commands.GitLogCommand
import groovy.json.JsonSlurper
import hudson.model.*
import rtc.commands.RTCCreateRepositoryWorkspace

class GetJobsUtils extends Loggable {

	// Parámetros de RTC			// Parámetros que viene de Git.
									String commitsId;
	String projectNature;			String keystoreVersion;
	String action;					String gitHost;
	String onlyChanges;				String privateGitLabToken;
	String workspaceRTC;			String technology;
	String jenkinsHome;				String urlGitlab;
	String scmToolsHome;			String urlNexus;
	String daemonsConfigDir;		String lastUserIC;
	String userRTC;					String gitCommand;
	String pwdRTC;					String mavenHome;
	String urlRTC;					String gitGroup;
	String parentWorkspace;			String branch;
	String stream;
	String todos_o_ninguno;
	String getOrdered;
	String componentesRelease;
	String streamCargaInicial;
	String arrastrarDependencias;


	public GetJobsUtils(String projectNature, String action,
			String onlyChanges, String workspaceRTC, String jenkinsHome,
			String scmToolsHome, String daemonsConfigDir, String userRTC,
			String pwdRTC, String urlRTC, String parentWorkspace,
			String commitsId, String gitHost, String keystoreVersion,
			String privateGitLabToken, String technology, String urlGitlab,
			String urlNexus, String lastUserIC, String gitCommand,
			String mavenHome, String stream, String todos_o_ninguno,
			String getOrdered, String componentesRelease, String gitGroup, String branch,
			String streamCargaInicial, String arrastrarDependencias) {
		super();
		this.projectNature = projectNature;
		this.action = action;
		this.onlyChanges = onlyChanges;
		this.workspaceRTC = workspaceRTC;
		this.jenkinsHome = jenkinsHome;
		this.scmToolsHome = scmToolsHome;
		this.daemonsConfigDir = daemonsConfigDir;
		this.userRTC = userRTC;
		this.pwdRTC = pwdRTC;
		this.urlRTC = urlRTC;
		this.parentWorkspace = parentWorkspace;
		this.commitsId = commitsId;
		this.gitHost = gitHost;
		this.keystoreVersion = keystoreVersion;
		this.privateGitLabToken = privateGitLabToken;
		this.technology = technology;
		this.urlGitlab = urlGitlab;
		this.urlNexus = urlNexus;
		this.lastUserIC = lastUserIC;
		this.gitCommand = gitCommand;
		this.mavenHome = mavenHome;
		this.stream = stream;
		this.todos_o_ninguno = todos_o_ninguno;
		this.getOrdered = getOrdered;
		this.componentesRelease = componentesRelease;
		this.gitGroup = gitGroup;
		this.branch = branch;
		this.streamCargaInicial = streamCargaInicial;
		this.arrastrarDependencias = arrastrarDependencias;
	}

	/**
	 * En el caso, bastante común, de necesitar una release parcial, se aplica este parámetro
	 * Contiene los componentes que entran en la construcción, separados por comas
	 * @return List<String> listaComponentesRelease
	 */
	def getComponentsReleaseList() {
		log("Calculamos componentes introducidos a mano...")
		List<String> listaComponentesRelease = [];
		if (componentesRelease != null && componentesRelease.trim().length() > 0) {
			componentesRelease.split(",").each { String compo ->
				listaComponentesRelease.add(compo.trim());
			}
			log("Componentes introducidos a mano -> ${listaComponentesRelease}")
		}
		return listaComponentesRelease;
	}

	/**
	 * Devuelve la lista de componentes que cuelgan de un grupo scm según el
	 * scm del que se trate (stream para RTC o gitGroup para Git)
	 * @return List<String> scmComponentsList
	 */
	public List<String> getScmComponentsList() {
		log("projectNature = ${projectNature}")
		List<String> scmComponentsList;

		if(projectNature == "rtc") {
			// Antes de empezar a operar con RTC se crea el WORKSPACE
			// en caso de ser necesario.
			RTCCreateRepositoryWorkspace wksCommand = new RTCCreateRepositoryWorkspace()
			wksCommand.initLogger (this)
			wksCommand.setUrlRTC(urlRTC);
			wksCommand.setUserRTC(userRTC);
			wksCommand.setPwdRTC(pwdRTC);
			wksCommand.setScmToolsHome(scmToolsHome);
			wksCommand.setWorkspaceRTC(workspaceRTC);
			wksCommand.setStream(stream);
			wksCommand.execute();

			scmComponentsList = getComponentsRtc(stream);

		} else if(projectNature == "git") {
			scmComponentsList = getComponentsGit(gitGroup);

		}
		return scmComponentsList;
	}

	/**
	 * Devuelve la lista de componentes filtrada por el parámetro onlyChanges y el
	 * todos_o_ninguno si estos aplicasen.
	 * @param scmComponentsList
	 * @return finalComponentsList
	 */
	public List<String> getFinalComponentList(List<String> scmComponentsList, List<String> listaComponentesRelease) {
		List<String> finalComponentsList;

		if(projectNature.equals("rtc")) {
			finalComponentsList = getRTCFinalComponentList(scmComponentsList, listaComponentesRelease);

		} else if(projectNature.equals("git")) {
			finalComponentsList = getGitFinalComponentList(scmComponentsList, listaComponentesRelease);

		}
		return finalComponentsList;
	}

	/**
	 * Devuelve una lista de listas de jobs que han de lanzarse en paralelo según el
	 * tipo de scm donde estén los componentes.
	 * @param build Referencia a la ejecución en jenkins
	 * @param finalComponentsList
	 * @return sortedMavenCompoGroups
	 */
	public List<List<MavenComponent>> getOrderedList(AbstractBuild build, 
			List<String> finalComponentsList, List<String> scmComponentsList) {
		List<List<MavenComponent>> sortedMavenCompoGroups;
		if(projectNature.equals("rtc")) {
			sortedMavenCompoGroups = getRTCOrderedList(build, finalComponentsList, componentesRelease);

		} else if(projectNature.equals("git")) {
			sortedMavenCompoGroups = getGitOrderedList(finalComponentsList, scmComponentsList, componentesRelease);

		}
		
		List<MavenComponent> remainingComponents = getRemainingComponents(finalComponentsList, sortedMavenCompoGroups);
		
		if(remainingComponents != null && remainingComponents.size() > 0 && sortedMavenCompoGroups!= null) {
			sortedMavenCompoGroups.add(remainingComponents);
		}		
		
		return sortedMavenCompoGroups;
	}

	/**
	 * Según haya requerimiento de ordenación o no la lista "jobs"
	 * tendrá una sola lista con todos los jobs a lanzar en paralelo
	 * o varias listas con los grupos de jobs a lanzar en paralelo.
	 * @param getOrdered
	 * @param finalComponentsList
	 * @param sortedMavenCompoGroups
	 * @return
	 */
	public List<List<String>> getJobsList(List<String> finalComponentsList, List<List<MavenComponent>> sortedMavenCompoGroups) {
		def scmGroup;
		if(projectNature.equals("rtc")) {
			if(streamCargaInicial != null && !streamCargaInicial.trim().equals("")) {
				scmGroup = streamCargaInicial;
			} else {
				scmGroup = stream;
			}
		} else if(projectNature.equals("git")) {
			scmGroup = gitGroup;
		}
		List<List<String>> jobs = [];
		if(getOrdered.equals("false")) {
			List<String> thisJobList = [];
			finalComponentsList.each {
				def nombreJob = "${clean(scmGroup)} -COMP- ${clean(it)}";
				def job = Hudson.instance.getJob(nombreJob);
				if (job != null && !job.disabled) {
					thisJobList.add(nombreJob);
				} else {
					log("WARNING: El job \"${nombreJob}\" no está dado de alta en Jenkins o está desactivado.");
				}
			}
			jobs.add(thisJobList);
		} else if(getOrdered.equals("true")) {
			sortedMavenCompoGroups.each { List<MavenComponent> groupList ->
				def thisJobList = [];
				groupList.each { MavenComponent thisMavenComponent ->
					def nombreJob = "${clean(scmGroup)} -COMP- ${clean(thisMavenComponent.getName())}";
					def job = Hudson.instance.getJob(nombreJob);
					if (job != null && !job.disabled) {
						thisJobList.add(nombreJob);
					} else {
						log("WARNING: El job \"${nombreJob}\" no está dado de alta en Jenkins o está desactivado.");
					}
				}
				jobs.add(thisJobList);
			}
		}
		return jobs;
	}

	/**
	 * Obtención de nombres de componentes que cuelgan de una determinada
	 * corriente de RTC
	 * @param stream
	 * @return List<String> streamComponentsList;s
	 */
	private List<String> getComponentsRtc(String stream) {
		log("Se sacan los componentes de la stream \"${stream}\"")
		List<String> streamComponentsList = [];
		ScmCommand command = new ScmCommand(true, scmToolsHome, daemonsConfigDir);
		command.initLogger(this);
		try {
			log("Obteniendo componentes de " + stream)

			def listCommand = "list components \"${stream}\" -j";

			def streamComponentsJson = command.ejecutarComando(listCommand, userRTC, pwdRTC, urlRTC, new File(parentWorkspace));

			def streamComponentsJsonObject = new JsonSlurper().parseText(streamComponentsJson)
			streamComponentsJsonObject.workspaces[0].components.each {
				streamComponentsList.add(it.name);
			}
		} catch(Exception e) {
			e.printStackTrace();
			throw e;
		}
		finally {
			command.detenerDemonio(new File(parentWorkspace));
		}

		return streamComponentsList;
	}

	/**
	 * Obtención de nombres de componentes que cuelgan de un
	 * determinado grupo de git
	 * @param gitGroup
	 * @return List<String> streamComponentsList;s
	 */
	private List<String> getComponentsGit(String gitGroup) {
		List<String> gitGrouopComponentsList = [];

		File jenkinsComponentsFile = new File("${parentWorkspace}/jenkinsComponents.txt");
		jenkinsComponentsFile.eachLine { String line ->
			gitGrouopComponentsList.add(line);
		}

		// TODO: Por lo pronto no podemos invocar a GitLabClient desde un System Groovy Script. Así que recogemos
		// el artivo jenkinsComponents.txt que se dejó en el paso getComponentsFromGit.groovy por ahora.

		//		List<String> gitGrouopComponentsList = [];
		//		GitlabClient gitLabClient = new GitlabClient(urlGitlab, privateGitLabToken, keystoreVersion, urlNexus);
		//		gitLabClient.initLogger(this);
		//
		//		def entity = "groups/${gitGroup}";
		//		def jsonResponse = gitLabClient.get(entity, null);
		//
		//		def jsonSlurper = new JsonSlurper()
		//		def jsonObject = jsonSlurper.parseText(jsonResponse);
		//
		//		jsonObject.projects.each {
		//			gitGrouopComponentsList.add(it.name);
		//		}

		return gitGrouopComponentsList;
	}


	/**
	 * Analiza un json de conjuntos de cambios para determinar si hay o
	 * no cambios relevantes.
	 * @param component
	 * @return
	 */
	private boolean areThereChangesInRTC(def component) {
		boolean ret = false;
		if(component.added != null && component.added == true) {
			ret = true;
		}
		if(component.changesets != null) {
			component.changesets.each { def changeset ->
				// Para cada cambio, comprobar si el autor es distinto
				//	del usuario de RTC de referencia (JENKINS_RTC)
				if (!changeset.author.userName.equals(userRTC)) {
					ret = true;
				}
			}
		}
		if(component.baselines != null) {
			component.baselines.each { def baseline ->
				if(baseline.changesets != null) {
					baseline.changesets.each { def changeset ->
						// Para cada cambio, comprobar si el autor es distinto
						//	del usuario de RTC de referencia (JENKINS_RTC)
						if (!changeset.author.userName.equals(userRTC)) {
							ret = true;
						}
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Cálculo de la lista de componentes que han cambiado en función
	 * del parámetro "onlyChanges" para proyectos RTC.
	 * @param componentesRelease
	 * @param streamComponentsList
	 * @return List<String> finalComponentsList
	 */
	private List<String> getRTCFinalComponentList(List<String> streamComponentsList, List<String> listaComponentesRelease) {
		List<String> finalComponentsList = [];
		ScmCommand command = new ScmCommand(true, scmToolsHome, daemonsConfigDir);
		try {
			String ret = "";
			if (onlyChanges == "true" && (todos_o_ninguno == "false" || todos_o_ninguno == null || todos_o_ninguno == "null")) {
				// Si onlyChanges == true finalComponentsList se rellena sólo
				// con los componentes que tengan cambios de la Stream.
				log "Comparando cambios de workspace \"${workspaceRTC}\" contra stream \"${stream}\"";
				ret = command.ejecutarComando("compare workspace \"${workspaceRTC}\" stream \"${stream}\" -f i -I dcbsw -C \"|{name}|{email}|\" -D \"|yyyy-MM-dd-HH:mm:ss|\" -j ", userRTC, pwdRTC, urlRTC, new File(parentWorkspace));

				if (command.getLastErrorOutput().size() > 0){
					log "No se puede realizar la comparación contra el WSR: \"${workspaceRTC}\". Se procede a listar todos los componentes"
					finalComponentsList = streamComponentsList;
				}
				else {
					log "Json de cambios devuelto por RTC:"
					log ret +"\n"
					def retJson = new JsonSlurper().parseText(ret);
					retJson.direction[0].components.each { component ->
						if(areThereChangesInRTC(component)) {
							finalComponentsList.add(component.name);
						}
					}
				}

				if(listaComponentesRelease != null && listaComponentesRelease.size() > 0) {
					List<String> partialComponentesRelease = [];
					listaComponentesRelease.each { String componenteRelease ->
						String foundCompo = finalComponentsList.find { it.equals(componenteRelease) }
						if(foundCompo != null) {
							partialComponentesRelease.add(foundCompo);
						}
					}
					finalComponentsList = partialComponentesRelease;
				}

			} else if(onlyChanges == "true" && todos_o_ninguno == "true") {
				// Si todos_o_ninguno == "true" finalComponentsList se rellena con TODOS los
				// componentes de la stream si alguno ha cambiado o se deja vacío si ninguno ha cambiado.
				log "Comparando cambios de workspace \"${workspaceRTC}\" contra stream \"${stream}\"";
				ret = command.ejecutarComando("compare workspace \"${workspaceRTC}\" stream \"${stream}\" -f i -I dcbsw -C \"|{name}|{email}|\" -D \"|yyyy-MM-dd-HH:mm:ss|\" -j ", userRTC, pwdRTC, urlRTC, new File(parentWorkspace));
				if (command.getLastErrorOutput().size() > 0){
					log "No se puede realizar la comparación contra el WSR: \"${workspaceRTC}\". Se procede a listar todos los componentes"
					finalComponentsList = streamComponentsList;

				} else {
					log "Json de cambios devuelto por RTC:"
					log ret +"\n"
					def retJson = new JsonSlurper().parseText(ret);
					def chagesFlag = false;
					retJson.direction[0].components.each { component ->
						if(isComponentEnabled(component.name, stream)) {
							if(areThereChangesInRTC(component)) {
								chagesFlag = true;
							}
						}						
					}
					if(chagesFlag == true) {
						// Si ha habido cambios la lista final contiene todos los componentes.
						finalComponentsList = streamComponentsList;
					} else {
						finalComponentsList = []; // Si no ha habido cambios la lista final no contiene nada.
					}
				}

				if(listaComponentesRelease != null && listaComponentesRelease.size() > 0) {
					List<String> partialComponentesRelease = [];
					listaComponentesRelease.each { String componenteRelease ->
						String foundCompo = finalComponentsList.find { it.equals(componenteRelease) }
						if(foundCompo != null) {
							partialComponentesRelease.add(foundCompo);
						}
					}
					finalComponentsList = partialComponentesRelease;
				}

			} else if(onlyChanges == "false") {
				// Si onlyChanges == "false" se rellena finalComponentsList
				// directamente con todos los componentes de la stream.
				if(listaComponentesRelease != null && listaComponentesRelease.size() > 0) {
					finalComponentsList = listaComponentesRelease;
				} else {
					finalComponentsList = streamComponentsList;
				}
			}

		} catch(Exception e) {
			e.printStackTrace();
			throw e;
		}
		finally {
			command.detenerDemonio(new File(parentWorkspace));
		}

		// Descartamos todos los componentes que no tengan un job activo en Jenkins.
		def scmGroup;
		if(streamCargaInicial != null && !streamCargaInicial.trim().equals("")) {
			scmGroup = streamCargaInicial;
		} else {
			scmGroup = stream;
		}
		def definitiveFinalComponentsList = [];
		finalComponentsList.each { String compo ->
			def job = Hudson.instance.getJob("${scmGroup} -COMP- ${compo}");
			if(job != null && !job.disabled) {
				definitiveFinalComponentsList.add(compo);
			} else {
				log("[WARNING] El job \"${scmGroup} -COMP- ${compo}\" no tiene job activo en Jenkins. No se incluirá en finalComponentsList.");
			}
		}

		return definitiveFinalComponentsList;
	}

	/**
	 * Cálculo de la lista de componentes que han cambiado en función
	 * del parámetro "onlyChanges" para proyectos git.
	 * @param componentesRelease
	 * @param streamComponentsList
	 * @return List<String> finalComponentsList
	 */
	private List<String> getGitFinalComponentList(List<String> scmComponentsList, List<String> listaComponentesRelease) {
		List<String> thisFinalCompoList = [];

		Map<String,String> commitsIdMap = getStoredCommitsId(scmComponentsList, parentWorkspace);
		log("storedCommitsId -> ${commitsIdMap}");
		List<String> composToRemove = checkUpdatedComponents(scmComponentsList, parentWorkspace, branch, gitHost, gitGroup, commitsIdMap, action);

		// Creamos los nuevos archivos de commitsId en caso de action = build
		if(action.equals("build") || action.equals("deploy")) {
			if(listaComponentesRelease != null && listaComponentesRelease.size() > 0) {
				saveComponentLastCommit(listaComponentesRelease, branch, gitHost, gitGroup);
			} else {
				saveComponentLastCommit(scmComponentsList, branch, gitHost, gitGroup);
			}
			
		}

		if(onlyChanges.equals("true")) {
			scmComponentsList.each {
				if(!composToRemove.contains(it)) {
					thisFinalCompoList.add(it);
				}
			}
		} else {
			thisFinalCompoList = scmComponentsList;
		}

		if(listaComponentesRelease != null && listaComponentesRelease.size() > 0) {
			List<String> partialComponentesRelease = [];
			listaComponentesRelease.each { String componenteRelease ->
				String foundCompo = thisFinalCompoList.find { it.equals(componenteRelease) }
				if(foundCompo != null) {
					partialComponentesRelease.add(foundCompo);
				}
			}
			thisFinalCompoList = partialComponentesRelease;
		}

		def finalComponentsList = [];
		thisFinalCompoList.each { String compo ->
			def job = Hudson.instance.getJob("${gitGroup} -COMP- ${compo}");
			if(job != null && !job.disabled) {
				finalComponentsList.add(compo);
			} else {
				log("[WARNING] El job \"${gitGroup} -COMP- ${compo}\" no tiene job activo en Jenkins. No se incluirá en finalComponentsList ");
			}
		}

		return finalComponentsList;
	}

	/**
	 * Guarda la info del último commit de cada componente
	 * @param jenkinsComponents
	 */
	private void saveComponentLastCommit(List<String> componentsArray, String branch, String gitHost, String gitGroup) {
		componentsArray.each { String component ->
			TmpDir.tmp { File dir ->
				GitCloneCommand cc = new GitCloneCommand();
				GitLogCommand lg = new GitLogCommand();
				try {
					cc.setParentWorkspace(new File(dir.getAbsolutePath()));
					cc.setGitBranch(branch);
					cc.setGitHost(gitHost);
					cc.setGitPath("${gitGroup}/${component}.git");
					cc.setGitUser("git");
					cc.setAdditionalParams("--no-checkout");
					cc.setLocalFolderName(component);
					cc.setGitCommand(this.gitCommand);
					cc.execute();

					
					lg.setResultsNumber("1");
					lg.setParentWorkspace("${dir.getAbsolutePath()}/${component}");
					lg.setGitCommand(this.gitCommand);
					def commitLog = lg.execute();
					def id = "";
					commitLog.eachLine { String line ->
						if(line.startsWith("commit")) {
							id = line.split(" ")[1];
						}
					}

					def commitLogFile = new File("${parentWorkspace}/${component}_lastCommit.txt");
					commitLogFile.text = id;
				}
				catch (Exception e) {
					if (cc.getLastReturnCode() == 128 || lg.getLastReturnCode() == 128) {
						// Repositorio no inicializado
						log ("WARNING: repositorio $component no inicializado")
					}
					else {
						throw e;
					}
				}
			}
		}
	}

	/**
	 * Obtiene los ids de los commits almacenados en el workspace para cada componente.
	 * @param componentsArray
	 * @param parentWorkspace
	 * @return String commitsId
	 */
	private Map<String,String> getStoredCommitsId(componentsArray, parentWorkspace) {		
		def commitsIdMap = [:];
		componentsArray.each { String component ->
			def lastCommitFile = new File("${parentWorkspace}/${component}_lastCommit.txt");
			if(lastCommitFile.exists()) {
				commitsIdMap.put("${component}".toString(), "${lastCommitFile.text}".toString());
			}
		}		
		return commitsIdMap;
	}

	/**
	 * Comprueba qué componentes están actualizados.
	 * @param jenkinsComponents
	 * @return componentsToRemove
	 */
	private List<String> checkUpdatedComponents(List<String> componentsArray, String parentWorkspace,
			String branch, String gitHost, String gitGroup, Map<String,String> commitsIdMap, String action) {

		def componentsToRemove = [];

		// En caso de action = build or deploy se comprueba el commitId guardado.
		if(action.equals("build") || action.equals("deploy")) {			
			componentsArray.each { String component ->
				log("### Comprobando cambios en componente \"${component}\"")
				// Comprobamos lastUser
				def lastUser = getLastUser(component, branch, gitHost, gitGroup);
				if (lastUser != null) {
					if(lastUser.equals(lastUserIC)) {
						if(!componentsToRemove.contains(component)) {
							componentsToRemove.add(component);
						}
					}
				} else {
					// si el lastUser es nulo, hubo un problema al consultar el repo
					// posiblemente no está inicializado.
					if(!componentsToRemove.contains(component)) {
						componentsToRemove.add(component);
					}
				}

				// Comprobamos el LastCommitId
				def lastCommitId = getLastCommitId(component, branch, gitHost, gitGroup);
				log("lastCommitId -> ${lastCommitId}");
				if (lastCommitId != null) {					
					log("Sacando storedCommitId para el componente \"${component}\"... (del mapa ${commitsIdMap})")
					String storedCommitId = commitsIdMap["${component}".toString()];
					log("storedCommitId -> ${storedCommitId}")
					storedCommitId = storedCommitId==null?"":storedCommitId
					if(storedCommitId.trim().equals(lastCommitId)) {
						if(!componentsToRemove.contains(component)) {
							componentsToRemove.add(component);
						}
					}
				} else {
					// si el lastCommitId es nulo, hubo un problema al consultar el repo
					// posiblemente no está inicializado.
					if(!componentsToRemove.contains(component)) {
						componentsToRemove.add(component);
					}
				}
			}
		}
		// si action != build/deploy se comprueba el último usuario que ha modificado cada componente en el branch.
		else {
			componentsArray.each { String component ->
				def lastUser = getLastUser(component, branch, gitHost, gitGroup);
				if (lastUser != null) {
					if(lastUser.equals(lastUserIC)) {
						if(!componentsToRemove.contains(component)) {
							componentsToRemove.add(component);
						}
					}
				} else {
					// si el lastUser es nulo, hubo un problema al consultar el repo
					// posiblemente no está inicializado.
					if(!componentsToRemove.contains(component)) {
						componentsToRemove.add(component);
					}
				}
			}
		}

		return componentsToRemove;
	}

	/**
	 * Devuelve el último commitId de un componente en Git
	 * @param (String) component
	 * @return String lastCommitId
	 */
	private String getLastCommitId(String component, String branch, String gitHost, String gitGroup) {
		String lastCommitId = null;
		TmpDir.tmp { File dir ->
			GitCloneCommand cc = new GitCloneCommand();
			GitLogCommand lg = new GitLogCommand();
			cc.initLogger(this)
			try {
				cc.setParentWorkspace(new File(dir.getAbsolutePath()));
				cc.setGitBranch(branch);
				cc.setGitHost(gitHost);
				cc.setGitPath("${gitGroup}/${component}.git");
				cc.setGitUser("git");
				cc.setAdditionalParams("--no-checkout");
				cc.setLocalFolderName(component);
				cc.setGitCommand(this.gitCommand);
				cc.execute();
				
				lg.initLogger(this)
				lg.setResultsNumber("1");
				lg.setParentWorkspace("${dir.getAbsolutePath()}/${component}");
				lg.setGitCommand(this.gitCommand);
				String commitLog = lg.execute();

				commitLog.eachLine { String line ->
					if(line.trim().startsWith("commit")) {
						lastCommitId = line.split(" ")[1]
					}
				}
			}
			catch (Exception e) {
				if (cc.getLastReturnCode() == 128 || lg.getLastReturnCode() == 128) {
					// Repositorio no inicializado
					log ("WARNING: repositorio $component no inicializado")
				}
				else {
					throw e;
				}
			}
		}
		return lastCommitId;
	}

	/**
	 * Devuelve una lista de listas de componentes que han de
	 * lanzarse en paralelo por parte del Triggers
	 * @param finalComponentsList
	 * @param scmComponentsList
	 * @param componentesRelease
	 * @return List<List<MavenComponent>> sortedMavenCompoGroups
	 */
	private List<List<MavenComponent>> getGitOrderedList(List<String> finalComponentsList, List<String> scmComponentsList, String componentesRelease) {
		List<List<MavenComponent>> sortedMavenCompoGroups = [];
		File baseDir = new File(parentWorkspace);
		// Ordenamos los componentes si así se indica.
		if(getOrdered == "true") {
			log("Ordenando los componentes. Este proceso puede llevar unos minutos...")
			TmpDir.tmp { File tmpDir ->
				GitBuildFileHelper gitBuildFileHelper = new GitBuildFileHelper(action, baseDir);
				gitBuildFileHelper.initLogger(this);

				scmComponentsList.each {
					log "Se crea la estructura local de poms para el componente ${it}";
					gitBuildFileHelper.createBuildFileStructure(baseDir, it,	technology,	gitHost, gitGroup, branch, this.gitCommand);
				}

				List <MavenComponent> reactor = gitBuildFileHelper.createStreamReactor(baseDir, scmComponentsList, finalComponentsList);
				def ls = System.getProperty("line.separator");

				// "CheckOpenVersionsAndDeps"
				// Se comprueba que las versiones y que los pom.xml tienen las dependencias
				// bien cuadradas con lo que se necesita y que las versiones.
				if(action.equals("release")) {
					log("[checkOpenVersionAndDeps] Comprobamos que las dependencias de los poms están en orden...");
					checkOpenVersionAndDeps(baseDir, finalComponentsList);
				} else {
					log("[checkOpenVersionAndDeps] Action = ${action}. No comprobamos las dependencias de los pom.")
				}

				// En este punto debemos añadir los componentes arrastrados por dependencias.
				// Si un componente no ha cambiado pero su dependencia sí, ha de construirse también.
				def componentsMavenArray = [];
				finalComponentsList.each { String compoFromArray ->
					MavenComponent tmp = reactor.find { it.getName().equals(compoFromArray) }
					if(tmp != null) {
						componentsMavenArray.add(tmp);
					}
				}

				// Añadimos los componentes arrastrados por dependencias. Es decir, si uno de
				// los componentes que no se van a construir depende de uno de los que sí se van
				// a construir ha de construirse también.
				if(arrastrarDependencias.equals("true")) {
					log("Se calculan los componentes arrastrados por dependencias...");
					def componentesArrastrados = [];
					reactor.each { MavenComponent mavenComponent ->
						finalComponentsList.each { String component ->
							MavenComponent thisMavenComponent = reactor.find { it.getName().equals(component) };
							if(MavenComponent.dependsOn(mavenComponent, thisMavenComponent)) {
								if(!finalComponentsList.contains(mavenComponent.getName())) {
									componentesArrastrados.add(mavenComponent);
								}
							}
						}
					}
					componentsMavenArray.addAll(componentesArrastrados);
				}

				// Se ordena ahora componentsMavenArray
				def orderedComponents = [];
				reactor.each { MavenComponent mavenComponent ->
					MavenComponent tmp = componentsMavenArray.find { it.getName().equals(mavenComponent.getName()) }
					if(tmp != null) {
						orderedComponents.add(tmp);
					}
				}

				log("finalMavenComponentListOrdered ->")
				int ind = 0;
				orderedComponents.each {
					ind++;
					log(ind + ": " + it.getName());
				}

				// Obtenemos una lista de listas de MavenComponents con los MavenComponents
				// agrupados según los que se puedan construir en paralelo.
				sortedMavenCompoGroups = new SortGroupsStrategy().sortGroups(orderedComponents);
			}
		}

		return sortedMavenCompoGroups;
	}

	/**
	 * Devuelve una lista de listas de componentes que se 
	 * pueden construir en paralelo para proyectos RTC.
	 * @param build Referencia a la ejecución en jenkins
	 * @param finalComponentsList
	 * @param componentesRelease
	 * @return List<List<String>> sortedMavenCompoGroups
	 */
	private List<List<MavenComponent>> getRTCOrderedList(AbstractBuild build, 
			List<String> finalComponentsList, String componentesRelease) {
		List<List<String>> sortedMavenCompoGroups;
		File baseDir = new File(build.workspace.toString());
		RTCBuildFileHelper helper = new RTCBuildFileHelper(action, baseDir);
		if(getOrdered.equals("true") && finalComponentsList.size() > 0) {
			log("Ordenando los componentes. Esto puede llevar unos minutos...")
			// Contruimos el createStreamReactor para saber qué componentes dependen de los que han
			// cambiado para incluirlos también en la lista de cambios.
			//TmpDir.tmp { File tmp ->
			RTCBuildFileHelper helperReactor = new RTCBuildFileHelper(action, baseDir);
			helperReactor.initLogger(this);
			// Crea el reactor y el artifacts.json al vuelo.
			// El reactor será una lista de todos los MavenComponents ya ordenados del proyecto.
			// El artifactsJson contendrá sólo los componentes que entren en la release.
			List <MavenComponent> reactor = helperReactor.createStreamReactor(
					baseDir,
					stream,
					"maven",
					userRTC,
					pwdRTC,
					urlRTC,
					null,
					finalComponentsList);

			// "CheckOpenVersionsAndDeps"
			// Se comprueba que las versiones y que los pom.xml tienen las dependencias
			// bien cuadradas con lo que se necesita y que las versiones.
			if(action.equals("release")) {
				log("[checkOpenVersionAndDeps] Comprobamos que las dependencias de los poms están en orden...");
				checkOpenVersionAndDeps(baseDir, finalComponentsList);
			} else {
				log("[checkOpenVersionAndDeps] Action = ${action}. No comprobamos las dependencias de los pom.")
			}

			log("#### REACTOR ####")
			reactor.each {
				log it.getName()
			}
			log("#################")

			def finalMavenComponentList = []; // La "finalComponentsList" pero con MavenComponents en lugar de con Strings.
			finalComponentsList.each { compo ->
				MavenComponent temp = reactor.find { mavenCompo -> mavenCompo.getName().equals(compo) }
				if(temp != null) {
					finalMavenComponentList.add(temp);
				}
			}

			// Añadimos los componentes arrastrados por dependencias. Es decir, si uno de
			// los componentes que no se van a construir depende de uno de los que sí se van
			// a construir ha de construirse también.
			if(arrastrarDependencias.equals("true")) {
				log("Se calculan los componentes arrastrados por dependencias...")
				def componentesArrastrados = [];
				reactor.each { MavenComponent mavenComponent ->
					finalComponentsList.each { String component ->
						MavenComponent thisMavenComponent = reactor.find { it.getName().equals(component) };
						if(MavenComponent.dependsOn(mavenComponent, thisMavenComponent)) {
							if(!finalComponentsList.contains(mavenComponent.getName())) {
								componentesArrastrados.add(mavenComponent);
							}
						}
					}
				}
				finalMavenComponentList.addAll(componentesArrastrados);
			}

			def finalMavenComponentListOrdered = [];
			// Ahora tenemos mezclados los componentes finales con los componentes arrastrados.
			// Ordenamos esta lista final fijándonos en el reactor.
			reactor.each { MavenComponent reactorComponent ->
				MavenComponent thisMavenComponent = finalMavenComponentList.find { it.getName().equals(reactorComponent.getName())}
				if(thisMavenComponent != null) {
					finalMavenComponentListOrdered.add(thisMavenComponent);
				}
			}

			log("finalMavenComponentListOrdered ->")
			int ind = 0;
			finalMavenComponentListOrdered.each {
				ind ++;
				log(ind + ": " + it.getName());
			}
			// Obtenemos una lista de listas de MavenComponents con los MavenComponents
			// agrupados según los que se puedan construir en paralelo.
			sortedMavenCompoGroups = new SortGroupsStrategy().sortGroups(finalMavenComponentListOrdered);
			//}
		}

		return sortedMavenCompoGroups;
	}

	/**
	 * Quita espacios en blanco alrededor de una cadena
	 * y sustituye "/" por "-".
	 * @param cadena
	 * @return cadena
	 */
	private String clean(cadena){
		cadena = cadena.replaceAll("/","-")
		return cadena.trim();
	}

	/**
	 * Devuelve un String con los componentesUrbancode de cada job (si este está definido)
	 * @param jobs
	 * @return String componentsUrban
	 */
	def getComponentsUrban(List<String> scmComponentsList) {
		def scmGroup;
		if(projectNature.equals("rtc")) {
			if(streamCargaInicial != null && !streamCargaInicial.trim().equals("")) {
				scmGroup = streamCargaInicial;
			} else {
				scmGroup = stream;
			}
		} else if(projectNature.equals("git")) {
			scmGroup = gitGroup;
		}
		def componentsUrban = "";
		if(scmComponentsList != null) {
			scmComponentsList.each { String thisCompo ->
				def thisJob = "${scmGroup} -COMP- ${thisCompo}";
				def job = hudson.model.Hudson.instance.getJob(thisJob);
				def componentUrban;
				if (job != null && !job.disabled) {
					def jobParameters = getJobParameters(thisJob);
					if(jobParameters.contains("componenteUrbanCode")) {
						if(job != null) {
							job.getProperties().values().each { value ->
								if(value instanceof hudson.model.ParametersDefinitionProperty) {
									def paramValue = value.getParameterDefinition("componenteUrbanCode").getDefaultParameterValue().getValue();
									if(!paramValue.trim().equals("")) {
										componentUrban = "${thisCompo}:" + paramValue
									} else {
										componentUrban = "${thisCompo}:NULL";
									}
								}
							}
						}
					} else {
						componentUrban = "${thisCompo}:NULL";
					}
					componentsUrban = componentsUrban + componentUrban +","
				} else {
					componentUrban = "${thisCompo}:NULL";
				}
			}
		}
		return removeLastComma(componentsUrban);
	}

	/**
	 * Devuelve los parámetros de un job formado como "${gitGroup} -COMP- ${component}";
	 * @param gitGroup
	 * @param component
	 * @return jobParameters
	 */
	def getJobParameters(jobName) {
		def job = hudson.model.Hudson.instance.getJob(jobName);
		def jobParameters = [];
		if(job != null) {
			job.getProperties().values().each { value ->
				if(value instanceof hudson.model.ParametersDefinitionProperty) {
					jobParameters = value.getParameterDefinitionNames();
				}
			}
		} else {
			log("[WARNING] El job ${jobName} no existe en Jenkins");
		}

		return jobParameters;
	}


	/**
	 * Elimina el último carácter de un String si éste
	 * es una coma.
	 * Lo usamos porque la lista de jobs vendrá
	 * con una coma al final que hay que eliminar.
	 * @param (String)text
	 * @return (String)result
	 */
	private String removeLastComma(String text) {
		def result;
		if(text.endsWith(",")) {
			result = text.substring(0, text.length() - 1);
		} else {
			result = text;
		}
		return result;
	}

	/**
	 * Añade un nuevo set de parámetros a la build de Jenkins
	 * @param build
	 * @param params
	 */
	public void setParams(build, params) {
		def paramsIn = build?.actions.find{ it instanceof ParametersAction }?.parameters
		def index = build?.actions.findIndexOf{ it instanceof ParametersAction }
		def paramsTmp = []
		if (paramsIn!=null) {
			//No se borra nada para compatibilidad hacia atrás.
			paramsTmp.addAll(paramsIn)
			//Borra de la lista los paramaterAction
			build?.actions.remove(index)
		}
		paramsTmp.addAll(params)

		build?.actions.add(new ParametersAction(paramsTmp))
	}

	/**
	 * Devuelve el último usuario que ha hecho commit en un componente
	 * @param component
	 * @param branch
	 * @param gitHost
	 * @param gitGroup
	 * @return String lastUser
	 */
	private String getLastUser(String component, String branch, String gitHost, String gitGroup) {
		String lastUser = null;
		TmpDir.tmp { File dir ->
			GitCloneCommand cc = new GitCloneCommand();
			GitLogCommand lg = new GitLogCommand();
			try {
				cc.setParentWorkspace(new File(dir.getAbsolutePath()));
				cc.setGitBranch(branch);
				cc.setGitHost(gitHost);
				cc.setGitPath("${gitGroup}/${component}.git");
				cc.setGitUser("git");
				cc.setEmpty("true");
				cc.setLocalFolderName(component);
				cc.setGitCommand(this.gitCommand);
				cc.execute();
				
				lg.setResultsNumber("1");
				lg.setParentWorkspace("${dir.getAbsolutePath()}/${component}");
				lg.setGitCommand(this.gitCommand);
				String commitLog = lg.execute();

				commitLog.eachLine { String line ->
					if(line.trim().startsWith("Author")) {
						lastUser = line.split(" ")[1]
					}
				}
			}
			catch (Exception e) {
				if (cc.getLastReturnCode() == 128 || lg.getLastReturnCode() == 128) {
					// Repositorio no inicializado
					log ("WARNING: repositorio $component no inicializado")
				}
				else {
					throw e;
				}
			}
		}
		return lastUser;
	}

	/**
	 * Obtiene una lista de sublistas con un job por
	 * cada sublista para forzar un lanzamiento secuencial
	 * por parte del Trigger en Jenkins.
	 * @param jobs
	 * @return List<List<String>> jobs
	 */
	public getSequentialJobs(List<List<String>> jobs) {
		List<List<String>> ret = [];
		jobs.each { List<String> sublista ->
			sublista.each { String job ->
				List<String> tempList = [];
				tempList.add(job);
				ret.add(tempList);
			}
		}
		return ret;
	}

	/**
	 * Se comprueba que las versiones y que los pom.xml tienen las dependencias
	 * bien cuadradas con lo que se necesita y que las versiones.
	 * @param baseDir
	 * @param finalComponentsList
	 * @return
	 */
	public void checkOpenVersionAndDeps(File baseDir, List<String> finalComponentsList) {
		String artifactsJson = new File(baseDir,"artifacts.json").getText();
		finalComponentsList.each { String componentName ->
			File componentDir = new File(baseDir,componentName);
			PomXmlOperations.checkOpenVersionAndDeps(componentDir, artifactsJson, urlNexus);
		}
	}
	
	/**
	 * Calcula la lista de componentes ordenada junto con la lista de componentes que se ha quedado fuera de la
	 * ordenación por no tener pom.xml.
	 * @param finalComponentsList
	 * @param sortedMavenCompoGroups
	 * @return 
	 */
	public List<MavenComponent> getRemainingComponents(List<String> finalComponentsList, List<List<MavenComponent>> sortedMavenCompoGroups) {
		// Mediante remainingComponents calculamos los componentes que tengan jobs activos que
		// se hayan quedado fuera por no cumplir los requisitos de ordenación (no tener pom.xml).
		def scmGroup;
		if(projectNature.equals("rtc")) {
			if(streamCargaInicial != null && !streamCargaInicial.trim().equals("")) {
				scmGroup = streamCargaInicial;
			} else {
				scmGroup = stream;
			}
		} else if(projectNature.equals("git")) {
			scmGroup = gitGroup;
		}
		List<MavenComponent> remainingComponents = [];
		finalComponentsList.each { String finalCompo ->
			boolean contained = false;
			sortedMavenCompoGroups.each { List<MavenComponent> compoGroup ->
				compoGroup.each { MavenComponent compo ->
					if(finalCompo.equals(compo.getName())) {
						contained = true;
					}
				}
			}
			if(contained == false) {
				// Comprobar que tiene job activo en Jenkins.
				def nombreJob = "${scmGroup} -COMP- ${finalCompo}";
				def job = Hudson.instance.getJob(nombreJob);
				if (job != null && !job.disabled) {
					remainingComponents.add(new MavenComponent(finalCompo));
				} else {
					log("WARNING: El job \"${nombreJob}\" no está dado de alta en Jenkins o está desactivado.");
				}

			}
		}
		
		return remainingComponents;
	}
	
	private Boolean isComponentEnabled(String component, String stream) {
		Boolean isEnabled = false;
		def nombreJob = "${stream} -COMP- ${component}";
		def job = Hudson.instance.getJob(nombreJob);
		if (job != null && !job.disabled) {
			isEnabled = true;
		}
		return isEnabled;
	}
}
