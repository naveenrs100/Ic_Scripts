package git

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

import es.eci.utils.TmpDir
import es.eci.utils.base.Loggable
import es.eci.utils.commandline.CommandLineHelper
import git.commands.GitCloneCommand
import groovy.json.JsonSlurper

/** Funciones de utilidad para git. */
class GitUtils extends Loggable {

	//----------------------------------------------------------
	// Constantes de la clase

	/** Formato de último sign in */
	private static final DateFormat GITLAB_DATE_FORMAT =
	new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

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
		def lastTag = null;
		try {
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
		} catch (Exception e) {
			log "-- No se ha podido sacar la última tag de la branch ${branch} del repositorio \"${gitGroup}/${repositoryName}.git\""
		}
		return lastTag;
	}

	/**
	 * Parsea una fecha devuelta por el webservice de gitlab.
	 * @param s Fecha en formato cadena
	 * @return Objeto fecha correspondiente
	 */
	public static Date parseDate(String s) {
		Date ret = null;
		if (s != null) {
			ret = GITLAB_DATE_FORMAT.parse(s);
		}
		return ret;
	}

	// Devuelve la fecha más reciente entre dos fechas
	public static Date mostRecentDate(Date... dates) {
		List<Date> tmp = []
		dates.each { if (it != null) { tmp << it } }
		Date ret = null;
		if (tmp != null && tmp.size() > 0) {
			Collections.sort(tmp);
			ret = tmp[tmp.size() - 1];
		}
		return ret;
	}
	
	/**
	 * Comprueba si existe una branch para un repositorio remoto.
	 * @param branch
	 * @return
	 */
	public boolean checkRemoteBranch(String repoPath, String branch, String parentWorkspace) {
		def listBranchesCommand = "${this.gitCommand} ls-remote --heads ${gitUser}@${this.gitHost}:${repoPath}";
		CommandLineHelper buildCommandLineHelper = new CommandLineHelper(listBranchesCommand);
		buildCommandLineHelper.initLogger(this);
		def returnCode = buildCommandLineHelper.execute(new File("${parentWorkspace}"));
				
		if(returnCode != 0) {
			throw new Exception("Error al ejecutar el comando ${listBranchesCommand}. Código -> ${returnCode}");
		}
		boolean ret = false;
		def output = buildCommandLineHelper.getStandardOutput();
		output.eachLine { String line ->
			if(line.trim().endsWith(branch)) {
				ret = true;
			}
		}
		return ret;
	}
}
