package git.commands

import java.text.DateFormat
import java.text.SimpleDateFormat

import es.eci.utils.ParameterValidator
import es.eci.utils.base.Loggable
import git.GitUtils;
import git.GitlabClient
import git.GitlabHelper
import git.beans.GitlabUser
import groovy.json.JsonSlurper

/**
 * Esta clase implementa una funcionalidad para detectar usuarios que lleven 
 * más de X días de inactividad en gitlab.
 * 
 * Definimos el último instante de actividad como el más reciente de entre:
 * + Último login a la interfaz web
 * + Último evento push o fetch sobre servidor
 * 
 */
class GitDisableInactiveUsersCommand extends Loggable {

	//--------------------------------------------------
	// Constantes del comando
	
	/** Eventos reconocidos como actividad */
	private static final List<String> ACTIVITY_EVENTS = [ 'push', 'tag_push' ];
	/** Días de inactividad tolerados por defecto. */
	private static final long INACTIVITY_DAYS = 30l;
	/** Umbral de inactividad, expresado en milisegundos */
	private static final long THRESHOLD = 24l * 60l * 60l * 1000l;
	/** Formato de fechas */
	private static final DateFormat DF = new SimpleDateFormat("dd/MM/yyyy")
	
	//--------------------------------------------------
	// Propiedades del comando
	
	// URL de gitlab a la que atacar con servicios REST
	private String urlGitlab;
	// Autenticación en gitlab
	private String privateGitLabToken;
	// Versión del keystore para contactar con el servidor git
	private String keystoreVersion;
	// Directorio de ejecución
	// Ruta del comando local que permite lanzar git
	private String gitCommand;
	// URL de nexus para bajar el keystore
	private String urlNexus;
	// Días de inactividad
	private Long days = INACTIVITY_DAYS; 
	// Excepciones
	private String gitUserExceptions;
	
	// Cliente git
	private GitlabClient client = null;
	
	// Indica si solamente se desea informar de los usuarios candidatos a bloqueo
	private boolean dryRun = false;
	
	// Caché de identificadores de usuario
	private Map<Integer, String> usersCache = [:]
	
	//--------------------------------------------------
	// Métodos del comando
	
	/**
	 * Crea un comando que calcula los candidatos a bloqueo y automáticamente
	 * los bloquea (es equivalente a this(false))
	 */
	public GitDisableInactiveUsersCommand() {
		this (false);
	}
	
	// Formatea una fecha al formato deseado
	private String formatDate(Date d) {
		String ret = " - ";
		if (d != null) {
			ret = DF.format(d);
		}
		return ret;
	}
	
	/**
	 * Crea un comando indicando si se desea o no ejecutar un test de usuarios
	 * candidatos a ser bloqueados.
	 * @param dryRun Si es cierto, se limita a listar en el log los usuarios
	 * candidatos a ser bloqueados.  Si es falso, lanza además el POST al 
	 * servicio rest que los bloquea.
	 */
	public GitDisableInactiveUsersCommand(boolean dryRun) {
		this.dryRun = dryRun;
	}
	
	/**
	 * @param keystoreVersion the keystoreVersion to set
	 */
	public void setKeystoreVersion(String keystoreVersion) {
		this.keystoreVersion = keystoreVersion;
	}
	/**
	 * @param gitCommand the gitCommand to set
	 */
	public void setGitCommand(String gitCommand) {
		this.gitCommand = gitCommand;
	}
	
	/**
	 * @param privateGitLabToken the privateGitLabToken to set
	 */
	public void setPrivateGitLabToken(String privateGitLabToken) {
		this.privateGitLabToken = privateGitLabToken;
	}
	/**
	 * @param urlNexus the urlNexus to set
	 */
	public void setUrlNexus(String urlNexus) {
		this.urlNexus = urlNexus;
	}
	
	/**
	 * @param urlGitlab the urlGitlab to set
	 */
	public void setUrlGitlab(String urlGitlab) {
		this.urlGitlab = urlGitlab;
	}
	
	
	/**
	 * @param dryRun the dryRun to set
	 */
	public void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
	}
	
	/**
	 * @param gitUserExceptions the gitUserExceptions to set
	 */
	public void setGitUserExceptions(String gitUserExceptions) {
		this.gitUserExceptions = gitUserExceptions;
	}	
	
	/**
	 * @param days the days to set
	 */
	public void setDays(Long days) {
		this.days = days;
	}

	
	
	// Este método devuelve la fecha de la última actividad de usuario en gitlab
	// Definimos última actividad como la fecha más reciente de entre:
	//	- Último login a la aplicación web
	//	- Último evento push
	private Date getLastActivity(GitlabUser user) {
		// Último login en la web
		Date lastSignInDate = user.getLastSignIn();
		Date currentSignInDate = user.getCurrentSignIn();
		Date creationDate = user.getCreationDate();
		// Última contribución 
		String userEventsJson = client.get("users/${user.userId}/events", [:])
		def userEventsObject = new JsonSlurper().parseText(userEventsJson);
		Date lastEventDate = null;
		userEventsObject?.each { def event ->
			if (event.data != null) {
	 			if (ACTIVITY_EVENTS.contains(event.data["object_kind"])) {
					Date tmp = GitUtils.parseDate(event["created_at"]);
					lastEventDate = GitUtils.mostRecentDate(lastEventDate, tmp);
				}
			}
		}
		log "Usuario ${user.userName} [${user.userId}] : "
		log "\tFecha de creación: ${formatDate(creationDate)}"
		log "\tÚltimo login: ${formatDate(lastSignInDate)}"
		log "\tLogin actual: ${formatDate(currentSignInDate)}"
		log "\tÚltima contribución: ${formatDate(lastEventDate)}"
		// La más reciente entre el último login y la última contribución
		Date lastActivity = GitUtils.mostRecentDate(
			currentSignInDate, lastSignInDate, lastEventDate, creationDate);
		log "\t--> Última actividad: ${formatDate(lastActivity)}"
		return lastActivity;
	}
	
	// Lanzamiento del comando
	public void execute() {
		
		// Validar parámetros de entrada
		ParameterValidator.builder().
			add("urlGitlab", urlGitlab).
			add("privateGitLabToken", privateGitLabToken).
			add("urlNexus", urlNexus).
			add("keystoreVersion", keystoreVersion).
			add("gitCommand", gitCommand).build().validate();
		
		if (dryRun) {
			log "WARNING ---> dry run - No se harán cambios sobre gitlab"
		}
			
		client = new GitlabClient(urlGitlab, privateGitLabToken, keystoreVersion, urlNexus)
		client.initLogger(this);
		List<GitlabUser> users = new GitlabHelper(client).getAllUsers();
		
		// Usuarios administradores de gitlab, mantenidos como usuarios locales de gitlab
		List<String> userExceptions = []
		if (gitUserExceptions != null) {
			userExceptions = Arrays.asList(gitUserExceptions.split(","));
		} 
		
		// Para cada usuario, obtener la información relativa a último login
		//	y última contribución a repositorio
		Map<String, Date> lastActivity = new HashMap<String, Date>();
		
		long today = new Date().getTime();
		
		List<GitlabUser> candidates = []
		
		for(GitlabUser user: users) {
			if (!userExceptions.contains(user.getUserName())) {				
				// Verificar que al menos haya pasado el periodo de días desde
				//	que el usuario fue creado
				if (today - user.getCreationDate().getTime() > 
						days * THRESHOLD) {
					Date lastUserActivity = getLastActivity(user);
					//Date creationDate = 
					lastActivity.put(user.getUserId(), lastUserActivity);
					log "${user.getUserId()} -> ${lastUserActivity}"
					if (lastUserActivity == null ||
						(today - lastUserActivity.getTime()) > 
							days * THRESHOLD) {
						log "WARNING: ${user.getUserName()} es candidato a bloqueo"
						candidates << user 
						user.setLastActivity(lastUserActivity)
					}
				}
				else {
					log "Omitiendo ${user.userName} debido a que ha sido dado de alta recientemente"
				}
			}
			else {
				log "Omitiendo ${user.userName} debido a que figura en la lista de excepciones"
			}
		} 
		
		log "==================================="
		log "Umbral: ${days} días"
		if (candidates != null && candidates.size() > 0) {
			// Informar de la lista de usuarios bloqueados
			log "Lista de usuarios bloqueados [${candidates.size()}]:"
			log "==================================="
			candidates.each { GitlabUser candidate ->
				log("${candidate.getUserName()} - ${candidate.getUserDisplayName()} - ${formatDate(candidate.getLastActivity())}")
			}
		}
		else {
			log "No hay usuarios inactivos"
			log "========================="
		}
		
		if (!dryRun) {
			if (candidates != null && candidates.size() > 0) {
				// Bloquear los usuarios
				candidates.each { GitlabUser candidate ->
					client.put("/users/${candidate.getUserId()}/block", [:])
				}				
			}
		}
		
		if (candidates != null && candidates.size() > 0) {
			// Se considera error para disparar el correo con el informe
			throw new Exception()
		}
	}

}
