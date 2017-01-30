package git.commands

import java.io.File;

import es.eci.utils.base.Loggable;
import es.eci.utils.commandline.CommandLineHelper
import git.commands.GitCloneCommand;
import es.eci.utils.TmpDir;

class GitTaggerCommand extends Loggable {
	
	public GitTaggerCommand() {
		// Empty constructor.
	}
	
	public GitTaggerCommand(String tag, String comment, String isAnnotated,
			String branch, String gitUser, String gitHost, String gitPath,
			String gitCommand = null) {
		super();
		this.tag = tag;
		this.comment = comment;
		this.isAnnotated = isAnnotated;
		this.branch = branch;
		this.gitUser = gitUser;
		this.gitHost = gitHost;
		this.gitPath = gitPath;
		this.gitCommand = gitCommand;
	}

	protected String tag;
	protected String comment;	
	protected String isAnnotated;
	protected String branch;
	protected String gitUser;
	protected String gitHost;
	protected String gitPath;	
	protected String gitCommand;
	
	public void execute() {

		TmpDir.tmp { File dir ->
			GitCloneCommand cloneCommand = new GitCloneCommand();
			cloneCommand.setGitUser(gitUser);
			cloneCommand.setGitHost(gitHost);
			cloneCommand.setGitPath(gitPath);
			cloneCommand.setGitBranch(branch);
			cloneCommand.setParentWorkspace(dir);
			cloneCommand.setGitCommand(gitCommand);
			cloneCommand.setEmpty("true");
			cloneCommand.execute();
			
			gitCommand = (this.gitCommand == null) || (this.gitCommand.trim().equals("")) ? "git" : gitCommand;
			
			def tagCommand = "${gitCommand} tag -a ${tag} -m ${comment}";
			println "Se va a lanzar el comando de etiquetado -> ${tagCommand}"

			CommandLineHelper buildCommandLineHelper = new CommandLineHelper(tagCommand);
			buildCommandLineHelper.initLogger(this);
			def returnCode = buildCommandLineHelper.execute(dir);
			if(returnCode != 0) {
				throw new Exception("Error al ejecutar el comando ${tagCommand}. Code -> ${returnCode}");
			}

			def pushTagCommand = "${gitCommand} push --tags";
			buildCommandLineHelper = new CommandLineHelper(pushTagCommand);
			returnCode = buildCommandLineHelper.execute(dir);
			if(returnCode != 0) {
				throw new Exception("Error al ejecutar el comando ${tagCommand}. Code -> ${returnCode}");
			}
		}
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}	

	public String getIsAnnotated() {
		return isAnnotated;
	}

	public void setIsAnnotated(String isAnnotated) {
		this.isAnnotated = isAnnotated;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
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

	public String getGitPath() {
		return gitPath;
	}

	public void setGitPath(String gitPath) {
		this.gitPath = gitPath;
	}

	public String getGitCommand() {
		return gitCommand;
	}

	public void setGitCommand(String gitCommand) {
		this.gitCommand = gitCommand;
	}


}
