package git.commands

import es.eci.utils.base.Loggable;
import es.eci.utils.commandline.CommandLineHelper

class GitMergeBranchesCommand extends Loggable {
	
	protected String parentWorkspace;
	protected String originBranch;
	protected String targetBranch;	
	protected String gitCommand;
	
	/**
	 * Método para machacar la rama de RELEASE con el contenido de DESARROLLO.
	 * Método sacado de http://stackoverflow.com/questions/2862590/how-to-replace-master-branch-in-git-entirely-from-another-branch
	 * git checkout DESARROLLO
	 * git merge -s ours RELEASE
	 * git checkout RELEASE
	 * git merge DESARROLLO
	 * git push
	 */
	public void execute() {
		gitCommand = (this.gitCommand == null) || (this.gitCommand.trim().equals("")) ? "git" : gitCommand;
		def commandsSequence = 
		[	"${gitCommand} checkout --force ${this.originBranch}",
			"${gitCommand} merge -s ours origin/${this.targetBranch}",
			"${gitCommand} checkout ${this.targetBranch}",
			"${gitCommand} merge ${this.originBranch}",
			"${gitCommand} push",
			"${gitCommand} checkout ${this.originBranch}"	
		];
		
		commandsSequence.each {
			log "Ejecutando comando ${it}";
			log "sobre ${this.parentWorkspace}";
			CommandLineHelper buildCommandLineHelper = new CommandLineHelper(it);
			buildCommandLineHelper.initLogger(this);
			def returnCode = buildCommandLineHelper.execute(new File("${this.parentWorkspace}"));
			if(returnCode != 0) {
				throw new Exception("Error al ejecutar el comando ${it}. Código -> ${returnCode}");
			}
		}
					
	}

	public void setParentWorkspace(String parentWorkspace) {
		this.parentWorkspace = parentWorkspace;
	}

	public void setOriginBranch(String originBranch) {
		this.originBranch = originBranch;
	}

	public void setTargetBranch(String targetBranch) {
		this.targetBranch = targetBranch;
	}

	public String getGitCommand() {
		return gitCommand;
	}

	public void setGitCommand(String gitCommand) {
		this.gitCommand = gitCommand;
	}

	public String getParentWorkspace() {
		return parentWorkspace;
	}

	public String getOriginBranch() {
		return originBranch;
	}

	public String getTargetBranch() {
		return targetBranch;
	}
	
	
	
}
