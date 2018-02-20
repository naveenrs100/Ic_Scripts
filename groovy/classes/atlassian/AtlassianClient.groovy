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

	//-----------------------------------------------------------------
	// Constantes del cliente

	// Coordenadas del keystore en Nexus
	private static final String KEYSTORE_GROUPID = "es.eci.bitbucket";
	private static final String KEYSTORE_ARTIFACTID = "keystore";
	private static final String KEYSTORE_FILENAME = "bitbucket.jks";
	private static final String KEYSTORE_PASSWORD = "12345678";	

	//-----------------------------------------------------------------
	// Propiedades del cliente
	/** User y Pass de Atlassian **/
	private String atlassianUser;
	private String atlassianPass;
	/** URL de gitlab. */
	private String urlAtlassian;
	/** Versión del certificado de gitlab (se bajará de Nexus). */
	private String keystoreVersion;
	/** URL de Nexus. */
	private String urlNexus = null;
	/** Caché de keystore. */	
	private SecureRESTClientHelper secureHelper = new SecureRESTClientHelper();

	//-----------------------------------------------------------------
	// Métodos del cliente

	/**
	 * Construye un cliente inicializado para comunicarse con el 
	 * servidor indicado.
	 * @param urlAtlassian URL del servicio Atlassian.
	 */
	public AtlassianClient(String urlAtlassian, String atlassianKeystoreVersion, String atlassianUser, String atlassianPass) {
		this(urlAtlassian, atlassianKeystoreVersion, null, atlassianUser, atlassianPass)
	}

	/**
	 * Construye un cliente inicializado para comunicarse con el 
	 * servidor indicado.
	 * @param urlAtlassian URL del servicio Atlassian.
	 * @param keystoreVersion Versión de keystore a utilizar, si se indica.
	 * @param urlNexus URL de Nexus, si se indica
	 */
	public AtlassianClient(String urlAtlassian, String atlassianKeystoreVersion, String urlNexus, String atlassianUser, String atlassianPass) {
		ParameterValidator.builder().
				add("urlAtlassian", urlAtlassian).
				build().validate();

		this.urlAtlassian = urlAtlassian;
		if (keystoreVersion != null) {
			this.keystoreVersion = keystoreVersion;
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
	public String get(String entityName, String apiVersion, Map<String, String> params) {
		HttpGet get = RESTClientHelper.initGetMethod(
				urlAtlassian + "/rest/api/"+ apiVersion +"/" + entityName,
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
			MavenCoordinates coords = new MavenCoordinates(KEYSTORE_GROUPID, KEYSTORE_ARTIFACTID, keystoreVersion);
			coords.setPackaging("zip");
			secureHelper.initLogger(this);
			if (this.urlNexus != null) {
				secureHelper.setUrlNexus(this.urlNexus);
			}

			CredentialsProvider provider = new BasicCredentialsProvider();
			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(atlassianUser, atlassianPass);
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

