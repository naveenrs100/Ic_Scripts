package git.commands

import es.eci.utils.base.Loggable
import es.eci.utils.commandline.CommandLineHelper
import es.eci.utils.Retries;

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
		
		CommandLineHelper buildCommandLineHelper = new CommandLineHelper(command);
		buildCommandLineHelper.initLogger(this);
		
		def salida;
		Retries.retry(5,1000,{int i ->
			log "Intento ${i}."
			log "Se lanza el comando:\n ${command}\n sobre el directorio:\n ${this.parentWorkspace}";
			def returnCode = buildCommandLineHelper.execute(new File(this.parentWorkspace));
			salida = buildCommandLineHelper.getStandardOutput();
			log salida;
			if(returnCode != 0) {
				throw new Exception("Error al ejecutar el comando ${command}. Código -> ${returnCode}");
			}
		});
		
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
