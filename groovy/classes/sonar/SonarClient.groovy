package sonar

import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
import org.apache.http.auth.AuthScope
import org.apache.http.auth.Credentials
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.AuthCache
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.BasicAuthCache
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.HTTP
import org.apache.http.util.EntityUtils

import ssh.KeystoreInformation
import ssh.SecureRESTClientHelper
import es.eci.utils.Stopwatch;
import es.eci.utils.base.Loggable
import es.eci.utils.pom.MavenCoordinates

/**
 * Esta clase agrupa la funcionalidad de cliente de servicios REST de SonarQUBE
 */
class SonarClient extends Loggable {

	//-----------------------------------------------------------------
	// Constantes del cliente
	
	// Coordenadas del keystore en Nexus
	private static final String KEYSTORE_GROUPID = "es.eci.sonar";
	private static final String KEYSTORE_ARTIFACTID = "keystore";
	private static final String KEYSTORE_FILENAME = "sonar.jks";
	private static final String KEYSTORE_PASSWORD = "changeit";
	// Códigos de éxito
	private static final List<Integer> SUCCESS_CODES = [ 200, 204 ]
	
	//-----------------------------------------------------------------
	// Propiedades del cliente
	
	/** URL de gitlab. */
	private String urlSonar;
	/** Autenticación de usuario */
	private SonarCredentials credentials;
	/** Versión del certificado de sonar (se bajará de Nexus). */
	private String keystoreVersion = "1.0.0.0";
	/** URL de Nexus. */
	private String urlNexus = null;
	/** Caché del keystore. */
	private SecureRESTClientHelper secureHelper;
	
	//-----------------------------------------------------------------
	// Métodos del cliente
	
	/**
	 * Crea un cliente de sonar con las credenciales y la información de acceso
	 * @param credentials Credenciales de usuario Sonar
	 * @param urlSonar URL de Sonar
	 * @param keystoreVersion (opcional) versión de keystore a utilizar
	 * @param urlNexus (opcional) Dirección del repo public de Nexus
	 */
	public SonarClient(
			SonarCredentials credentials, 
			String urlSonar, 
			String keystoreVersion,
			String urlNexus) {
		this.credentials = credentials;
		this.urlSonar = urlSonar;
		if (keystoreVersion != null) {
			this.keystoreVersion = keystoreVersion;
		}
		secureHelper = new SecureRESTClientHelper();
		secureHelper.initLogger(this);
		if (urlNexus != null) {
			this.urlNexus = urlNexus;
			secureHelper.setUrlNexus(this.urlNexus);
		}
	}
	
	/**
	 * Crea un cliente de sonar con las credenciales y la información de acceso
	 * @param credentials Credenciales de usuario Sonar
	 * @param urlSonar URL de Sonar
	 */
	public SonarClient(
			SonarCredentials credentials,
			String urlSonar) {
		this.credentials = credentials;
		this.urlSonar = urlSonar;
	}
			
	/**
	 * Este método hace una llamada get a un servicio REST.
	 * @param entityName Entidad solicitada por REST.
	 * @param params Parámetros a la llamada.
	 * @return Cadena JSON devuelta por Sonar.
	 */
	public String get(String entityName, Map<String, String> params) {		
		String ret = null;
		HttpGet get = null;
		long millis = Stopwatch.watch {
			StringBuilder uri = new StringBuilder(getBaseSonarURI() + entityName);
			boolean first = true;
			for (String key: params?.keySet()) {
				if (first) {
					uri.append("?");
					first = false;
				}
				else {
					uri.append("&")
				}
				uri.append(key);
				uri.append("=");
				uri.append(URLEncoder.encode(params[key].toString(), "UTF-8"));
			}
			get = new HttpGet(uri.toString());
			HttpClient client = getHttpClient();		
			log "Lanzando $get ..."		
			HttpResponse response = executeWithBasicAuthentication(client, get);
			HttpEntity entity = response.getEntity();
			ret = EntityUtils.toString(entity);
		}
		log "Completado GET -> $millis msec."
		return ret;
	}

	// Lanza indicando al cliente que debe usar autenticación
	private HttpResponse executeWithBasicAuthentication(
			HttpClient client, 
			HttpRequestBase request) {
		HttpHost target = createTargetHost();
		AuthCache authCache = new BasicAuthCache();
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(target, basicAuth);
		HttpClientContext localContext = HttpClientContext.create();
		localContext.setAuthCache(authCache);
		HttpResponse response = client.execute(request, localContext)
		if (!SUCCESS_CODES.contains(response.getStatusLine().getStatusCode())) {
			throw new Exception("ERROR HTTP ${response.getStatusLine().getStatusCode()}\n"
				+"${response.getStatusLine().getReasonPhrase()}\n"
				+"${request}")
		}
		return response
	}
	
	// Este método crea un target host en términos de apache commons http para
	//	la URL pedida				
	public HttpHost createTargetHost() {
		URL url = new URL(this.urlSonar)
		return new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
	}

	// URI base de sonar
	private String getBaseSonarURI() {
		String fragment = null;
		if (urlSonar.endsWith("/")) {
			fragment = "api/";
		}
		else {
			fragment = "/api/";
		}
		return urlSonar + fragment;
	}
	
	/**
	 * Este método hace una llamada post a un servicio REST.
	 * @param entityName Entidad solicitada por REST.
	 * @param params Parámetros a la llamada.
	 * @return Cadena JSON devuelta por Sonar.
	 */
	public String post(String entityName, Map<String, String> params) {	
		String ret = null;
		HttpPost post = null;
		long millis = Stopwatch.watch {
			post = new HttpPost(getBaseSonarURI() + entityName);
			List<BasicNameValuePair> nvps = new ArrayList<BasicNameValuePair>();
			for (String key: params.keySet()) {
				nvps.add(new BasicNameValuePair(key, params[key]));
			}
			post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			HttpClient client = getHttpClient();
			log("Lanzando $post ...");
			HttpResponse response = executeWithBasicAuthentication(client, post);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				ret = EntityUtils.toString(entity);
			}
		}
		log "Completado POST -> $millis msec."
		return ret;
	}

	// Crea una instancia de cliente http
	private org.apache.http.impl.client.CloseableHttpClient getHttpClient() {
		HttpClient client = null;
		// Autenticación propia de sonar
		CredentialsProvider provider = new BasicCredentialsProvider();
		Credentials credentials =
				new UsernamePasswordCredentials(
				credentials.getUser(),
				credentials.getPassword());
		provider.setCredentials(AuthScope.ANY, credentials)
		if (urlSonar.startsWith("https")) {
			MavenCoordinates coords =
					new MavenCoordinates(KEYSTORE_GROUPID, KEYSTORE_ARTIFACTID, keystoreVersion);
			coords.setPackaging("zip");
			secureHelper.initLogger(this);
			client = secureHelper.createSecureHttpClient(
					new KeystoreInformation(
					KEYSTORE_FILENAME,
					KEYSTORE_PASSWORD),
					coords,
					provider);
		}
		else {
			// cliente sin configuración adicional
			HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setDefaultCredentialsProvider(provider);
			client = builder.build();
		}
		return client
	}
}
