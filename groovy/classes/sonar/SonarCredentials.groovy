/**
 * 
 */
package sonar

/**
 * Credenciales para sonar
 */
class SonarCredentials {

	//-------------------------------------------------------------
	// Propiedades de la clase
	
	// Usuario
	private String user;
	// Password
	private String password;
	
	//-------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Crea una instancia de credenciales (usuario/password) para autenticarse
	 * en Sonar.
	 * @param user Usuario de Sonar
	 * @param password Password de Sonar
	 */
	public SonarCredentials(String user, String password) {
		super();
		this.user = user;
		this.password = password;
	}
	
	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}
	
	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}
	
}
