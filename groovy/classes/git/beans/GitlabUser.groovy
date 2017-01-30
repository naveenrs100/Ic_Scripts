package git.beans

/**
 * Modela la información de un usuario de gitlab
 */
class GitlabUser {

	//------------------------------------------------
	// Propiedades del bean
	
	// Id de usuario (numérico)
	private long userId;
	// Nombre de usuario (X12345AB)
	private String userName;
	// Nombre mostrado en la aplicación (Pepe Pérez)
	private String userDisplayName;
	// Correo electrónico
	private String email;
	
	//------------------------------------------------
	// Métodos del bean
	
	/**
	 * @return Id de usuario (numérico)
	 */
	public long getUserId() {
		return userId;
	}
	
	/**
	 * @param userId Id de usuario (numérico)
	 * @param userName Nombre de usuario (X12345AB)
	 * @param userDisplayName Nombre mostrado en la aplicación (Pepe Pérez) 
	 * @param email Correo electrónico
	 */
	public GitlabUser(
			long userId, 
			String userName, 
			String userDisplayName,
			String email) {
		super();
		this.userId = userId;
		this.userName = userName;
		this.userDisplayName = userDisplayName;
		this.email = email;
	}
	
	/**
	 * @return Nombre de usuario (X12345AB)
	 */
	public String getUserName() {
		return userName;
	}
	
	/**
	 * @return Nombre mostrado en la aplicación (Pepe Pérez)
	 */
	public String getUserDisplayName() {
		return userDisplayName;
	}

	/**
	 * @return Correo electrónico
	 */
	public String getEmail() {
		return email;
	}
	
	
}
