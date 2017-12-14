package git.commands

import es.eci.utils.base.Loggable;
import es.eci.utils.TmpDir;
import es.eci.utils.commandline.CommandLineHelper
import git.GitUtils;

class GitPromoteTagToProductionCommand extends Loggable {

	protected String parentWorkspace;
	protected String productionBranch;
	protected String tag;
	protected String gitCommand;
	protected String gitHost;
	protected String repoPath;

	/*
	 * Método de promoción de una tag a la rama PRODUCCION
	 * git clone <host> --branch PRODUCCION
	 * git reset --hard "tag_ABC"
	 * git push --force origin PRODUCCION
	 */
	public void execute() {
		gitCommand = (this.gitCommand == null) || (this.gitCommand.trim().equals("")) ? "git" : gitCommand;

		GitUtils gu = new GitUtils(gitCommand, gitHost, gitCommand);
		boolean productionBranchExists = gu.checkRemoteBranch(this.repoPath, this.productionBranch, this.parentWorkspace);

		if(!productionBranchExists) {
			def commandsSequence = [
				"${gitCommand} clone ${this.gitCommand}@${this.gitHost}:${this.repoPath} --branch \"RELEASE\" --no-checkout .",
				"${gitCommand} push origin RELEASE:${this.productionBranch}"
			];
			commandsSequenceExecution(commandsSequence);
		}
		
		def commandsSequence =
				[	"${gitCommand} clone ${this.gitCommand}@${this.gitHost}:${this.repoPath} --branch \"${this.productionBranch}\" --no-checkout .",
					"${gitCommand} reset --hard ${this.tag}",
					"${gitCommand} push --force origin ${this.productionBranch}"
				];
		commandsSequenceExecution(commandsSequence);

	}

	private commandsSequenceExecution(ArrayList<String> commandsSequence) {
		TmpDir.tmp { File tmpDir ->
			commandsSequence.each {
				log "Ejecutando comando ${it}";
				log "sobre ${this.parentWorkspace}";
				CommandLineHelper buildCommandLineHelper = new CommandLineHelper(it);
				buildCommandLineHelper.initLogger(this);
				def returnCode = buildCommandLineHelper.execute(tmpDir);
				if(returnCode != 0) {
					throw new Exception("Error al ejecutar el comando ${it}. Código -> ${returnCode}");
				}
			}
		}
		
	}

	public String getParentWorkspace() {
		return parentWorkspace;
	}

	public void setParentWorkspace(String parentWorkspace) {
		this.parentWorkspace = parentWorkspace;
	}

	public String getProductionBranch() {
		return productionBranch;
	}

	public void setProductionBranch(String productionBranch) {
		this.productionBranch = productionBranch;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getGitCommand() {
		return gitCommand;
	}

	public void setGitCommand(String gitCommand) {
		this.gitCommand = gitCommand;
	}

	public String getGitHost() {
		return gitHost;
	}

	public void setGitHost(String gitHost) {
		this.gitHost = gitHost;
	}

	public String getRepoPath() {
		return repoPath;
	}

	public void setRepoPath(String repoPath) {
		this.repoPath = repoPath;
	}





}
