/**
 * 
 */
package sonar.usersinterface

/**
 * Esta clase modela una plantilla de permisos en Sonar.
 */
class SonarPermissionTemplate {

	//------------------------------------------------------------
	// Propiedades del bean
	
	// Nombre de la plantilla
	private String name;
	// Permisos de la plantilla
	// De la documentación de sonar:
	// Possible values for project permissions user, admin, issueadmin, codeviewer, scan
	private Map<String, Permission> permissions;
	// Expresión regular para relacionarla con un project key
	private String pattern;
	// ¿Leído de sonar?
	private boolean existsInSonar = false;
	
	//------------------------------------------------------------
	// Métodos del bean
	
	/**
	 * Construye una plantilla informada con patrón y permisos.
	 * @param name Nombre de la plantilla.
	 * @param pattern Patrón de proyecto sobre el que se usará la plantilla.
	 * @param permissions Tabla de permisos
	 */
	SonarPermissionTemplate(String name, String pattern, Map<String, Permission> permissions) {
		this(name, pattern, true);
		this.permissions = permissions;
	}
	
	/**
	 * Construye una plantilla vacía.
	 * @param name Nombre de la plantilla.
	 * @param pattern Patrón de proyecto sobre el que se usará la plantilla.
	 */
	SonarPermissionTemplate(String name, String pattern, boolean existsInSonar = false) {
		this.name = name;
		this.pattern = pattern;
		this.existsInSonar = existsInSonar;
		permissions = [:]
	}
	
	// Devuelve un permiso del mapa, o lo añade si no existe
	private Permission getPermission(String permissionName) {
		Permission ret = null;
		if (!permissions.containsKey(permissionName)) {
			ret = new Permission();
			permissions.put(permissionName, ret);
		}
		else {
			ret = permissions[permissionName];
		}
		return ret;
	}
	
	/**
	 * Añade un usuario a un permiso de la plantilla.
	 * @param permissionName Nombre del permiso.
	 * @param user Usuario a añadir.
	 */
	public void addPermissionUser(String permissionName, String user) {
		Permission permission = getPermission(permissionName);
		permission.addUser(user);
	}
	
	/**
	 * Añade un grupo a un permiso de la plantilla.
	 * @param permissionName Nombre del permiso.
	 * @param group Grupo a añadir.
	 */
	public void addPermissionGroup(String permissionName, SonarGroup group) {
		Permission permission = getPermission(permissionName);
		permission.addGroup(group);
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the permissions
	 */
	public Map<String, Permission> getPermissions() {
		return permissions;
	}

	/**
	 * @return the pattern
	 */
	public String getPattern() {
		return pattern;
	}	

	/**
	 * @param pattern the pattern to set
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}	

	/**
	 * @return the existsInSonar
	 */
	public boolean doesExistsInSonar() {
		return existsInSonar;
	}
	
	//------------------------------------------------------------
	// Clases privadas del bean	

	/**
	 * Esta clase modela una lista de usuarios y grupos con un determinado 
	 * permiso.
	 */
	private class Permission {
		//------------------------------------------
		// Propiedades del bean
		
		// Lista de usuarios
		private List<String> users;
		// Lista de grupos
		private List<SonarGroup> groups;
		
		//------------------------------------------
		// Métodos del bean
		
		// Crea un permiso vacío
		public Permission() {
			users = new LinkedList<String>();
			groups = new LinkedList<SonarGroup>();
		}
		
		// Añade un usuario al permiso
		public void addUser(String user) {
			users << user;
		}
		
		// Añade un grupo al permiso
		public void addGroup(SonarGroup group) {
			groups << group;			
		}

		/**
		 * @return the users
		 */
		public List<String> getUsers() {
			return users;
		}

		/**
		 * @return the groups
		 */
		public List<SonarGroup> getGroups() {
			return groups;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[ PERMISSION TEMPLATE -->\n");
		sb.append("name: ${this.name} \n");
		sb.append("projectKeyPattern: ${this.pattern} \n");
		sb.append("Permissions:\n");
		for(String permissionName: permissions.keySet()) {
			Permission permission = permissions[permissionName];
			sb.append(" ${permissionName}:\n")
			// Grupos de la plantilla
			sb.append("  Groups:\n");
			for (SonarGroup g: permission.getGroups()) {
				sb.append("    ${g.getName()}\n");
			}
			// Usuarios de la plantilla
			sb.append("  Users:\n");
			for (String eciCode: permission.getUsers()) {
				sb.append("    ${eciCode}\n");
			}
		}
		sb.append("]\n");
		return sb.toString();
	}
}
