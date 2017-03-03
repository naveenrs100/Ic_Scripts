package sonar.usersinterface

import es.eci.utils.base.Loggable
import groovy.json.JsonSlurper

/**
 * Esta clase debe procesar un fichero rtc.json generado en la interfase de usuarios
 * mediante el script QuveUsersQuery.groovy
 */
public class RTCUsersReader extends Loggable {

	/**
	 * Este método debe procesar el fichero de información de RTC, y partiendo
	 * de la información disponible de sonar, reconstruir la información de grupos.
	 * @param rtcFile Fichero json con la información obtenida de RTC.
	 * @param sonarInfo Objeto previamente inicializado con la caché de grupos 
	 * 	y permisos de sonar.
	 * @return Lista resultante de actualizar los grupos existentes en Sonar con la 
	 * 	información de RTC.
	 */
	public List<SonarGroup> getGroups(File rtcFile, SonarPermissionsService sonarInfo) {
		SonarGroupsMerger merger = new SonarGroupsMerger(sonarInfo);
		merger.initLogger(this);
		List<SonarGroup> ret = [];
		def rtcObject = new JsonSlurper().parseText(rtcFile.text);
		// A efectos de esta interfase, un área de proyecto de RTC es un grupo
		//	de Sonar
		rtcObject.projectAreas.values().each { def projectArea -> 
			List<String> eciCodes = []
			projectArea.users.values().each { def user ->
				eciCodes << user.eciCode;
			}
			ret << merger.merge(projectArea.name, eciCodes);
		}
		return ret;
	} 
}
