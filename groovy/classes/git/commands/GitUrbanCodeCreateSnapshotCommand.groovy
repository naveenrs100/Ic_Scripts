package git.commands

import java.io.File;
import es.eci.utils.base.Loggable;
import es.eci.utils.commandline.CommandLineHelper

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import urbanCode.UrbanCodeExecutor;
import urbanCode.UrbanCodeSnapshot;
import urbanCode.UrbanCodeComponentVersion;
import groovy.io.*;
import groovy.json.*;
import es.eci.utils.ZipHelper;
import es.eci.utils.NexusHelper;
import es.eci.utils.TmpDir;
import es.eci.utils.versioner.XmlUtils;
import es.eci.utils.CheckSnapshots;
import git.commands.GitCloneCommand;

class GitUrbanCodeCreateSnapshotCommand extends Loggable {

	String componentsUrban;
	String gitGroup;
	String udClientCommand;
	String urlUrbanCode;
	String userUrban;
	String passwordUrban;
	String gitUser;
	String gitHost;	
	String gitCommand;
	String application;
	String targetBranch = "RELEASE";
	

	def execute() {
		gitCommand = (this.gitCommand == null) || (this.gitCommand.trim().equals("")) ? "git" : gitCommand;
		def componentsUrban = this.componentsUrban.split(",");

		UrbanCodeExecutor exe = new UrbanCodeExecutor(udClientCommand, urlUrbanCode, userUrban, passwordUrban);
		exe.initLogger({ println it });

		componentsUrban.each { String componentUrbanPair ->
			def componentName = componentUrbanPair.split(":")[0];
			def componentUrban = componentUrbanPair.split(":")[1];
			
			def version = getComponentLastTag(componentName,gitCommand);
			
			println("Version del componente ${componentName} -> ${version}");

			if(!componentUrban.trim().equals("") && !componentUrban.trim().equals("NULL")) {
				UrbanCodeComponentVersion componentVersion = 
				new UrbanCodeComponentVersion(componentUrban, version, null, null);
				
				try {
					// Crear la versión sobre Urban Code
					def json = exe.createVersion(componentVersion);
					println("Creada versión en UrbanCode para el componente \"${componentName}\"");
					println(json);
				} catch (Exception e) {
					// Si ha habido algún error de comunicación con UrbanCode soltamos un WARNING.
					log("[WARNING] No se ha podido crear la versión ${version} "
						+ "del componente ${componentName} en UrbanCode.  Posiblemente exista ya")
				}
			}
		}
	}

	/**
	 * Se conecta a gitLab y recoge el útimo tag del componente.
	 * @param componentName
	 * @return
	 */
	def getComponentLastTag(String componentName, String gitCommand) {
		def lastTag;
		TmpDir.tmp { File dir ->
			def gitPath = "${gitGroup}/${componentName}.git"
			GitCloneCommand cloneCommand = new GitCloneCommand(gitUser, gitHost, gitPath, 
				targetBranch, null, dir, null, null, "true", gitCommand);
			
			cloneCommand.execute();
			
			def getTgCommand = "${gitCommand} for-each-ref --sort=taggerdate --format '%(refname) %(taggerdate)' refs/tags";
			CommandLineHelper buildCommandLineHelper = new CommandLineHelper(getTgCommand);
			buildCommandLineHelper.initLogger(this);
			def returnCode = buildCommandLineHelper.execute(dir);
			if(returnCode != 0) {
				throw new Exception("Error al ejecutar el comando ${getTgCommand}. Código -> ${returnCode}");
			}
			def salida = buildCommandLineHelper.getStandardOutput();			
			salida.eachLine {
				def lastTagWithDate = it.split("/")[2];
				lastTag = lastTagWithDate.split(" ")[0]
			}			 
		}
		return lastTag;
	}

	public String getComponentsUrban() {
		return componentsUrban;
	}

	public void setComponentsUrban(String componentsUrban) {
		this.componentsUrban = componentsUrban;
	}

	public String getGitGroup() {
		return gitGroup;
	}

	public void setGitGroup(String gitGroup) {
		this.gitGroup = gitGroup;
	}

	public String getUdClientCommand() {
		return udClientCommand;
	}

	public void setUdClientCommand(String udClientCommand) {
		this.udClientCommand = udClientCommand;
	}

	public String getUrlUrbanCode() {
		return urlUrbanCode;
	}

	public void setUrlUrbanCode(String urlUrbanCode) {
		this.urlUrbanCode = urlUrbanCode;
	}

	public String getUserUrban() {
		return userUrban;
	}

	public void setUserUrban(String userUrban) {
		this.userUrban = userUrban;
	}

	public String getPasswordUrban() {
		return passwordUrban;
	}

	public void setPasswordUrban(String passwordUrban) {
		this.passwordUrban = passwordUrban;
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

	public void setTargetBranch(String targetBranch) {
		this.targetBranch = targetBranch;
	}
	
	

}
