package sonar.usersinterface

import es.eci.utils.base.Loggable
import git.GitlabClient
import groovy.json.JsonSlurper


class GitUsersReader extends Loggable {

	/**
	 * Este método debe procesar los grupos y usuarios de gitlab, y partiendo
	 * de la información disponible de sonar, reconstruir la información de grupos.
	 * @param client Cliente para acceder a gitlab
	 * @param sonarInfo Objeto previamente inicializado con la caché de grupos
	 * 	y permisos de sonar.
	 * @return Lista resultante de actualizar los grupos existentes en Sonar con la
	 * 	información de gitlab.
	 */
	public List<SonarGroup> getGroups(GitlabClient client, SonarInstancePermissionsInfo sonarInfo) {
		SonarGroupsMerger merger = new SonarGroupsMerger(sonarInfo);
		merger.initLogger(this);
		// Recuperar de git la información de grupos y usuarios
		List<SonarGroup> ret = [];
		String groupsJson = client.get("groups", [:]);
		def groupsObject = new JsonSlurper().parseText(groupsJson);
		groupsObject.each { def group ->
			// Recuperar los usuarios del grupo
			String usersJson = client.get("groups/${group.id}/members", [:]);
			def usersObject = new JsonSlurper().parseText(usersJson);
			List<String> users = []
			usersObject.each { def user -> users << user.username }
			// Con el nombre de grupo y la lista, mezclarlo sobre sonar
			ret << merger.merge(group.name, users);
		}
		return ret;
	}
}
