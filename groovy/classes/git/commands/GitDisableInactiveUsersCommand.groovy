package git.commands

import java.text.DateFormat
import java.text.SimpleDateFormat

import es.eci.utils.ParameterValidator
import es.eci.utils.base.Loggable
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
	
	// Paginación de usuarios
	
	/** Formato de último sign in */
	private static final DateFormat GITLAB_DATE_FORMAT = 
		new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	/** Eventos reconocidos como actividad */
	private static final List<String> ACTIVITY_EVENTS = [ 'push', 'tag_push' ];
	/** Días de inactividad tolerados por defecto. */
	private static final long INACTIVITY_DAYS = 30l;
	/** Umbral de inactividad, expresado en milisegundos */
	private static final long THRESHOLD = 24l * 60l * 60l * 1000l;
	
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
	 * @param days the days to set
	 */
	public void setDays(Long days) {
		this.days = days;
	}

	// Aplica a una cadena el formato de gitlab
	private Date parseDate(String s) {
		Date ret = null;
		if (s != null) {
			ret = GITLAB_DATE_FORMAT.parse(s);
		}
		return ret;
	}
	
	// Devuelve la fecha más reciente entre dos fechas
	private Date mostRecentDate(Date a, Date b) {
		Date ret = null;
		if (a != null || b != null) {
			if (a == null) {
				ret = b;
			}
			else if (b == null) {
				ret = a;
			}
			else {
				ret = a.compareTo(b) < 0?b:a;
			}
		}
		return ret;
	}
	
	// Este método devuelve la fecha de la última actividad de usuario en gitlab
	// Definimos última actividad como la fecha más reciente de entre:
	//	- Último login a la aplicación web
	//	- Último evento push
	private Date getLastActivity(long userId) {
		// Último login en la web
		String userJson = client.get("users/${userId}", [:])
		def userObject = new JsonSlurper().parseText(userJson);
		String lastSignIn = userObject["last_sign_in_at"];
		Date lastSignInDate = parseDate(lastSignIn);
		// Última contribución 
		String userEventsJson = client.get("users/${userId}/events", [:])
		def userEventsObject = new JsonSlurper().parseText(userEventsJson);
		Date lastEventDate = null;
		userEventsObject?.each { def event ->
			if (event.data != null) {
	 			if (ACTIVITY_EVENTS.contains(event.data["object_kind"])) {
					Date tmp = parseDate(event["created_at"]);
					lastEventDate = mostRecentDate(lastEventDate, tmp);
				}
			}
		}
		log "Usuario $userId : "
		log "\tÚltimo login: $lastSignInDate"
		log "\tÚltima contribución: $lastEventDate"
		// La más reciente entre el último login y la última contribución
		Date lastActivity = mostRecentDate(lastSignInDate, lastEventDate);
		log "\tÚltima actividad: $lastActivity"
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
			
		client = new GitlabClient(urlGitlab, privateGitLabToken)
		client.initLogger(this);
		List<GitlabUser> users = new GitlabHelper(client).getAllUsers();
		
		// Para cada usuario, obtener la información relativa a último login
		//	y última contribución a repositorio
		Map<String, Date> lastActivity = new HashMap<String, Date>();
		
		long today = new Date().getTime();
		
		List<GitlabUser> candidates = []
		
		for(GitlabUser user: users) {
			Date lastUserActivity = getLastActivity(user.getUserId());
			lastActivity.put(user.getUserId(), lastUserActivity);
			log "${user.getUserId()} -> ${lastUserActivity}"
			if (lastUserActivity == null ||
				(today - lastUserActivity.getTime()) > 
					days * THRESHOLD) {
				log "WARNING: ${user.getUserName()} es candidato a bloqueo"
				candidates << user 
			}
		} 
		
		log "==================================="
		log "Umbral: ${days} días"
		if (candidates != null && candidates.size() > 0) {
			// Informar de la lista de usuarios bloqueados
			log "Lista de usuarios bloqueados [${candidates.size()}]:"
			log "==================================="
			candidates.each { GitlabUser candidate ->
				log(candidate.getUserName())
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
	}
}
