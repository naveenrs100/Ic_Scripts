package sonar.usersinterface

import java.util.Map;

import es.eci.utils.base.Loggable
import groovy.json.JsonSlurper
import sonar.SonarClient
import sonar.usersinterface.SonarPermissionTemplate.Permission;


/**
 * Esta clase modela la información de permisos contenida en una instancia de 
 * Sonar. 
 * 
 * Recupera y cachea los grupos de usuarios existentes en Sonar.
 * Recupera y cachea las plantillas de permisos existentes en Sonar.  Una plantilla
 * de permisos tiene:
 * + Grupos y usuarios con permisos para ejecutar cada funcionalidad de las 
 * 	disponibles (ver la lista PERMISSIONS)
 * + Un patrón con formato de expresión regular que indica a qué proyectos se
 * 	aplicará.
 * 
 * El objetivo de la política de permisos es:
 * + Replicar los grupos y usuarios existentes en los SCM corporativos en Sonar
 * + Crear plantillas de permisos que relacionen estos grupos con un patrón en
 * 	forma de expresión regular.
 * 
 * Cada vez que llegue un análisis a Sonar, se comparará el project.key que venga
 * 	anexo al análisis con los patrones de las plantillas de permisos existentes.  
 *  Si Sonar aprecia coincidencia, le aplicará al proyecto la plantilla o plantillas
 *  de permisos que coincidan con el patrón.  De esta forma, se prepara cada noche,
 *  independientemente de si existen análisis o no, la plantilla de permisos que se
 *  aplicará al proyecto cuando llegue el primer análisis.  
 *  
 * Así mismo, esta clase mantiene también los usuarios en los grupos de forma
 *  transparente a los proyectos que ya existan, para reflejar altas y bajas en 
 *  los grupos de gitlab y en las áreas de proyecto/equipo de RTC.
 */
public class SonarPermissionsService extends Loggable {

	//----------------------------------------------------------------
	// Constantes de la clase
	
	/** 
	 * Este mapa determina, para cada permiso existente, si
	 * el grupo de usuarios debe tener o no el permiso.
	 */
	public static final Map<String, Boolean> PERMISSIONS = 
		[ 'user':Boolean.TRUE, 
	// No se permite a los usuarios 'normales' administrar el proyecto en sonar
		  'admin':Boolean.FALSE,   
		  'issueadmin':Boolean.TRUE, 
		  'codeviewer':Boolean.TRUE, 
		  'scan':Boolean.TRUE ];
	
	//----------------------------------------------------------------
	// Propiedades de la clase
		
	// Caché de grupos
	private Map<String, SonarGroup> groups;
	// Caché de plantillas de permisos
	private Map<String, SonarPermissionTemplate> permissionTemplates;
	// Acceso a sonar
	private SonarClient client;	
	
	//----------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Construye la información existente en Sonar a partir de un cliente
	 * de servicios.
	 * @param client Cliente de servicios de sonar
	 */
	public SonarPermissionsService(SonarClient client) {
		this.client = client;
	}
	
	/**
	 * Devuelve un grupo de sonar obtenido de la caché de grupos.
	 * @param name Nombre de grupo buscado.
	 * @param description Opcional.  Descripción del grupo.
	 * @return Instancia del grupo que se busca.
	 */
	public SonarGroup group(String name, String description = null) {
		SonarGroup group = null;
		if (!groups.containsKey(name)) {
			group = new SonarGroup(name, false);
			if (description != null) {
				group.setDescription(description);
			}
			groups[name] = group;
		}
		else {
			group = groups[name];
		}
		return group;
	}
	
	/**
	 * Devuelve una plantilla de permisos obtenida de la caché de plantillas.
	 * @param name Nombre de la plantilla buscada.
	 * @return Instancia de la plantilla buscada.
	 */
	public SonarPermissionTemplate template(String name) {
		SonarPermissionTemplate template = null;
		if (!permissionTemplates.containsKey(name)) {
			template = new SonarPermissionTemplate(name, "", false);
			permissionTemplates[name] = template;
		}
		else {
			template = permissionTemplates[name];
		}
		return template;
	}
	
	/**
	 * Este método carga los grupos existentes en Sonar, con los usuarios 
	 * correspondientes.
	 */
	public void loadGroups() {
		this.groups = new HashMap<String, SonarGroup>()
		String groupsJson = client.get("user_groups/search", [:]);
		log groupsJson
		def groupsObject = new JsonSlurper().parseText(groupsJson);
		groupsObject.groups?.each { def group ->
			List<String> users = getGroupUsers(group.name);
			this.groups.put(group.name, new SonarGroup(group.name, users));
		}
	}
	
	/**
	 * Devuelve la lista de usuarios de un grupo.
	 * @param group Nombre del grupo.
	 * @return Lista de eciCodes de los usuarios del grupo.
	 */
	private List<String> getGroupUsers(String group) {
		List<String> users = []
		String usersJson = client.get("user_groups/users", ["name":group, "ps":600]);
		def usersObject = new JsonSlurper().parseText(usersJson);
		usersObject.users?.each { def user ->
			users << user.login;
		}
		return users;
	}
	
	/**
	 * Devuelve la lista de usuarios asociados a una plantilla y permiso concreto.
	 * @param templateName Nombre de la plantilla.
	 * @param permissionName Nombre del permiso concreto.
	 * @return Usuarios asociados a una plantilla y permiso concreto.
	 */
	private List<String> getTemplateUsers(String templateName, String permissionName) {
		String usersJson = client.get("permissions/template_users",
			["permission":permissionName,
			 "templateName":templateName]);
		def usersObject = new JsonSlurper().parseText(usersJson);
		List<String> users = [];
		usersObject.users?.each { def user ->
			users << user.login;
		}
		return users;
	}
	
	/**
	 * Devuelve la lista de grupos asociados a una plantilla y permiso concreto.
	 * @param templateName Nombre de la plantilla.
	 * @param permissionName Nombre del permiso concreto.
	 * @return Grupos asociados a una plantilla y permiso concreto.
	 */
	private List<SonarGroup> getTemplateGroups(String templateName, String permissionName) {
		String groupsJson = client.get("permissions/template_groups",
			["permission":permissionName,
			 "templateName":templateName]);
		def groupsObject = new JsonSlurper().parseText(groupsJson);
		List<SonarGroup> groups = []
		groupsObject.groups?.each { def groupRecord ->
			SonarGroup g = group(groupRecord.name);
			groups << g;
		}
		return groups;
	}

	/**
	 * Este método devuelve la información cacheada de usuarios y grupos en
	 * 	la instancia de Sonar.
	 * @return Grupos de permisos definidos en sonar
	 */
	public Collection<SonarGroup> getGroupsList() {
		return this.groups?.values();
	}
	
	/** 
	 * Este método carga las plantillas existentes en Sonar.
	 */
	public void loadPermissionTemplates() {
		this.permissionTemplates = new HashMap<String, SonarPermissionTemplate>()
		String templatesJson = client.get("permissions/search_templates", [:]);
		log templatesJson
		def templatesObject = new JsonSlurper().parseText(templatesJson);
		templatesObject.permissionTemplates?.each { def template ->
			Map<String, Permission> permissions = new HashMap<String, Permission>();
			PERMISSIONS.keySet().each { String permissionName ->
				Permission p = new Permission();
				// Grupos de la plantilla
				List<SonarGroup> groups = getTemplateGroups(template.name, permissionName)
				groups.each { SonarGroup g ->
					p.addGroup(g);
				}
				// Usuarios de la plantilla
				List<String> users = getTemplateUsers(template.name, permissionName)
				users.each { String user ->
					p.addUser(user);
				}
				if (users.size() > 0 || groups.size() > 0) {
					permissions[permissionName] = p;
				}
			}
			this.permissionTemplates.put(template.name, 
				new SonarPermissionTemplate(template.name, 
					template.projectKeyPattern, permissions));
		}
	}
	
	/**
	 * Este método devuelve la información cacheada de plantillas de permisos en
	 * 	la instancia de Sonar.
	 * @return Plantillas de permisos definidas en sonar
	 */
	public Collection<SonarPermissionTemplate> getTemplatesList() {
		return this.permissionTemplates?.values();
	}
	
	/**
	 * Este método crea o actualiza una plantilla de permisos.
	 * @param t Plantilla de permisos en Sonar.
	 */
	public void createTemplate(SonarPermissionTemplate t) {
		try {
			// Crear la plantilla
			client.post("permissions/create_template", [
				"name":t.getName(),
				"projectKeyPattern":t.getPattern()]);
			// Añadirle los usuarios y los grupos
			t.getPermissions().keySet().each { String permissionName ->
				Permission p = t.getPermissions().get(permissionName);
				// Usuarios
				p.getUsers().each { String eciCode ->
					client.post("permissions/add_user_to_template", [
							"templateName":t.getName(),
							"login":eciCode,
							"permission":permissionName
						]);
				}
				// Grupos
				p.getGroups().each { SonarGroup group ->
					client.post("permissions/add_group_to_template", [
							"templateName":t.getName(),
							"groupName":group.getName(),
							"permission":permissionName
						]);
				}
			}
		}
		catch (Exception e) {
			log("WARNING: No se puede dar de alta la "
				+ "plantilla de permisos ${t.getName()}");
		}
	}
	
	/**
	 * Este método crea o actualiza un grupo en Sonar
	 * @param g Grupo sonar con su lista de usuarios.
	 */
	public void saveGroup(SonarGroup g) {
		if (g.doesExistInSonar()) {
			if (g.isModified()) {
				// Actualizar
				log "Actualizando el grupo ${g.name} ..."
				List<String> existingUsers = getGroupUsers(g.getName());
				List<String> newUsers = g.getUsers();
				// Añadir los nuevos
				newUsers.each { String eciCode ->
					if (!existingUsers.contains(eciCode)) {
						addUserToGroupInSonar(g, eciCode)
					}
				}
				// Eliminar los que falten
				existingUsers.each { String eciCode ->
					if (!newUsers.contains(eciCode)) {
						removeUserFromGroupInSonar(g, eciCode);
					}
				}
			}
		}
		else {
			log "Creando el grupo ${g.name} ..."
			// Crear
			client.post("user_groups/create", [
				"name":g.getName(),
				"description": 
					(g.getDescription()==null?
						"Creado en la interfase de usuarios y grupos"
						:g.getDescription())
				]);
			g.getUsers().each { String eciCode ->
				addUserToGroupInSonar(g, eciCode)
			}
		}
	}

	/** 
	 * Este método añade un usuario a un grupo en sonar.
	 * @param g Grupo sonar.
	 * @param eciCode Código de usuario ECI.
	 */
	private void addUserToGroupInSonar(SonarGroup g, String eciCode) {
		try {
			client.post("user_groups/add_user", [
				"name": g.getName(),
				"login": eciCode
			]);
		}
		catch(Exception e) {
			log("WARNING: No se puede dar de alta el "
					+ "usuario $eciCode en el grupo ${g.name}");
		}
	}
	
	/** 
	 * Este método elimina un usuario de un grupo de sonar.
	 * @param g Grupo sonar.
	 * @param eciCode Código de usuario ECI.
	 */
	private void removeUserFromGroupInSonar(SonarGroup g, String eciCode) {
		try {
			client.post("user_groups/remove_user", [
				"name": g.getName(),
				"login": eciCode
				]);
		}
		catch(Exception e) {
			log("WARNING: No se puede eliminar el "
				+ "usuario $eciCode del grupo ${g.name}");
		}
	}
}
