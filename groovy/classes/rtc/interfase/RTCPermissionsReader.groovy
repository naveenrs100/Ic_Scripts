package rtc.interfase

import rtc.RTCClient
import es.eci.utils.ParameterValidator
import es.eci.utils.StringUtil;
import es.eci.utils.UtilUUID;
import es.eci.utils.base.Loggable
import groovy.json.JsonOutput;

/**
 * Esta clase se comunica con RTC para generar un formato JSON de intercambio
 * con la información de qué usuarios componen las project areas:
 * 
{
  "projectAreas": {
    "id_project_area_1": {
      "id": "id_project_area_1",
      "name": "la primera área de proyecto",
      "users": {
        "123456": {
          "eciCode": "123456",
          "name": "foo bar",
          "email": null
        },
        "654321": {
          "eciCode": "654321",
          "name": "bar baz",
          "email": null
        }
      }
    },
    "id_project_area_2": {
      "id": "id_project_area_2",
      "name": "la segunda área de proyecto",
      "users": {
        "123456": {
          "eciCode": "123456",
          "name": "foo bar",
          "email": null
        }
      }
    }
  }
}
 */
class RTCPermissionsReader extends Loggable {

	//-----------------------------------------------------------
	// Constantes del lector
	
	// Nombre por defecto del fichero
	private static final String PERMISSIONS_FILE = "permissions.json"
	
	//-----------------------------------------------------------
	// Propiedades del lector
	
	// URL de nexus
	private String nexusURL;
	// Versión de keystore
	private String keystoreVersion;
	// URL de RTC
	private String rtcURL;
	// Usuario de RTC
	private String rtcUser;
	// Password de RTC
	private String rtcPwd;
	// Directorio de trabajo
	private String parentWorkspace;
	// Opcional - nombre de fichero a generar
	private String targetFile;
	// Opcional - embellecer áreas de proyecto de Kiuwan
	private Boolean beautify = Boolean.FALSE;
	
	
	//-----------------------------------------------------------
	// Métodos del lector
	
	/**
	 * Construye un lector vacío
	 */
	public RTCPermissionsReader() {}

	// Añade un nombre de usuario a la lista, si no está en la misma
	private void process(users, teamMember) {
		def id = teamMember.userId[0].text();
		def name = teamMember.name[0].text();
		def email = teamMember.emailAddress[0].text();
		if (!users.find { it['eciCode'] == id } ) {
			users << [ "eciCode":id, "name":name, "email":email ]
		}
	}
	
	/** Genera el fichero de permisos consultando los servicios web de RTC. */
	public void execute() {
		// Parámetros obligatorios
		ParameterValidator.builder().
			add("nexusURL", nexusURL).
			add("keystoreVersion", keystoreVersion).
			add("rtcURL", rtcURL).
			add("rtcUser", rtcUser).
			add("rtcPwd", rtcPwd).
			add("parentWorkspace", parentWorkspace, {
					return !StringUtil.isNull(it) &&
						new File(it).exists() &&
						new File(it).isDirectory()
				}).build().validate();
			
		// Construye el cliente RTC
		RTCClient client = new RTCClient(rtcURL, rtcUser, rtcPwd, keystoreVersion, nexusURL);
		client.initLogger(this)
		String ret = client.get("rpt/repository/foundation", 
			["size":"1000",
			 "fields":"foundation/projectArea/(name|itemId|allTeamAreas/teamMembers/(userId|name|emailAddress)|teamMembers/(userId|name|emailAddress))"]);
		def xml = new XmlParser().parseText(ret)
		Map info = [:]
		Map projectAreas = [:]
		// Para cada projectArea, recuperar id y nombre
		// Recuperar todos los usuarios: teamMembers/userId
		// Recuperar todos los usuarios de cada área de equipo: allTeamAreas/teamMembers/userId
		// 	Las dos últimas listas deben aplanarse en una sola lista, eliminando
		//	repeticiones.
		xml.projectArea?.each { def projectArea ->
			def record = [:]
			def base64string = projectArea.itemId[0].text();
			// Recortar el '_' del principio
			record['id'] = UtilUUID.getUUIDFromBase64String(base64string, true)
			if (!beautify) {
				record['name'] = projectArea.name[0].text();
			}
			else {
				record['name'] = StringUtil.normalizeProjectArea(projectArea.name[0].text());
			}
			record['users'] = [:]
			def users = []
			projectArea.teamMembers?.each { def teamMember ->
				process(users, teamMember)
			} 
			projectArea.allTeamAreas?.each { def teamArea ->
				teamArea.teamMembers?.each { def tm ->
					process(users, tm)
				}
			}
			users.each { def user ->
				record['users'][user['eciCode']] = user;
			}
			projectAreas[record['id']] = record
		}
		info['projectAreas'] = projectAreas;
		// Escribir el resultado
		String fileName = PERMISSIONS_FILE;
		if (!StringUtil.isNull(targetFile)) {
			fileName = targetFile;
		}
		File f = new File(parentWorkspace, fileName);
		log "Escribiendo los resultados en ${f.canonicalPath} ..."
		f.text = JsonOutput.prettyPrint(JsonOutput.toJson(info));
	}
	
	/**
	 * @return the nexusURL
	 */
	public String getNexusURL() {
		return nexusURL;
	}

	/**
	 * @param nexusURL the nexusURL to set
	 */
	public void setNexusURL(String nexusURL) {
		this.nexusURL = nexusURL;
	}

	/**
	 * @return the keystoreVersion
	 */
	public String getKeystoreVersion() {
		return keystoreVersion;
	}

	/**
	 * @param keystoreVersion the keystoreVersion to set
	 */
	public void setKeystoreVersion(String keystoreVersion) {
		this.keystoreVersion = keystoreVersion;
	}

	/**
	 * @return the rtcURL
	 */
	public String getRtcURL() {
		return rtcURL;
	}

	/**
	 * @param rtcURL the rtcURL to set
	 */
	public void setRtcURL(String rtcURL) {
		this.rtcURL = rtcURL;
	}

	/**
	 * @return the rtcUser
	 */
	public String getRtcUser() {
		return rtcUser;
	}

	/**
	 * @param rtcUser the rtcUser to set
	 */
	public void setRtcUser(String rtcUser) {
		this.rtcUser = rtcUser;
	}

	/**
	 * @return the rtcPwd
	 */
	public String getRtcPwd() {
		return rtcPwd;
	}

	/**
	 * @param rtcPwd the rtcPwd to set
	 */
	public void setRtcPwd(String rtcPwd) {
		this.rtcPwd = rtcPwd;
	}

	/**
	 * @return the parentWorkspace
	 */
	public String getParentWorkspace() {
		return parentWorkspace;
	}

	/**
	 * @param parentWorkspace the parentWorkspace to set
	 */
	public void setParentWorkspace(String parentWorkspace) {
		this.parentWorkspace = parentWorkspace;
	}

	/**
	 * @return the targetFile
	 */
	public String getTargetFile() {
		return targetFile;
	}

	/**
	 * @param targetFile the targetFile to set
	 */
	public void setTargetFile(String targetFile) {
		this.targetFile = targetFile;
	}

	public Boolean getBeautify() {
		return beautify;
	}

	public void setBeautify(Boolean beautify) {
		this.beautify = beautify;
	}
	
	
}
