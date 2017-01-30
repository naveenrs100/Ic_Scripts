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

class GitGetGroupComponentsCommand extends Loggable {
	
	// Nombre del grupo en git
	String gitGroup;
	
	// Versión del keystore para contactar con el servidor git
	String keystoreVersion;
	
	// Directorio de ejecución
	String parentWorkspace;
	
	// Token de seguridad de gitLab
	String privateGitLabToken;	
	
	// URL de gitlab a la que atacar con servicios REST
	String urlGitlab;
	
	// URL de nexus para bajar el keystore
	String urlNexus;	
	
	String branch;

	public void execute() {
		
		branch = getProperBranchName(branch); 		
		
		GitlabClient gitLabClient = new GitlabClient(urlGitlab, privateGitLabToken, keystoreVersion, urlNexus);
		gitLabClient.initLogger(this);
		
		def entity = "groups/${gitGroup}";
		def jsonResponse = gitLabClient.get(entity, null);
		
		def jsonSlurper = new JsonSlurper()
		def jsonObject = jsonSlurper.parseText(jsonResponse);
		
		def jenkinsComponents = new File("${parentWorkspace}/jenkinsComponents.txt");
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
	}
	
	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
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
	public String getGitGroup() {
		return gitGroup;
	}

	public void setGitGroup(String gitGroup) {
		this.gitGroup = gitGroup;
	}

	public String getKeystoreVersion() {
		return keystoreVersion;
	}

	public void setKeystoreVersion(String keystoreVersion) {
		this.keystoreVersion = keystoreVersion;
	}

	public String getParentWorkspace() {
		return parentWorkspace;
	}

	public void setParentWorkspace(String parentWorkspace) {
		this.parentWorkspace = parentWorkspace;
	}

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

	public String getUrlNexus() {
		return urlNexus;
	}

	public void setUrlNexus(String urlNexus) {
		this.urlNexus = urlNexus;
	}
	
}
