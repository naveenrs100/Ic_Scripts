package git.commands

import es.eci.utils.base.Loggable;
import es.eci.utils.commandline.CommandLineHelper
import java.nio.file.Files;
import java.nio.file.Paths;

class GitUploadCommand extends Loggable {

	protected String commitMessage;
	protected String parentWorkspace;	
	protected boolean sameBranch;
	protected String originFolder;
	protected String targetBranch;
	protected String gitUser;
	protected String gitHost;
	protected String gitPath;
	protected String originBranch;
	protected String gitCommand;
	protected String gitFilter;

	public void execute() {
		if(this.sameBranch) {
			executeUploadInSameBranch();
		} else {
			executeUploadToOtherBranch();
		}
	}

	/**
	 * Commit y push desde y sobre una misma rama.
	 * Se añaden los nuevos cambios y se hace un push y un commit 
	 * del nuevo contenido a la rama.
	 */
	public void executeUploadInSameBranch() {	
		gitCommand = (this.gitCommand == null) || (this.gitCommand.trim().equals("")) ? "git" : gitCommand;
		
		// INI - GDR - 18/11/2016 - Modificación para realizar add, únicamente del fichero package.json
		// Existen construcciones que modifican la estructura del workspace y haciendo un git add -A, se
		// suben cambios no deseados al repositorio, provocando el fallo de compilaciones futuras.
		def commands = []
		if ( !gitFilter.isEmpty() ) {
			File folder = new File("${this.parentWorkspace}")
			folder.traverse (
				type: groovy.io.FileType.FILES,
				preDir : { if (it.name == 'node_modules' || it.name == 'target') 
					return groovy.io.FileVisitResult.SKIP_SUBTREE },
				nameFilter: ~/${this.gitFilter}/
				) { 
				// Se comprueba que el archivo del que se va a hacer add no está ignorado por el .gitignore
				CommandLineHelper buildCommandLineHelper = new CommandLineHelper("${gitCommand} check-ignore pathname \"" + it.canonicalPath + "\"");
				buildCommandLineHelper.initLogger(this);
				def returnCode = buildCommandLineHelper.execute(new File(this.parentWorkspace));
				if(returnCode != 0) {
					commands.add( "${gitCommand} add " + it.canonicalPath );
				} else {
					log "--No se hace add de la ruta \"${it.canonicalPath}\""
				}
				 
				}
		} else {
			commands.add( "${gitCommand} add -A" )
		}
		// INI - GDR - 24/11/2016 - El -a es redundante, se comprueba en el paso anterior //
		commands.add( "${gitCommand} commit -m \"${this.commitMessage}\" --no-verify" )
		// FIN - GDR - 24/11/2016
		commands.add( "${gitCommand} push --force" )
		// FIN - GDR - 18/11/2016
		commands.each {
			log "Se lanza el comando:\n ${it}\n Sobre el directorio:\n ${this.parentWorkspace}";
			CommandLineHelper buildCommandLineHelper = new CommandLineHelper(it);
			buildCommandLineHelper.initLogger(this);
			def returnCode = buildCommandLineHelper.execute(new File("${this.parentWorkspace}"));
			if(returnCode != 0) {
				throw new Exception("Error al ejecutar el comando ${it}. Código -> ${returnCode}");
			}
		}
	}
	
	/**
	 * Commit y push desde una rama hacia otra.
	 * IMPORTANTE: Requiere que la rama origen esté bajada bajo un subdirectorio.
	 * 1) Se baja el contenido de la rama destino.
	 * 2) Se borra el contenido de la rama destino (excepto el directorio .git)
	 * 3) Se copia el contenido local del directorio de la rama origen al directorio local de la rama destino.
	 * 4) Se hace un commit y un push del nuevo contenido de la rama destino.
	 * (Pensar una forma más elegante usando Git)
	 */
	public void executeUploadToOtherBranch() {
		log "Se sube el contenido al branch ${targetBranch}";
		gitCommand = (this.gitCommand == null) || (this.gitCommand.trim().equals("")) ? "git" : gitCommand;
		String commandCloneTarget = "${gitCommand} clone ${gitUser}@${gitHost}:${gitPath} --branch ${this.targetBranch} \"${this.targetBranch}\"";

		log "Se lanza el comando:\n ${commandCloneTarget}\n Sobre el directorio:\n ${this.parentWorkspace}";
		CommandLineHelper buildCommandLineHelper = new CommandLineHelper(commandCloneTarget);
		buildCommandLineHelper.initLogger(this);
		def returnCode = buildCommandLineHelper.execute(new File(this.parentWorkspace));		
		if(returnCode != 0) {
			throw new Exception("Error al ejecutar el comando ${commandCloneTarget}. Código -> ${returnCode}");
		}
		
		// Se borran los archivos locales descargados del branch (excepto el directorio .git)
		log "Borrando archivos de ${this.parentWorkspace}/${this.targetBranch} (excepto .git) ..."
		new AntBuilder().delete(includeemptydirs: true) {
			fileset(
				dir: "${this.parentWorkspace}/${this.targetBranch}",
				excludes: "**/.git/**",
				defaultexcludes: false
				)
		}

		// Se copian los archivos de la rama de origen al directorio de la rama destino.
		log "Copiando de ${this.parentWorkspace}/${this.originFolder} a ${this.parentWorkspace}/${this.targetBranch} ...";
		new AntBuilder().copy(
				includeemptydirs: true,
				todir:"${this.parentWorkspace}/${this.targetBranch}",
				force: true,
				overwrite: true) {
					fileset(
						dir:"${this.parentWorkspace}/${this.originFolder}",
						defaultexcludes: false) {
							exclude(name: "**/.git/**")
					}
				}

		// Se añaden los nuevos contenidos del branch destino a la rama
		// INI - GDR - 18/11/2016 - Modificación para realizar add, únicamente del fichero package.json
		// Existen construcciones que modifican la estructura del workspace y haciendo un git add -A, se
		// suben cambios no deseados al repositorio, provocando el fallo de compilaciones futuras.
		def commands = []
		if ( !gitFilter.isEmpty() ) {
			File folder = new File("${this.parentWorkspace}")
			folder.traverse (
				type: groovy.io.FileType.FILES,
				preDir : { if (it.name == 'node_modules') return groovy.io.FileVisitResult.SKIP_SUBTREE },
				nameFilter: ~/${this.gitFilter}/
				) { commands.add( "${gitCommand} add " + it.canonicalPath ) }
		} else {
			commands.add( "${gitCommand} add -A" )
		}
		// INI - GDR - 24/11/2016 - El -a es redundante, se comprueba en el paso anterior //
		commands.add( "${gitCommand} commit -m \"${this.commitMessage}\" --no-verify" )
		// FIN - GDR - 24/11/2016
		commands.add( "${gitCommand} push --force" )
		// FIN - GDR - 18/11/2016
		commands.each {
			log "Se lanza el comando:\n ${it}\n Sobre el directorio:\n ${this.parentWorkspace}/${this.targetBranch}";
			CommandLineHelper buildCommandLineHelperCommit = new CommandLineHelper(it);
			buildCommandLineHelperCommit.initLogger(this);
			returnCode = buildCommandLineHelperCommit.execute(new File("${this.parentWorkspace}/${this.targetBranch}"));
			if(returnCode != 0) {
				throw new Exception("Error al ejecutar el comando ${it}. Código -> ${returnCode}");
			}
		}

	}

	// Getters and Setters
	public void setCommitMessage(String commitMessage) {
		this.commitMessage = commitMessage;
	}	

	public void setSameBranch(boolean sameBranch) {
		this.sameBranch = sameBranch;
	}

	public void setTargetBranch(String targetBranch) {
		this.targetBranch = targetBranch;
	}

	public void setGitUser(String gitUser) {
		this.gitUser = gitUser;
	}

	public void setGitHost(String gitHost) {
		this.gitHost = gitHost;
	}

	public void setGitPath(String gitPath) {
		this.gitPath = gitPath;
	}

	public void setOriginBranch(String originBranch) {
		this.originBranch = originBranch;
	}

	public void setOriginFolder(String originFolder) {
		this.originFolder = originFolder;
	}

	public void setParentWorkspace(String parentWorkspace) {
		this.parentWorkspace = parentWorkspace;
	}

	public String getGitCommand() {
		return gitCommand;
	}

	public void setGitCommand(String gitCommand) {
		this.gitCommand = gitCommand;
	}
	
	public void setGitFilter(String gitFilter) {
		this.gitFilter = gitFilter;
	}
}
