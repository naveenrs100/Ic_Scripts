package jira

import ssh.SecureRESTClientHelper
import es.eci.utils.ParameterValidator
import es.eci.utils.base.Loggable

/**
 * Esta clase encapsula la funcionalidad de comunicación con el servidor JIRA
 * de ECI
 */
class JiraClient extends Loggable {

	//-----------------------------------------------------------------
	// Constantes del cliente
	
	// Coordenadas del keystore en Nexus
	private static final String KEYSTORE_GROUPID = "es.eci.jira";
	// Acceso SSL
	private static final String KEYSTORE_ARTIFACTID = "keystore";
	private static final String KEYSTORE_FILENAME = "jira.jks";
	private static final String KEYSTORE_PASSWORD = "changeit";
	
	//-----------------------------------------------------------------
	// Propiedades del cliente
	
	/** URL de gitlab. */
	private String jiraURL;
	/** Autenticación basic */
	private String user;
	private String pass;
	/** Versión del certificado de jira (se bajará de Nexus). */
	private String keystoreVersion = "1.0.0";
	/** URL de Nexus. */
	private String nexusURL = null;
	/** Caché de keystore. */	
	private SecureRESTClientHelper secureHelper = new SecureRESTClientHelper();
	
	//------------------------------------------------------------------
	// Métodos del cliente
	
	/**
	 * Crea un cliente inicializado para lanzar peticiones a JIRA
	 * @param jiraURL URL de JIRA
	 * @param user Usuario de JIRA
	 * @param pass Password de usuario en JIRA
	 * @param keystoreVersion Versión del keystore en Nexus
	 * @param nexusURL URL de Nexus
	 */
	public JiraClient(
			String jiraURL, 
			String user, 
			String pass, 
			String keystoreVersion, 
			String nexusURL) {
			
		ParameterValidator.builder().
			add("jiraURL", jiraURL).
			add("user", user).
			add("pass", pass).
			add("keystoreVersion", keystoreVersion).
			add("nexusURL", nexusURL).
				build().validate();
				
		this.jiraURL = jiraURL;
		this.user = user;
		this.pass = pass;
		this.keystoreVersion = keystoreVersion;
		this.nexusURL = nexusURL;
	}
			
	
}
