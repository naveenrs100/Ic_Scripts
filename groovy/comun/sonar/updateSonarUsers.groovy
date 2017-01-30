@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.4')

import java.beans.XMLDecoder

import rtc.RTCProjectArea
import rtc.RTCUtils
import sonar.SonarClient
import sonar.SonarCredentials
import sonar.usersinterface.GitUsersReader
import sonar.usersinterface.RTCUsersReader
import sonar.usersinterface.SonarGroup
import sonar.usersinterface.SonarInstancePermissionsInfo
import sonar.usersinterface.SonarPermissionTemplate
import es.eci.utils.ParameterValidator
import es.eci.utils.Stopwatch
import es.eci.utils.SystemPropertyBuilder
import git.GitlabClient

/**
 * Interfase de usuarios y permisos desde RTC/Gitlab hacia Sonar.  
 * Ejecutarse periódicamente como groovy script desde Jenkins.
 * 
 * Crea o actualiza las plantillas de permisos y los grupos de usuarios en sonar.  
 * Estas plantillas quedan asociadas a un grupo de sonar y a un patrón relacionado 
 * con un nombre de grupo (que es a su vez el de la AP de RTC o el del grupo de Gitlab).
 * 
 * Cada análisis que venga de Jenkins hacia Sonar tendrá que tener una projectKey que empiece 
 * por este patrón para que al nuevo proyecto creado se le aplique la plantilla de
 * permisos correspondiente. 
 * 
 * Parámetros:
 * 
 * URL de Sonar (parámetro global de Jenkins)
 * Usuario administrador de Sonar (parámetro global de Jenkins)
 * Password administrador de Sonar (parámetro global de Jenkins)
 * URL de gitlab (parámetro global de Jenkins)
 * Token privado de administración de Gitlab (parámetro global de Jenkins)
 * URL de Nexus, para recuperar los keystores
 * Versión del keystore de Gitlab
 * Versión del keystore de Sonar
 * URL de RTC (parámetro global de Jenkins).  Se utiliza para consultar el UUID para las
 * 	permission templates
 * Usuario de RTC (parámetro global de Jenkins)
 * Password de RTC (parámetro global de Jenkins)
 * 
 * Establece la siguiente relación:
 * 
 * AP/Teamarea de RTC <-> Grupo de Sonar
 * Grupo de gitlab <-> Grupo de Sonar
 * Crea plantillas de permisos con los patrones y grupos asociados oportunos.
 * 
 * El rtc.json da la relación en RTC de usuarios con AP/áreas de equipo.
 * El script debe extraer de gitlab la relación de usuarios con grupos. 
 * 
 */

// Verificar que tiene el fichero rtc.json y el git.xml
assert new File("rtc.json").exists()
assert new File("git.xml").exists()

SystemPropertyBuilder builder = new SystemPropertyBuilder();
Map<String, String> params = builder.getSystemParameters();

String sonarURL 		= params['sonarURL']
String sonarUser 		= params['sonarUser']
String sonarPwd 		= params['sonarPwd']
 
String gitlabURL		= params['gitlabURL']
String gitlabSecret 	= params['gitlabSecret']
String gitlabKeystore 	= params['gitlabKeystore']

String nexusURL			= params['nexusURL']
String sonarKeystore 	= params['sonarKeystore']

String rtcURL			= params['rtcURL']
String rtcUser			= params['rtcUser']
String rtcPwd			= params['rtcPwd']
 
ParameterValidator.builder().
	add("sonarURL", sonarURL).
	add("sonarUser", sonarUser).
	add("sonarPwd", sonarPwd).
	add("gitlabURL", gitlabURL).
	add("gitlabSecret", gitlabSecret).
	add("gitlabKeystore", gitlabKeystore).
	add("nexusURL", nexusURL).
	add("sonarKeystore", sonarKeystore).
	add("rtcURL", rtcURL).
	add("rtcUser", rtcUser).
	add("rtcPwd", rtcPwd)
	.build().validate();

long millis = -1;
	
try {
	millis = Stopwatch.watch {
		SonarCredentials creds = new SonarCredentials(sonarUser, sonarPwd);
		SonarClient client = new SonarClient(creds, sonarURL, sonarKeystore, nexusURL);
		client.initLogger { println it }
		SonarInstancePermissionsInfo info = new SonarInstancePermissionsInfo(client);
		info.initLogger { println it }
		
		// Recuperar la información actual de grupos y plantillas en la instancia 
		//	de sonar		
		info.loadGroups();
		println info.getGroupsList();
		
		info.loadPermissionTemplates();
		println info.getTemplatesList();
		
		// Reunir la información de grupos de RTC
		// Se asume la presencia de un fichero rtc.json con la información		
		updateRTCInfo(info, rtcUser, rtcPwd, rtcURL);
		
		// Reunir la información de usuarios y grupos de gitlab
		GitlabClient gitlabClient = 
			new GitlabClient(
				gitlabURL, gitlabSecret, gitlabKeystore, nexusURL);
		gitlabClient.initLogger { println it }
		updateGitlabInfo(gitlabClient, info);
		
	}
}
catch(Exception e) {
	e.printStackTrace();
	throw e;
}
finally {
	if (millis >= 0) {
		println "Interfase completada en $millis mseg."
	}
}

// Este método toma la información de usuarios y grupos de gitlab y la actualiza
//	sobre sonar.
private void updateGitlabInfo(GitlabClient client, SonarInstancePermissionsInfo info) {
	// Recuperar la información tomada de jenkins en el script gitProjectAreaUuidReader.groovy.
	//	Este script ha dejado en un fichero git.xml la relación entre
	//	los UUID de áreas de proyecto y los jobs de grupo de git en Jenkins.
	XMLDecoder d = new XMLDecoder(
		new BufferedInputStream(
			new FileInputStream("git.xml")));	
	Map<String, String> gitGroupsPA = (Map<String, String>) d.readObject();
	d.close();
	// Lectura de git
	GitUsersReader reader = new GitUsersReader();
	reader.initLogger { println it }
	List<SonarGroup> mergedGitGroups = reader.getGroups(client, info);
	// Crea los grupos mezclados en Sonar
	mergedGitGroups.each {  info.saveGroup(it) }
	// Actualizar las permissionTemplates, para aquellas de las que se dispone de
	//	relación con algún área de proyecto
	// Actualizar las plantillas, una por cada grupo
	savePermissionTemplates(mergedGitGroups, gitGroupsPA, info)	
}

// Este método toma la información del fichero rtc.json para actualizar la
//	información de grupos, usuarios y plantillas de permisos en Sonar
private void updateRTCInfo(
		SonarInstancePermissionsInfo info,
		String rtcUser, 
		String rtcPwd,
		String rtcURL) {
	// Áreas de proyecto de RTC
	RTCUtils utils = new RTCUtils();
	utils.initLogger { println it }
	Map<String, RTCProjectArea> projectAreas =
		utils.getProjectAreas(rtcUser, rtcPwd, rtcURL);
	// Convertir la estructura de datos a un simple map de nombre -> UUID
	Map<String, String> projectAreasUUID = [:]
	projectAreas.keySet().each { String projectAreaName ->
		projectAreasUUID.put(projectAreaName, 
			projectAreas.get(projectAreaName).getUuid());
	}
	RTCUsersReader rtcReader = new RTCUsersReader();
	rtcReader.initLogger { println it }
	// Leer el json y mezclarlo con la información existente
	// Asume que en el directorio de ejecución se encuentra el fichero rtc.json
	List<SonarGroup> mergedRTCGroups = rtcReader.getGroups(new File("rtc.json"), info);
	// Crea los grupos mezclados en Sonar
	mergedRTCGroups.each {  info.saveGroup(it) }
	// Actualizar las plantillas, una por cada grupo
	savePermissionTemplates(mergedRTCGroups, projectAreasUUID, info)
}

// Este método actualiza en Sonar los plantillas de permisos.
// Cada plantilla de permisos debe quedar relacionada con un UUID de área de proyecto
//	venga del SCM que venga, si no se puede se emite un WARNING
private savePermissionTemplates(
		List<SonarGroup> mergedGroups, 
		Map<String, String> projectAreas, 
		SonarInstancePermissionsInfo info) {
	mergedGroups.each { SonarGroup group ->
		String scmName = group.getName().replaceAll(" - users", "");
		SonarPermissionTemplate template =
				info.template(scmName + " - permissionTemplate");
		if (!template.doesExistsInSonar()) {
			// Consultar a RTC el UUID del área de proyecto
			String uuid = projectAreas[scmName];
			if  (uuid != null) {
				// Se debe convertir el nombre de grupo en un patrón válido, escapando
				//	cualquier carácter propio de una regexp
				template.setPattern(
						uuid.
						replaceAll('([|\\\\{}\\(\\)\\[\\]^$+*?.])', "\\\\\$1")
						+ "\\..*");
				SonarInstancePermissionsInfo.PERMISSIONS.each { String permissionName ->
					template.addPermissionGroup(permissionName, group);
					// Por defecto, sonar-administrators debe tener siempre permisos
					template.addPermissionGroup(permissionName, 
						info.group("sonar-administrators"));
				}
				info.createTemplate(template);
			}
			else {
				println "WARNING - PROJECT AREA UUID - No se puede relacionar $scmName con ningún área de proyecto"
			}
		}
	}
}