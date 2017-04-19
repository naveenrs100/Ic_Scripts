package git

import es.eci.utils.TmpDir
import es.eci.utils.base.Loggable;
import es.eci.utils.commandline.CommandLineHelper
import git.commands.GitCloneCommand
import groovy.json.JsonSlurper

/** Funciones de utilidad para git. */
class GitUtils extends Loggable {
	
	//----------------------------------------------------------
	// Propiedades de la clase
	
	private String gitUser;
	private String gitHost;
	private String gitCommand;
	
	//----------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Este método consulta la caché de grupos git dejada por la noche por el job
	 * actualizarUsuariosSonar, en el fichero gitlab_groups.json, para deducir el id
	 * del grupo git.
	 * @param gitGroup Nombre del grupo git
	 * @return Id interno del grupo git correspondiente
	 */
	public static Integer getCachedGroupId(String gitGroup) {
		Integer ret = null;
		File workspace = new File("/jenkins/workspace/actualizarUsuariosSonar");
		File groupsFile = new File(workspace, "gitlab_groups.json");
		if (groupsFile.exists()) {
			String content = groupsFile.text;
			def obj = new JsonSlurper().parseText(content);
			def group = obj.find { it.name.equals(gitGroup) } 
			ret = group.id
		}
		return ret;
	}
	
	/**
	 * Construye un objeto de utilidades con la información para logarse en git
	 * @param gitUser Usuario
	 * @param gitHost Dirección de git
	 * @param gitCommand Ejecutable git a utilizar
	 */
	public GitUtils(String gitUser, String gitHost, String gitCommand) {
		super();
		this.gitUser = gitUser;
		this.gitHost = gitHost;
		this.gitCommand = gitCommand;
	}

	/**
	 * Se conecta a gitLab y recoge el útimo tag del componente.
	 * @param gitGroup Nombre de grupo gitlab
	 * @param repositoryName Nombre de repositorio
	 * @param branch Rama de la que se hace el checkout, por defecto master
	 * @return Nombre de la última tag
	 */
	public String getRepositoryLastTag(String gitGroup, String repositoryName, String branch = "master") {
		def lastTag;
		TmpDir.tmp { File dir ->
			def gitPath = "${gitGroup}/${repositoryName}.git"
			GitCloneCommand cloneCommand = new GitCloneCommand(gitUser, gitHost, gitPath,
				branch, null, dir, null, null, "true",gitCommand);
			cloneCommand.initLogger(this);
			
			cloneCommand.execute();
			
			def getTgCommand = 
				//"${gitCommand} for-each-ref --sort=taggerdate --format '%(refname) %(taggerdate)' refs/tags";
				gitCommand + ' for-each-ref --format=\'%(creatordate:raw) %(refname) \' refs/tags | sort -n | awk \'{ print $3; }\''
			CommandLineHelper buildCommandLineHelper = new CommandLineHelper(getTgCommand);
			buildCommandLineHelper.initLogger(this);
			def returnCode = buildCommandLineHelper.execute(dir);
			if(returnCode != 0) {
				throw new Exception("Error al ejecutar el comando ${getTgCommand}. Código -> ${returnCode}");
			}
			def salida = buildCommandLineHelper.getStandardOutput();
			salida.eachLine {
				def lastTagWithDate = it.split("/")[2];
				if (lastTagWithDate.contains(" ")) {
					lastTag = lastTagWithDate.split(" ")[0]
				}
				else {
					lastTag = lastTagWithDate;
				}
			}
		}
		return lastTag;
	}
}
