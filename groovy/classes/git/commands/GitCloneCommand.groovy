package git.commands;

import java.io.File;

import es.eci.utils.base.Loggable
import es.eci.utils.commandline.CommandLineHelper;

/**
 * Comando tipo: git user@host:path --branch [branchName]
 * <br/>
 * Comando del cliente git:<br/>
 * git clone [--template=<template_directory>]
 [-l] [-s] [--no-hardlinks] [-q] [-n] [--bare] [--mirror]
 [-o <name>] [-b <name>] [-u <upload-pack>] [--reference <repository>]
 [--dissociate] [--separate-git-dir <git dir>]
 [--depth <depth>] [--[no-]single-branch]
 [--recursive | --recurse-submodules] [--] <repository>
 [<directory>]
 * 
 *<br/><br/>
    --- OBLIGATORIOS<br/>
  
 	<b>gitUser</b> Usuario de conexión.<br/>
	<b>gitHost</b> URL de git.<br/>
	<b>gitPath</b> Ruta del repositorio en git (incluye nombre de grupo).<br/>
	<b>gitBranch</b> Rama de la que hacer el clone.<br/>
	<b>parentWorkspace</b> Directorio de trabajo.<br/>
	<br/>
	--- OPCIONALES<br/>
	
	<b>localFolderName</b> Directorio de destino del clone.<br/>
	<b>additionalParams</b> Parámetros adicionales al git clone.<br/>
	<b>tag</b> Si viene informada la etiqueta, baja el contenido de la etiqueta.<br/>
	<b>empty</b> Por defecto: false.  Si viene informado, hace un git clone --no-checkout, es
		decir, un dry run<br/>
	<b>gitCommand</b> Por defecto: git.  Si viene informado, usa un comando git distinto
		en la máquina de ejecución<br/>
 *
 */
public class GitCloneCommand extends Loggable {

	protected String gitUser;
	protected String gitHost;
	protected String gitPath;
	protected String gitBranch;
	protected String localFolderName;
	protected File parentWorkspace;
	protected String additionalParams;
	protected String tag;
	protected String empty;
	protected String gitCommand;
	// Último valor de retorno
	private Integer returnCode = null;


	public GitCloneCommand() {
		super();		
		// TODO Auto-generated constructor stub
	}

	public GitCloneCommand(String gitUser, String gitHost, String gitPath,
	String gitBranch, String localFolderName, File parentWorkspace,
	String additionalParams, String tag, String empty, String gitCommand = null) {
		this.gitUser = gitUser;
		this.gitHost = gitHost;
		this.gitPath = gitPath;
		this.gitBranch = gitBranch;
		this.localFolderName = localFolderName;
		this.parentWorkspace = parentWorkspace;
		this.additionalParams = additionalParams;
		this.tag = tag;
		this.empty = empty;
		this.gitCommand = (gitCommand == null) || (gitCommand.trim().equals("")) ? "git" : gitCommand;
	}

	/**
	 * Se ejecuta un git clone de la rama deseada y sobre el directorio local indicado.
	 */
	public void execute() {
		gitCommand = (this.gitCommand == null) || (this.gitCommand.trim().equals("")) ? "git" : gitCommand;
		String command = "${this.gitCommand} clone ${gitUser}@${gitHost}:${gitPath} --branch ${gitBranch}";
		tag = (this.tag != null && this.tag.trim().equals("")) ? null : this.tag;
		empty = (this.empty == null || this.empty.trim().equals("")) ? "false" : this.empty;

		deleteCloneDir(parentWorkspace);

		// Si no se indica tag se bajará el contenido asociado al último commit
		if(tag == null || tag.trim().equals("") ) {
			command = completeCommand(command);

			//println "Se lanza el comando:\n ${command}\n Sobre el directorio:\n ${this.parentWorkspace}";
			CommandLineHelper buildCommandLineHelper = new CommandLineHelper(command);
			buildCommandLineHelper.initLogger(this);
			returnCode = buildCommandLineHelper.execute(this.parentWorkspace);

			if(returnCode != 0) {
				throw new Exception("Error al ejecutar el comando ${command}. Código -> ${returnCode}");
			}
		}

		// Si se indica tag se bajará el contenido asociado al tag
		else if(!tag.trim().equals("")) {
			command = command + " --no-checkout";
			command = completeCommand(command);

			def commands = [command, "${this.gitCommand} checkout tags/${tag}"];

			commands.each {
				CommandLineHelper buildCommandLineHelper = new CommandLineHelper(it);
				buildCommandLineHelper.initLogger(this);
				def returnCode = buildCommandLineHelper.execute(this.parentWorkspace);
				if(returnCode != 0) {
					throw new Exception("Error al ejecutar el comando ${it}. Código -> ${returnCode}");
				}
			}
		}
	}

	/**
	 * Borra el contenido previo del directorio donde se va a descargar el contenido
	 * @param dir
	 */
	private void deleteCloneDir(File dir) {
		if(dir.exists()) {
			dir.eachFile { File file ->
				if(file.isDirectory()) {
					file.deleteDir();
				} else {
					file.delete();
				}
			}
		}
	}

	/**
	 * Devuelve el comando completando con argumentos.
	 * @param command
	 * @return command
	 */
	private completeCommand(String command) {
		if(this.localFolderName == null || this.localFolderName.size() < 1) {
			// Clona en la raiz del directorio donde se ejecuta.
			command = command + " .";
		} else {
			// Clona en el subdirectorio indicado.
			command = command + " \"${this.localFolderName}\"";
		}

		if(this.empty.equals("true")) {
			command = command + " --no-checkout";
		}

		if(!(this.additionalParams == null || this.additionalParams.size() < 1)) {
			command = command + " ${this.additionalParams}";
		}
		return command;
	}

	// Getters and Setters
	public void setGitUser(String gitUser) {
		this.gitUser = gitUser;
	}

	public void setGitHost(String gitHost) {
		this.gitHost = gitHost;
	}

	public void setGitPath(String gitPath) {
		this.gitPath = gitPath;
	}

	public void setGitBranch(String gitBranch) {
		this.gitBranch = gitBranch;
	}

	public void setParentWorkspace(File parentWorkspace) {
		this.parentWorkspace = parentWorkspace;
	}

	public void setAdditionalParams(String additionalParams) {
		this.additionalParams = additionalParams;
	}

	public void setLocalFolderName(String localFolderName) {
		this.localFolderName = localFolderName;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getEmpty() {
		return empty;
	}

	public void setEmpty(String empty) {
		this.empty = empty;
	}

	public String getGitCommand() {
		return gitCommand;
	}

	public void setGitCommand(String gitCommand) {
		this.gitCommand = (gitCommand == null) || (gitCommand.trim().equals("")) ? "git" : gitCommand;
	}
	
	/**
	 * @return Código de retorno del último comando ejecutado.  Null si no
	 * se ha ejecutado ninguno todavía.
	 */
	public Integer getLastReturnCode() {
		return this.returnCode;
	}
}
