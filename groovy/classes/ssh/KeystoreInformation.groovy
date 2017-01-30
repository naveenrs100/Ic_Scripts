package ssh

/**
 * Credenciales para uso de un keystore 
 */
class KeystoreInformation {

	//-------------------------------------------------------------
	// Propiedades de la clase
	
	// Usuario
	private String filename;
	// Password
	private String password;
	
	//-------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * @param filename Nombre de fichero dentro del zip del keystore
	 * @param password Password del keystore
	 */
	public KeystoreInformation(String filename, String password) {
		super();
		this.filename = filename;
		this.password = password;
	}
	
	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}
	
	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}
	
	
}
