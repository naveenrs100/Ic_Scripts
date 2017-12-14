package git

import es.eci.utils.base.Loggable
import git.beans.GitlabUser
import groovy.json.JsonSlurper

/**
 * Esta clase agrupa funcionalidad usada en gitlab.
 */
class GitlabHelper extends Loggable {
	
	//------------------------------------------------------------
	// Constantes de la clase
	
	/** Tamaño de página */
	private static final Integer PAGE_SIZE = 3; 
	
	//------------------------------------------------------------
	// Propiedades de la clase
	
	// Cliente REST
	private GitlabClient client = null;
	
	//------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Construye un helper con la información necesaria para acceder a 
	 * Gitlab.
	 * @param client Cliente para acceso a gitlab.
	 */
	public GitlabHelper(GitlabClient client) {
		this.client = client;
	}
	
	/**
	 * Devuelve la lista completa de usuarios de gitlab.
	 * @return Lista completa de usuarios.
	 */
	public List<GitlabUser> getAllUsers() {
		List<GitlabUser> ret = [];
		
		boolean keepOn = true;
		Integer pageIndex = 1;
		
		while (keepOn) {
			log("--> Leyendo usuarios: página $pageIndex ...")
			// Obtener una página de usuarios
			String json = client.get("users",
				[ "per_page": PAGE_SIZE,
				  "page": pageIndex ]);
			pageIndex = pageIndex + 1;
			def obj = new JsonSlurper().parseText(json);
			// Una vez alcanzada la última página, se devuelve un array vacío
			if (obj.size() == 0) {
				keepOn = false;
				log("--> Terminada la lectura de usuarios")
			}
			else {
				obj.each { def user ->
					if (user["state"] != "blocked") {
						GitlabUser tmp = new GitlabUser(
							user["id"], 
							user["username"], 
							user["name"], 
							user["email"]) 
						ret << tmp;
						tmp.setCreationDate(GitUtils.parseDate(user["created_at"]));
						tmp.setLastSignIn(GitUtils.parseDate(user["last_sign_in_at"]));
						tmp.setCurrentSignIn(GitUtils.parseDate(user["current_sign_in_at"]));
					}
				}
			}
		}
		
		return ret;
	}
}
