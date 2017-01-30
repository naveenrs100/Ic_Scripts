package ldap

import javax.naming.Context
import javax.naming.NamingEnumeration
import javax.naming.directory.Attribute
import javax.naming.directory.DirContext
import javax.naming.directory.SearchControls
import javax.naming.directory.SearchResult
import javax.naming.ldap.InitialLdapContext

import es.eci.utils.ParameterValidator
import es.eci.utils.base.Loggable

class LDAPClient extends Loggable {

	//---------------------------------------------
	// Constantes del cliente
	
	// Base de la búsqueda LDAP
	private static final String LDAP_BASE_QUERY = 
		"OU=Servicios Centrales,OU=El Corte Ingles,DC=grupoeci,DC=elcorteingles,DC=corp";
	
	
	//---------------------------------------------
	// Propiedades del cliente
	
	// Host de LDAP
	private String url;
	// Autorización para consultar LDAP
	private String user;
	private String password;
	// Tipo de autenticación con LDAP
	private String authentication = "simple";
	
	// Caché del contexto LDAP
	private DirContext ctx = null;
	
	//---------------------------------------------
	// Métodos del cliente
	
	/**
	 * Construye un cliente de LDAP contra las coordenadas indicadas.
	 * @param ldapURL URL de LDAP.
	 * @param port Puerto a usar para la conexión.
	 * @param userLDAP Usuario de LDAP que hace la conexión.
	 * @param passwordLDAP Password del usuario de LDAP.
	 * @param authentication Tipo de autenticación (OPCIONAL).
	 */
	public LDAPClient(String ldapURL, 
					  String ldapUser, 
					  String ldapPassword, 
					  String authentication = null) {
					  
		ParameterValidator.builder().
			add("url", ldapURL).
			add("user", ldapUser).
			add("password", ldapPassword).
				build().validate();
				
		this.url = ldapURL;
		this.user = ldapUser;
		this.password = ldapPassword;
		if (authentication != null) {
			this.authentication = authentication;
		}
		// Inicializar el contexto de LDAP
		Hashtable<String, Object> env = new Hashtable<String, Object>();
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, this.user);
		env.put(Context.SECURITY_CREDENTIALS, this.password);
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, url);

		//ensures that objectSID attribute values
		//will be returned as a byte[] instead of a String
		env.put("java.naming.ldap.attributes.binary", "objectSID");
		
		// the following is helpful in debugging errors
		//env.put("com.sun.jndi.ldap.trace.ber", System.err);
		
		ctx = new InitialLdapContext(env);
	}
		
	/**
	 * Este método devuelve un registro por id de usuario.
	 * @return Registro de LDAP por id de usuario.
	 */
	public LDAPRecord getByUserName(String userName) {
		LDAPRecord ret = null;
		List<SearchResult> results = queryLDAP("sAMAccountName=${userName}");
		if (results == null || results.size() > 1) {
			throw new Exception("Error en la búsqueda")
		}
		else if (results.size() == 1) {
			ret = populateRecord(results.get(0))
		}
		return ret;
	}
	
	// Compone un registro a partir de un resultado de la búsqueda
	private LDAPRecord populateRecord(SearchResult result) {
		LDAPRecord record = new LDAPRecord();
		if (result != null) {
			result.getAttributes().getAll().each { Attribute att ->
				// Obtener todos los valores del atributo
				List<Object> values = []
				NamingEnumeration e = att.getAll();
				while (e.hasMoreElements()) {
					Object o = e.nextElement();
					if (o instanceof List) {
						o.each { values << it }
					}
					else {
						values << o;
					}
				}
				record.populate(att.getID(), values);			
			}
		}
		return record;
	}
	
	/**
	 * Este método devuelve registros de LDAP por pertenencia a un determinado
	 * grupo.
	 * @param groupName Nombre de grupo a buscar.
	 * @return Lista de registros que corresponden a ese grupo.
	 */
	public List<LDAPRecord> getByGroupName(String groupName) {
		List<LDAPRecord> ret = [];
		queryLDAP("memberOf=${groupName}").each { SearchResult result ->
			ret << populateRecord(result);
		}
		return ret;
	}
	
	// Implementación de la consulta
	private List<SearchResult> queryLDAP(String queryString) {
		List<SearchResult> ret = [];
		SearchControls searchControls = new SearchControls();		
		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		NamingEnumeration<SearchResult> results = 
			ctx.search(LDAP_BASE_QUERY, queryString, searchControls);

		while (results.hasMoreElements()) {
			 ret << (SearchResult) results.nextElement();
		}
		return ret;
	}
}
