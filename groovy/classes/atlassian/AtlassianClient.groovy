package atlassian

import static groovy.io.FileType.*

import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils

import es.eci.utils.ParameterValidator
import es.eci.utils.base.Loggable
import es.eci.utils.pom.MavenCoordinates
import ssh.KeystoreInformation
import ssh.RESTClientHelper;
import ssh.SecureRESTClientHelper


class AtlassianClient extends Loggable {

	/** Certificado para bitbucket */
	public static final MavenCoordinates BITBUCKET_CERTIFICATE =
		new MavenCoordinates("es.eci.bitbucket", "keystore", "1.0.0");
	/** Certificado para JIRA */
	public static final MavenCoordinates JIRA_CERTIFICATE =
		new MavenCoordinates("es.eci.jira", "keystore", "1.0.0");
	
	//-----------------------------------------------------------------
	// Constantes del cliente

	// Coordenadas del keystore en Nexus: esto es común para todos
	//	los keystores necesarios para Atlassian
	private static final String KEYSTORE_FILENAME = "cert.jks";
	private static final String KEYSTORE_PASSWORD = "12345678";	

	//-----------------------------------------------------------------
	// Propiedades del cliente
	
	/** User y Pass de Atlassian **/
	private String atlassianUser;
	private String atlassianPass;
	/** URL de gitlab. */
	private String urlAtlassian;
	/** Versión del certificado de gitlab (se bajará de Nexus). */
	private String atlassianKeystoreVersion;
	/** URL de Nexus. */
	private String urlNexus = null;
	/** Caché de keystore. */	
	private SecureRESTClientHelper secureHelper = new SecureRESTClientHelper();
	// Coordenadas base para el certificado
	private MavenCoordinates certificate;

	//-----------------------------------------------------------------
	// Métodos del cliente

	/**
	 * Construye un cliente inicializado para comunicarse con el 
	 * servidor indicado.
	 * @param urlAtlassian URL del servicio Atlassian.
	 * @param certificateCoordinates Coordenadas base del certificado necesario (usar
	 * las constantes BITBUCKET_CERTIFICATE o JIRA_CERTIFICATE)
	 * @param atlassianKeystoreVersion Si es distinto de null, sobrescribe la
	 * versión en las coordenadas
	 * @param atlassianUser Usuario del servicio Atlassian
	 * @param atlassianPass Password del usuario
	 */
	public AtlassianClient(
			String urlAtlassian, 
			MavenCoordinates certificateCoordinates,
			String atlassianKeystoreVersion, 
			String atlassianUser, 
			String atlassianPass) {
		this(urlAtlassian, certificateCoordinates, 
			atlassianKeystoreVersion, null, atlassianUser, atlassianPass)
	}

	/**
	 * Construye un cliente inicializado para comunicarse con el 
	 * servidor indicado.
	 * @param urlAtlassian URL del servicio Atlassian.
	 * @param certificateCoordinates Coordenadas base del certificado necesario (usar
	 * las constantes BITBUCKET_CERTIFICATE o JIRA_CERTIFICATE)
	 * @param atlassianKeystoreVersion Si es distinto de null, sobrescribe la
	 * versión en las coordenadas
	 * @param atlassianUser Usuario del servicio Atlassian
	 * @param atlassianPass Password del usuario
	 * @param urlNexus URL de Nexus, si se indica
	 */
	public AtlassianClient(
			String urlAtlassian, 
			MavenCoordinates certificateCoordinates,
			String atlassianKeystoreVersion, 
			String urlNexus, 
			String atlassianUser,
			String atlassianPass) {
		ParameterValidator.builder().
				add("urlAtlassian", urlAtlassian).
				add("certificateCoordinates", certificateCoordinates).
				add("atlassianUser", atlassianUser).
				add("atlassianPass", atlassianPass).
				build().validate();

		this.certificate = new MavenCoordinates(certificateCoordinates);
		this.urlAtlassian = urlAtlassian;
		if (atlassianKeystoreVersion != null) {
			this.atlassianKeystoreVersion = atlassianKeystoreVersion;
		}
		if (urlNexus != null) {
			this.urlNexus = urlNexus;
		}
		this.atlassianUser = atlassianUser
		this.atlassianPass = atlassianPass
	}



	/**
	 * Este método hace una llamada GET a un servicio REST.
	 * @param entityName Entidad solicitada por REST.
	 * @param params Parámetros a la llamada.
	 * @param apiVersion Versión de la API que se consulta.
	 * @return Cadena JSON devuelta por el servicio Atlassian.
	 */
	public String get(String entityName = null, String apiVersion, Map<String, String> params = null) {
		HttpGet get = RESTClientHelper.initGetMethod(
				urlAtlassian + "/rest/api/" + apiVersion + "/" + entityName,
				params);
		HttpClient client = getHttpClient();
		// Autenticación "basic"
		UsernamePasswordCredentials creds = new UsernamePasswordCredentials(
				this.atlassianUser,
				this.atlassianPass);
			
		get.addHeader(BasicScheme.authenticate(creds,"UTF-8",false));
		log "Lanzando $get ..."
		HttpResponse response = client.execute(get);
		HttpEntity entity = response.getEntity();
		return EntityUtils.toString(entity);
	}


	// Instancia un cliente http con la seguridad apropiada
	private HttpClient getHttpClient() {
		HttpClient client = null;
		if (urlAtlassian.startsWith("https")) {
			MavenCoordinates coords = certificate;
			if (this.atlassianKeystoreVersion != null) {
				coords.setVersion(atlassianKeystoreVersion);
			}
			coords.setPackaging("zip");
			secureHelper.initLogger(this);
			if (this.urlNexus != null) {
				secureHelper.setUrlNexus(this.urlNexus);
			}

			CredentialsProvider provider = new BasicCredentialsProvider();
			UsernamePasswordCredentials credentials = 
				new UsernamePasswordCredentials(atlassianUser, atlassianPass);
			provider.setCredentials(AuthScope.ANY, credentials);

			client = secureHelper.createSecureHttpClient(
					new KeystoreInformation(
					KEYSTORE_FILENAME,
					KEYSTORE_PASSWORD),
					coords);
		}
		else {
			// cliente sin configuración adicional
			HttpClientBuilder builder = HttpClientBuilder.create();
			client = builder.build();
		}
		return client;
	}
}

