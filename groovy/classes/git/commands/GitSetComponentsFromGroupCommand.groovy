package git.commands;

import java.io.File;
import java.util.List;
import components.MavenComponent
import es.eci.utils.GitBuildFileHelper;
import es.eci.utils.base.Loggable;
import es.eci.utils.TmpDir;
import git.GitlabClient;
import groovy.json.*;
import groovy.util.ConfigSlurper;
import es.eci.utils.TmpDir;
import git.commands.GitCloneCommand;
import git.commands.GitLogCommand;
import es.eci.utils.pom.SortGroupsStrategy;

class GitSetComponentsFromGroupCommand extends Loggable {

	// build/release/addFix/addHotfix/deploy
	String action;
	// Nombre de la rama en git
	String branch;
	String commitsId;
	String components;
	// true/false
	String getOrdered;
	// Nombre del grupo en git
	String gitGroup;
	// Servidor git
	String gitHost;
	// Versión del keystore para contactar con el servidor git
	String keystoreVersion;
	// Directorio de ejecución
	String parentWorkspace;
	String privateGitLabToken;
	String technology;
	// URL de gitlab a la que atacar con servicios REST
	String urlGitlab;
	// URL de nexus para bajar el keystore
	String urlNexus;
	String lastUserIC;
	// Ruta del comando local que permite lanzar git
	String gitCommand;
	// Home local de maven
	String mavenHome;
	// true/false
	String onlyChanges;

	def tecnologias = ["maven":"pom\\.xml","gradle":"build\\.gradle"];

	public void execute() {

		branch = getProperBranchName(branch);

		// Se sacan los componentes que componen un grupo de gitLab mediante su API
		GitlabClient gitLabClient = new GitlabClient(urlGitlab, privateGitLabToken, keystoreVersion, urlNexus);
		gitLabClient.initLogger(this);

		def entity = "groups/${gitGroup}";
		def jsonResponse = gitLabClient.get(entity, null);

		def jsonSlurper = new JsonSlurper()
		def jsonObject = jsonSlurper.parseText(jsonResponse);
		
		def jenkinsComponents = new File("${parentWorkspace}/jenkinsComponents.txt");
		def jenkinsComponentsJobs = new File("${parentWorkspace}/jenkinsComponentsJobs.txt");
				
		jenkinsComponents.text = "";
		def ls = System.getProperty("line.separator");
		def compoObjects = jsonObject.projects;
		for(int i = 0; i < compoObjects.size(); i++) {
			if(i != (compoObjects.size() -1)) {
				jenkinsComponents.append(compoObjects[i].name + ls)
			} else {
				jenkinsComponents.append(compoObjects[i].name)
			}
		}
		
		jenkinsComponentsJobs.text = ""; // Lo vacíamos por si queda alguno de construcciones anteriores.		
		
		
		
		def componentsArray = [];
		def groupComponents = [];
		jsonObject.projects.each {			
			groupComponents.add(it.name);			
		}
		
		log "Componentes asociados al grupo ${gitGroup}:"
		if(components == null || components.trim().equals("")) {
			jsonObject.projects.each {
				log("Añadiendo ${it.name} a componentsArray")
				componentsArray.add(it.name);
			}
		} else {
			def componentsList = components.split(",");
			componentsList.each {
				log("Añadiendo ${it} a componentsArray")
				componentsArray.add(it);
			}
		}
		if(componentsArray.size() == 0) {
			throw new Exception("No hay jobs que lanzar. O bien los componentes indicados no existen o bien no hay componentes en el grupo ${gitGroup}");
		}

		commitsId = getStoredCommitsId(componentsArray,parentWorkspace);

		// Lista de componentes que ya están actualizados.
		def composToRemove = checkUpdatedComponents(componentsArray, parentWorkspace, branch, gitHost, gitGroup, commitsId, action);

		// Sólo eliminamos componentes de la lista si se nos ha pedido que no se construyan los que están actualizados.
		if(onlyChanges == "true") {
			componentsArray.removeAll(composToRemove);
		}
		
		// Creamos los nuevos archivos de commitsId en caso de action = build
		if(action.equals("build")) {
			saveComponentLastCommit(componentsArray, branch, gitHost, gitGroup);
		}
		
		List<List<MavenComponent>> sortedMavenCompoGroups;
		// Ordenamos los componentes si así se indica.
		if(getOrdered == "true") {
			log("Se ordenan los componentes:")
			GitBuildFileHelper gitBuildFileHelper = new GitBuildFileHelper(action,new File(parentWorkspace));
			gitBuildFileHelper.initLogger(this);

			componentsArray.each {
				log "Se crea la estructura local de poms para el componente ${it}";
				gitBuildFileHelper.createBuildFileStructure(new File(parentWorkspace), it,	technology,	gitHost, gitGroup, branch, this.gitCommand);
			}
						
			List <MavenComponent> reactor = gitBuildFileHelper.createStreamReactor(new File(parentWorkspace), groupComponents);
							
			// En este punto debemos añadir los componentes arrastrados por dependencias.
			// Si un componente no ha cambiado pero su dependencia sí, ha de construirse también.
			def componentsMavenArray = [];
			componentsArray.each { String compoFromArray ->
				MavenComponent tmp = reactor.find { it.getName().equals(compoFromArray) }
				if(tmp != null) {
					componentsMavenArray.add(tmp);
				}
			}
			
			def componentesArrastrados = [];
			reactor.each { MavenComponent mavenComponent ->
				componentsArray.each { String component ->
					MavenComponent thisMavenComponent = reactor.find { it.getName().equals(component) };
					if(MavenComponent.dependsOn(mavenComponent, thisMavenComponent)) {
						if(!componentsArray.contains(mavenComponent.getName())) {
							componentesArrastrados.add(mavenComponent);
						}
					}
				}
			}			
			componentsMavenArray.addAll(componentesArrastrados);
			
			// Se ordena ahora componentsMavenArray
			def orderedComponents = [];
			reactor.each { MavenComponent mavenComponent ->
				MavenComponent tmp = componentsMavenArray.find { it.getName().equals(mavenComponent.getName()) }
				if(tmp != null) {
					orderedComponents.add(tmp);
				}
			} 
						
			// Obtenemos una lista de listas de MavenComponents con los MavenComponents
			// agrupados según los que se puedan construir en paralelo.
			sortedMavenCompoGroups = new SortGroupsStrategy().sortGroups(orderedComponents);
		}
		
		// Según haya requerimiento de ordenación o no la lista "jobs" puede ser
		// una lista simple de componentes o una lista de listas de componentes agrupadas
		// según se puedan lanzar en paralelo.		
		List<List<String>> jobs = [];
		if(getOrdered.equals("false")) {
			List<String> thisJobList = [];
			componentsArray.each {
				def nombreJob = "${gitGroup} -COMP- ${it}";				
				thisJobList.add(nombreJob);				
			}
			jobs.add(thisJobList);
		} else if(getOrdered.equals("true")) {
			sortedMavenCompoGroups.each { List<MavenComponent> groupList ->
				def thisJobList = [];
				groupList.each { MavenComponent thisMavenComponent ->
					def nombreJob = "${gitGroup} -COMP- ${thisMavenComponent.getName()}";					
					thisJobList.add(nombreJob);					
				}
				jobs.add(thisJobList);
			}			
		}	
		jenkinsComponentsJobs.text = JsonOutput.toJson(jobs);
	}


	/**
	 * Obtiene los ids de los commits almacenados en el workspace para cada componente.
	 * @return commitsId
	 */
	private String getStoredCommitsId(componentsArray, parentWorkspace) {
		def commitsId ="";
		componentsArray.each { String component ->
			def lastCommitFile = new File("${parentWorkspace}/${component}_lastCommit.txt");
			if(lastCommitFile.exists()) {
				commitsId = commitsId + "${component}:${lastCommitFile.text},"
			}
		}
		return removeLastComma(commitsId);
	}

	/**
	 * Elimina el último carácter de un String.
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
	 * Comprueba qué componentes están actualizados.
	 * @param jenkinsComponents
	 */
	private List<String> checkUpdatedComponents(List<String> componentsArray, String parentWorkspace,
			String branch, String gitHost, String gitGroup, String commitsId, String action) {
		log("Se comprueban qué componentes están actualizados ya");
		def componentsToRemove = [];

		// En caso de action = build se comprueba el commitId guardado.
		if(action.equals("build")) {
			def commitsIdArray = commitsId.split(",");
			componentsArray.each { String component ->
				def lastCommitId = getLastCommitId(component, branch, gitHost, gitGroup);
				if (lastCommitId != null) {
					def componentIdPair = commitsIdArray.find { it.startsWith(component) };
					String storedCommitId = (componentIdPair != null)? componentIdPair.split(":")[1] : "";
	
					log("Comparamos el último commit de ${component}:");
					log("${storedCommitId} <-> ${lastCommitId}")
					if(storedCommitId.trim().equals(lastCommitId)) {
						log("Eliminamos el componente ${component} de la lista de componentes a contruir.");
						componentsToRemove.add(component);
					}
				}

			}
			log("Componentes actualizados a eliminar de la lista:")
			componentsToRemove.each {
				log it
			}
		}
		// si action != build se comprueba el último usuario que ha modificado cada componente en el branch.
		else {
			componentsArray.each { String component ->
				def lastUser = getLastUser(component, branch, gitHost, gitGroup);
				if (lastUser != null) {
					log("Comparamos el usuario ${lastUserIC} con ${lastUser}");
					if(lastUser.equals(lastUserIC)) {
						componentsToRemove.add(component);
					}
				}
			}
		}
		log("Componentes que no se van a construir por estar al día:")
		componentsToRemove.each {
			log(it)
		}
		return componentsToRemove;
	}

	/**
	 * Devuelve el último usuario que ha hecho commit en un componente
	 * @param component
	 * @param branch
	 * @param gitHost
	 * @param gitGroup
	 * @return
	 */
	private String getLastUser(String component, String branch, String gitHost, String gitGroup) {
		String lastUser = null;
		TmpDir.tmp { File dir ->
			GitCloneCommand cc = new GitCloneCommand();
			cc.initLogger(this)
			GitLogCommand lg = new GitLogCommand();
			lg.initLogger(this)
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
				if (cc.getLastReturnCode() == 128
						|| lg.getLastReturnCode() == 128) {
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
	 * Devuelve el último commitId de un componente en Git
	 * @param (String) component
	 * @return
	 */
	private String getLastCommitId(String component, String branch, String gitHost, String gitGroup) {
		String lastCommitId = null;
		TmpDir.tmp { File dir ->
			GitCloneCommand cc = new GitCloneCommand();
			cc.initLogger(this)
			GitLogCommand lg = new GitLogCommand();
			lg.initLogger(this)
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
				String commitLog = lg.execute();
	
				commitLog.eachLine { String line ->
					if(line.trim().startsWith("commit")) {
						lastCommitId = line.split(" ")[1]
					}
				}
			}
			catch (Exception e) {
				if (cc.getLastReturnCode() == 128
					|| lg.getLastReturnCode() == 128) {
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
	 * Guarda la info del último commit de cada componente
	 * @param jenkinsComponents
	 */
	private void saveComponentLastCommit(List<String> componentsArray, String branch, String gitHost, String gitGroup) {
		componentsArray.each { String component ->
			TmpDir.tmp { File dir ->
				GitCloneCommand cc = new GitCloneCommand();
				cc.initLogger(this)
				GitLogCommand lg = new GitLogCommand();
				lg.initLogger(this)
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
					if (cc.getLastReturnCode() == 128
						 || lg.getLastReturnCode() == 128) {
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

	private String getProperBranchName(String branch) {
		def result = branch;
		if(branch == null) {
			result = "DESARROLLO";
		}
		if(branch != null) {
			if(branch.trim().equals("")) {
				result = "DESARROLLO";
			}
		}
		return result;
	}

	// Getters and Setters
	public String getPrivateGitLabToken() {
		return privateGitLabToken;
	}

	public void setPrivateGitLabToken(String privateGitLabToken) {
		this.privateGitLabToken = privateGitLabToken;
	}

	public String getUrlGitlab() {
		return urlGitlab;
	}

	public void setUrlGitlab(String urlGitlab) {
		this.urlGitlab = urlGitlab;
	}

	public String getKeystoreVersion() {
		return keystoreVersion;
	}

	public void setKeystoreVersion(String keystoreVersion) {
		this.keystoreVersion = keystoreVersion;
	}

	public String getUrlNexus() {
		return urlNexus;
	}

	public void setUrlNexus(String urlNexus) {
		this.urlNexus = urlNexus;
	}

	public String getGitGroup() {
		return gitGroup;
	}

	public void setGitGroup(String gitGroup) {
		this.gitGroup = gitGroup;
	}

	public String getParentWorkspace() {
		return parentWorkspace;
	}

	public void setParentWorkspace(String parentWorkspace) {
		this.parentWorkspace = parentWorkspace;
	}

	public String getComponents() {
		return components;
	}

	public void setComponents(String components) {
		this.components = components;
	}

	public String getTechnology() {
		return technology;
	}

	public void setTechnology(String technology) {
		this.technology = technology;
	}

	public String getGitHost() {
		return gitHost;
	}

	public void setGitHost(String gitHost) {
		this.gitHost = gitHost;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public String getGetOrdered() {
		return getOrdered;
	}

	public void setGetOrdered(String getOrdered) {
		this.getOrdered = getOrdered;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getCommitsId() {
		return commitsId;
	}

	public void setCommitsId(String commitsId) {
		this.commitsId = commitsId;
	}


	public String getGitCommand() {
		return gitCommand;
	}

	public void setGitCommand(String gitCommand) {
		this.gitCommand = (gitCommand == null) || (gitCommand.trim().equals(""))? "git" : gitCommand;
	}

	public String getMavenHome() {
		return mavenHome;
	}

	public void setMavenHome(String mavenHome) {
		this.mavenHome = mavenHome;
	}

	public String getOnlyChanges() {
		return onlyChanges;
	}

	public void setOnlyChanges(String onlyChanges) {
		this.onlyChanges = onlyChanges;
	}
}
