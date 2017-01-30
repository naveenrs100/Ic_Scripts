package sonar.usersinterface

/**
 * Esta clase modela un grupo de usuarios de Sonar, con los permisos y usuarios
 * relacionados.
 */
class SonarGroup {

	//----------------------------------------------
	// Propiedades del bean
	
	// Lista de usuarios
	private List<String> users;
	// Nombre del grupo
	private String name;
	// ¿Leído de sonar?
	private boolean existsInSonar = false;
	// ¿Modificado?
	private boolean modified = false;
	
	
	//----------------------------------------------
	// Métodos del bean
	
	/**
	 * Construye un grupo vacío
	 */
	SonarGroup(String name, boolean existsInSonar = false) {
		super();
		this.name = name;
		this.existsInSonar = existsInSonar;
		users = new LinkedList<String>();
	}
	
	/**
	 * Construye un grupo con una lista de usuarios, que consta como
	 * original de sonar.
	 * @param name Nombre del grupo.
	 * @param users Lista de usuarios.
	 */
	SonarGroup(String name, List<String> users) {
		this(name, true);
		this.users = users; 
	}
	
	/**
	 * Este método añade un usuario a la lista de usuarios 
	 * @param user Usuario sonar
	 */
	public void addUser(String user) {
		if (users != null && !users.contains(user)) {
			users << user;
			modified = true;
		}
	}
	
	/**
	 * Elimina un usuario de la lista.
	 * @param user Usuario sonar
	 */
	public void removeUser(String user) {
		if (users != null && users.contains(user)) {
			users.remove(user);
			modified = true;
		}
	}

	/**
	 * @return the users
	 */
	public List<String> getUsers() {
		return users;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the existsInSonar
	 */
	public boolean doesExistInSonar() {
		return existsInSonar;
	}

	/**
	 * @return the modified
	 */
	public boolean isModified() {
		return modified;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[ GROUP -->\n");
		sb.append("name: ${this.name} \n");
		sb.append("users:\n")
		for (String eciCode: this.users) {
			sb.append(" $eciCode \n");
		}
		sb.append("]\n");
		return sb.toString();
	}
}
