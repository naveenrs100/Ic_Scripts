package atlassian

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import es.eci.utils.base.Loggable
import es.eci.utils.ParameterValidator
import es.eci.utils.Stopwatch

class ExtractUsersAndProjects extends Loggable {
	
	private String urlAtlassian;				// URL de JIRA
	private String atlassianKeystoreVersion;	// Versión del keysotre
	private String atlassianUser;				// Usuario de JIRA
	private String atlassianPass;				// Password de JIRA
	
	private ProjectAreasBean finalJson = new ProjectAreasBean();
	
	public void execute () {
		
		// Validación de obligatorios
		ParameterValidator.builder()
			.add("urlAtlassian", urlAtlassian)
			.add("atlassianKeystoreVersion", atlassianKeystoreVersion)
			.add("atlassianUser", atlassianUser)
			.add("atlassianPass", atlassianPass).build().validate();
	
		long millis = Stopwatch.watch {
			
			// Establecer la conexión
			AtlassianClient aClient = 
				new AtlassianClient(
					urlAtlassian,
					AtlassianClient.JIRA_CERTIFICATE, 
					atlassianKeystoreVersion, 
					atlassianUser, 
					atlassianPass)
			
			// Obtener el mapa global de proyectos
			String responseProjects = aClient.get("project", "2")
			JsonArray mainJsonProjects = new JsonParser().parse(responseProjects).getAsJsonArray()
			
			// Iterar por cada proyecto
			Iterator<JsonElement> projectElement = mainJsonProjects.iterator()
			while (projectElement.hasNext()) {
				
				long millis_two = Stopwatch.watch {
				
					JsonObject projectJson = (JsonObject) projectElement.next()
				
					// Creamos un nuevo objeto de projectArea
					ProjectAreaBean projectArea = new ProjectAreaBean()
					
					String uuid = new UUID(0, Long.parseLong(projectJson.get("id").getAsString())).toString()
					
					projectArea.setId(uuid)
					projectArea.setName(projectJson.get("name").getAsString())
					finalJson.getProjectAreas().put(uuid, projectArea)
			
					// Obtener id de esquema de permisos por id de proyecto.
					String responseScheme = aClient.get("project/" + projectJson.get("id").getAsString() +
						"/permissionscheme", "2")
					JsonObject schemeObject = new JsonParser().parse(responseScheme).getAsJsonObject()
					// log " --> Scheme id: " + schemeObject.get("id").getAsString()
					// ---
			
					// Obtener esquema de permisos
					HashMap<String,String> paramsScheme = new HashMap<String, String>()
					paramsScheme.put("expand", "all")
					String responsePermissions = aClient.get("permissionscheme/" + 
						schemeObject.get("id").getAsString(), "2", paramsScheme)
					JsonObject mainSchemePermissions = new JsonParser().parse(responsePermissions).getAsJsonObject()
					JsonArray mainSchemePermArray = mainSchemePermissions.get("permissions").getAsJsonArray()
			
					// Iterar por el esquema de permisos
					Iterator<JsonElement> schemePermElement = mainSchemePermArray.iterator()
					while (schemePermElement.hasNext()) {
						JsonObject schemePermJson = (JsonObject) schemePermElement.next()
						JsonObject holderJson = schemePermJson.get("holder")
				
						if ( holderJson.get("type").getAsString().equals("user") ) {
							JsonObject userJson = holderJson.get("user")
							// Añadimos los usuarios al projectArea creado anteriormente
							UsersBean user = new UsersBean()
							user.setEciCode(userJson.get("name").getAsString())
							user.setName(userJson.get("displayName").getAsString())
							user.setEmail(userJson.get("emailAddress").getAsString())
							projectArea.getUsers().put(userJson.get("name").getAsString(), user)
						}
				
						// Obtener grupo y esquema de grupo [Eliminar este if en caso de que los grupos
						// no sean necesarios en la extracción]
						if ( holderJson.get("type").getAsString().equals("group") ) {
							JsonObject groupJson = holderJson.get("group")
							// log "    --> Group name: " + groupJson.get("name").getAsString()
					
							HashMap<String,String> paramsGroup = new HashMap<String, String>()
							paramsGroup.put("groupname", groupJson.get("name").getAsString())
							paramsGroup.put("expand", "users")
							String responseGroup = aClient.get("group", "2", paramsGroup)
					
							JsonObject mainGroup = new JsonParser().parse(responseGroup).getAsJsonObject()
							JsonObject usersGroup = mainGroup.get("users").getAsJsonObject()
							JsonArray usersGroupArray = usersGroup.get("items").getAsJsonArray()
						
							// Iterar por los usuarios que componen el grupo
							Iterator<JsonElement> usersGroupElement = usersGroupArray.iterator()
							while (usersGroupElement.hasNext()) {
								JsonObject userJsonFromGroup = (JsonObject) usersGroupElement.next()

								// Si el usuario no existe, se añade
								if ( projectArea.getUsers().get(userJsonFromGroup.get("name").getAsString()) == null ) {
									UsersBean userFromGroup = new UsersBean()
									userFromGroup.setEciCode(userJsonFromGroup.get("name").getAsString())
									userFromGroup.setName(userJsonFromGroup.get("displayName").getAsString())
									userFromGroup.setEmail(userJsonFromGroup.get("emailAddress").getAsString())
									projectArea.getUsers().put(userJsonFromGroup.get("name").getAsString(), userFromGroup)
								}
							}
						} // Fin eliminar if de grupo
				
					} // Iteraciçon esquema de permisos
					
					log "Project [ " + projectJson.get("name").getAsString() + " ] added"
				
				} // Time millis_two
				
				log "Time spent: " + millis_two + " milliseconds."
			
			}
		
			// Pretty print
			Gson gson = new GsonBuilder().setPrettyPrinting().create()			
			System.out.println(gson.toJson(finalJson))
			
		}
		
		log "Tiempo total: ${millis} mseg."
		
	}

	/**
	 * @return the urlAtlassian
	 */
	public String getUrlAtlassian() {
		return urlAtlassian;
	}

	/**
	 * @param urlAtlassian the urlAtlassian to set
	 */
	public void setUrlAtlassian(String urlAtlassian) {
		this.urlAtlassian = urlAtlassian;
	}

	/**
	 * @return the atlassianKeystoreVersion
	 */
	public String getAtlassianKeystoreVersion() {
		return atlassianKeystoreVersion;
	}

	/**
	 * @param atlassianKeystoreVersion the atlassianKeystoreVersion to set
	 */
	public void setAtlassianKeystoreVersion(String atlassianKeystoreVersion) {
		this.atlassianKeystoreVersion = atlassianKeystoreVersion;
	}

	/**
	 * @return the atlassianUser
	 */
	public String getAtlassianUser() {
		return atlassianUser;
	}

	/**
	 * @param atlassianUser the atlassianUser to set
	 */
	public void setAtlassianUser(String atlassianUser) {
		this.atlassianUser = atlassianUser;
	}

	/**
	 * @return the atlassianPass
	 */
	public String getAtlassianPass() {
		return atlassianPass;
	}

	/**
	 * @param atlassianPass the atlassianPass to set
	 */
	public void setAtlassianPass(String atlassianPass) {
		this.atlassianPass = atlassianPass;
	}

}
