package git

import static groovy.io.FileType.*

import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.HTTP
import org.apache.http.util.EntityUtils

import ssh.KeystoreInformation
import ssh.SecureRESTClientHelper
import es.eci.utils.ParameterValidator
import es.eci.utils.Stopwatch
import es.eci.utils.base.Loggable
import es.eci.utils.pom.MavenCoordinates


/**
 * Esta clase encapsula la comunicación con el servidor gitlab.
 */
class GitlabClient extends Loggable {

	//-----------------------------------------------------------------
	// Constantes del cliente
	
	// Coordenadas del keystore en Nexus
	private static final String KEYSTORE_GROUPID = "es.eci.gitlab";
	private static final String KEYSTORE_ARTIFACTID = "keystore";
	private static final String KEYSTORE_FILENAME = "gitlab.jks";
	private static final String KEYSTORE_PASSWORD = "changeit";
	
	//-----------------------------------------------------------------
	// Propiedades del cliente
	
	/** URL de gitlab. */
	private String urlGitlab;
	/** Token privado (autenticación de usuario) */
	private String privateToken;
	/** Versión del certificado de gitlab (se bajará de Nexus). */
	private String keystoreVersion = "1.0.0.0";
	/** URL de Nexus. */
	private String urlNexus = null;
	/** Caché de keystore. */	
	private SecureRESTClientHelper secureHelper = new SecureRESTClientHelper();
	
	//-----------------------------------------------------------------
	// Métodos del cliente
	
	/**
	 * Construye un cliente inicializado para comunicarse con el 
	 * servidor indicado.
	 * @param urlGitlab URL de gitlab.
	 * @param privateToken Token privado de autenticación.
	 */
	public GitlabClient(
			String urlGitlab, 
			String privateToken) {
		this(urlGitlab, privateToken, null, null)
	}
			
	/**
	 * Construye un cliente inicializado para comunicarse con el 
	 * servidor indicado.
	 * @param urlGitlab URL de gitlab.
	 * @param privateToken Token privado de autenticación.
	 * @param keystoreVersion Versión de keystore a utilizar, si se indica.
	 * @param urlNexus URL de Nexus, si se indica
	 */
	public GitlabClient(
			String urlGitlab, 
			String privateToken, 
			String keystoreVersion, 
			String urlNexus) {
		
		ParameterValidator.builder().
			add("urlGitlab", urlGitlab).
			add("privateToken", privateToken).
				build().validate();
				
		this.urlGitlab = urlGitlab;
		this.privateToken = privateToken;
		if (keystoreVersion != null) {
			this.keystoreVersion = keystoreVersion;
		}
		if (urlNexus != null) {
			this.urlNexus = urlNexus;
		}
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
			post = new HttpPost(urlGitlab + "/api/v3/" + entityName);
			List<BasicNameValuePair> nvps = new ArrayList<BasicNameValuePair>();
			for (String key: params.keySet()) {
				nvps.add(new BasicNameValuePair(key, params[key]));
			}
			post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			HttpClient client = getHttpClient();
			log("Lanzando $post ...");
			post.addHeader("PRIVATE-TOKEN", privateToken);
			log "Lanzando $post ..."
			HttpResponse response = client.execute(post);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				ret = EntityUtils.toString(entity);
			}
		}
		log "Completado POST -> $millis msec."
		return ret;
	}
	
	/**
	 * Este método hace una llamada put a un servicio REST.
	 * @param entityName Entidad solicitada por REST.
	 * @param params Parámetros a la llamada.
	 * @return Cadena JSON devuelta por Sonar.
	 */
	public String put(String entityName, Map<String, String> params) {
		String ret = null;
		HttpPut put = null;
		long millis = Stopwatch.watch {
			put = new HttpPut(urlGitlab + "/api/v3/" + entityName);
			List<BasicNameValuePair> nvps = new ArrayList<BasicNameValuePair>();
			for (String key: params.keySet()) {
				nvps.add(new BasicNameValuePair(key, params[key]));
			}
			put.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			HttpClient client = getHttpClient();
			log("Lanzando $put ...");
			put.addHeader("PRIVATE-TOKEN", privateToken);
			log "Lanzando $put ..."
			HttpResponse response = client.execute(put);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				ret = EntityUtils.toString(entity);
			}
		}
		log "Completado PUT -> $millis msec."
		return ret;
	}
		
	/**
	 * Este método hace una llamada GET a un servicio REST. 
	 * @param entityName Entidad solicitada por REST.
	 * @param params Parámetros a la llamada.
	 * @return Cadena JSON devuelta por gitlab.
	 */
	public String get(String entityName, Map<String, String> params) {
		StringBuilder uri = new StringBuilder(urlGitlab + "/api/v3/" + entityName);
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
		HttpGet get = new HttpGet(uri.toString());
		HttpClient client = getHttpClient();
		// Autenticación propia de gitlab
		// Se include un header PRIVATE-TOKEN
		get.addHeader("PRIVATE-TOKEN", privateToken);
		log "Lanzando $get ..."
		HttpResponse response = client.execute(get);
		HttpEntity entity = response.getEntity();
		return EntityUtils.toString(entity);
	}

	// Instancia un cliente http con la seguridad apropiada	
	private HttpClient getHttpClient() {
		HttpClient client = null;
		if (urlGitlab.startsWith("https")) {
			MavenCoordinates coords =
				new MavenCoordinates(KEYSTORE_GROUPID, KEYSTORE_ARTIFACTID, keystoreVersion);
			coords.setPackaging("zip");
			secureHelper.initLogger(this);
			if (this.urlNexus != null) {
				secureHelper.setUrlNexus(this.urlNexus);
			}
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
