package git.commands

import es.eci.utils.base.Loggable
import es.eci.utils.commandline.CommandLineHelper

class GitLogCommand extends Loggable{

	String resultsNumber;
	String parentWorkspace;
	String gitCommand;
	
	
	
	public GitLogCommand() {
		super();
	}

	public GitLogCommand(String resultsNumber, String parentWorkspace, String gitCommand = null) {
		super();
		this.resultsNumber = resultsNumber;
		this.parentWorkspace = parentWorkspace;
		this.gitCommand = (gitCommand == null) || (gitCommand.trim().equals("")) ? "git" : gitCommand;
	}

	public String execute() {
		gitCommand = (this.gitCommand == null) || (this.gitCommand.trim().equals("")) ? "git" : gitCommand;
		String command = "${gitCommand} log";
		
		if(this.resultsNumber == null || this.resultsNumber.size() < 1) {
			// Saca todos los resultados del log
			command = command;
		} else {
			// Saca solo los resultNumber resultados del log.
			command = command + " -n ${resultsNumber}";
		}
		
		println "Se lanza el comando:\n ${command}\n sobre el directorio:\n ${this.parentWorkspace}";		
		CommandLineHelper buildCommandLineHelper = new CommandLineHelper(command);	
		buildCommandLineHelper.initLogger(this);
		buildCommandLineHelper.execute(new File(this.parentWorkspace));
		def salida = buildCommandLineHelper.getStandardOutput();
		println salida;
		return salida;
	}
	
	//Getters and Setters	
	public String getResultsNumber() {
		return resultsNumber;
	}

	public void setResultsNumber(String resultsNumber) {
		this.resultsNumber = resultsNumber;
	}

	public String getParentWorkspace() {
		return parentWorkspace;
	}

	public void setParentWorkspace(String parentWorkspace) {
		this.parentWorkspace = parentWorkspace;
	}

	public String getGitCommand() {
		return gitCommand;
	}

	public void setGitCommand(String gitCommand) {
		this.gitCommand = (gitCommand == null) || (gitCommand.trim().equals("")) ? "git" : gitCommand;
	}
}
