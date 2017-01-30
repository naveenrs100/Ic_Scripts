package git.commands

import org.w3c.dom.Document
import org.w3c.dom.Node

import urbanCode.UrbanCodeExecutor
import urbanCode.UrbanCodeSnapshot
import es.eci.utils.NexusHelper
import es.eci.utils.TmpDir
import es.eci.utils.ZipHelper
import es.eci.utils.base.Loggable
import es.eci.utils.versioner.XmlUtils
import git.GitUtils
import groovy.io.*
import groovy.json.*

class GitUrbanCodeCreateCompleteDescriptorCommand extends Loggable {
	
	String componentsUrban;
	String groupIdUrbanCode;
	String gradleBin;
	String cScriptsStore;
	String urlNexus;
	String urbanConnect;	
	String gitGroup;
	String instantanea; 
	String nexusUser;
	String nexusPass; 
	String application;	
	String gitUser;
	String gitHost;	
	String udClientCommand;
	String urlUrbanCode;
	String userUrban;
	String passwordUrban;
	String gitCommand;
	String targetBranch = "RELEASE";
	
	/**
	 * Crea la instantánea completa en Nexus
	 * @param urbanConnect
	 * @param instantanea
	 * @param application
	 * @param gitGroup
	 * @param nexusUser
	 * @param nexusPass
	 * @param gradleBin
	 * @param cScriptsStore
	 * @param groupIdUrbanCode
	 * @return
	 */
	def execute() {
		gitCommand = (this.gitCommand == null) || (this.gitCommand.trim().equals("")) ? "git" : gitCommand;
		// Se crea la instantánea completa.
		def componentsUrban = this.componentsUrban.split(","); 		
		def components = [];
		componentsUrban.each { String componentPair ->
			def componentName = componentPair.split(":")[0];
			def componentUrban = componentPair.split(":")[1];
			def componentVersion = new GitUtils(gitUser, gitHost, gitCommand).
				getRepositoryLastTag(gitGroup, componentName, targetBranch);
			if(!componentUrban.trim().equals("NULL")) {
				components.add(["${componentUrban}": "${componentVersion}"]);
			}
						
		}
	
		def jsonComplete = JsonOutput.toJson(["name": "${instantanea}", "application": "${application}" ,
			"description": "Snapshot Urban Code", "versions" : components])
	
		println("jsonComplete -> \n" + jsonComplete); // Json de UrbanCode completo
	
		// Se sube el nuevo descriptor al Nexus.
		TmpDir.tmp { tmpDir ->
			File tmp = new File(tmpDir.getCanonicalPath() + System.getProperty("file.separator") + "descriptor.json")
			tmp.text = jsonComplete;
			File zip = ZipHelper.addDirToArchive(tmpDir);
			try {
				def artifactId = application.replace(" - ", "_").replace(" -", "_").replace("- ", "_").replace(" ", "_");
				println "Subiendo el artifactId a la ruta ${groupIdUrbanCode}:${artifactId}:${instantanea} desde ${zip.getCanonicalPath()}"
				NexusHelper.uploadTarNexus(nexusUser, nexusPass, gradleBin, cScriptsStore,
						"fichas_despliegue", groupIdUrbanCode, artifactId, "${instantanea}", urlNexus, "true", zip.getCanonicalPath(), "zip", {println it});
	
				println "Subido del descriptor completo a Nexus.";
	
			} catch (Exception e) {
				println("[WARNING]: Ha habido un problmema subiendo el descriptor completo a Nexus:");
				e.printStackTrace();
			}
			finally {
				zip.delete()
			}
		}
	
		def objCompleteSnapshot = UrbanCodeSnapshot.parseJSON(jsonComplete);
		if(urbanConnect == "true") {
			UrbanCodeExecutor exe = new UrbanCodeExecutor(udClientCommand, urlUrbanCode, userUrban, passwordUrban);
			exe.initLogger({ println it });
			exe.createSnapshot(objCompleteSnapshot);
		}
	
	}
	
	/**
	 * Devuelve la versión de un pom.xml
	 * @param pomFile
	 * @return Sting pomVersion
	 */
	def String getPomVersion(File pomFile) {
		Document doc = XmlUtils.parseXml(pomFile);
		Node versionNode = XmlUtils.xpathNode(doc, "/project/version");
		String pomVersion = XmlUtils.solve(doc, versionNode.getTextContent());
		return pomVersion;
	}
	
	/**
	 * Elimina el último carácter de un String.
	 * Lo usamos porque la lista de jobs vendrá
	 * con una coma al final que hay que eliminar.
	 * @param (String)text
	 * @return (String)result
	 */
	def String removeLastComma(String text) {
		def result;
		if(text.endsWith(",")) {
			result = text.substring(0, text.length() - 1);
		} else {
			result = text;
		}
		return result;
	}
	
	
	// Geters and Setters
	
	public String getComponentsUrban() {
		return componentsUrban;
	}

	public void setComponentsUrban(String componentsUrban) {
		this.componentsUrban = componentsUrban;
	}

	public String getGroupIdUrbanCode() {
		return groupIdUrbanCode;
	}

	public void setGroupIdUrbanCode(String groupIdUrbanCode) {
		this.groupIdUrbanCode = groupIdUrbanCode;
	}

	public String getGradleBin() {
		return gradleBin;
	}

	public void setGradleBin(String gradleBin) {
		this.gradleBin = gradleBin;
	}

	public String getcScriptsStore() {
		return cScriptsStore;
	}

	public void setcScriptsStore(String cScriptsStore) {
		this.cScriptsStore = cScriptsStore;
	}

	public String getUrlNexus() {
		return urlNexus;
	}

	public void setUrlNexus(String urlNexus) {
		this.urlNexus = urlNexus;
	}

	public String getUrbanConnect() {
		return urbanConnect;
	}

	public void setUrbanConnect(String urbanConnect) {
		this.urbanConnect = urbanConnect;
	}

	public String getGitGroup() {
		return gitGroup;
	}

	public void setGitGroup(String gitGroup) {
		this.gitGroup = gitGroup;
	}

	public String getInstantanea() {
		return instantanea;
	}

	public void setInstantanea(String instantanea) {
		this.instantanea = instantanea;
	}

	public String getNexusUser() {
		return nexusUser;
	}

	public void setNexusUser(String nexusUser) {
		this.nexusUser = nexusUser;
	}

	public String getNexusPass() {
		return nexusPass;
	}

	public void setNexusPass(String nexusPass) {
		this.nexusPass = nexusPass;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getGitUser() {
		return gitUser;
	}

	public void setGitUser(String gitUser) {
		this.gitUser = gitUser;
	}

	public String getGitHost() {
		return gitHost;
	}

	public void setGitHost(String gitHost) {
		this.gitHost = gitHost;
	}

	public String getGitCommand() {
		return gitCommand;
	}

	public void setGitCommand(String gitCommand) {
		this.gitCommand = gitCommand;
	}

	/**
	 * @param targetBranch the targetBranch to set
	 */
	public void setTargetBranch(String targetBranch) {
		this.targetBranch = targetBranch;
	}
	
	
}
