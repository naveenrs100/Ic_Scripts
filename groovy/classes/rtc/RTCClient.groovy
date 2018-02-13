package rtc

import java.nio.charset.Charset

import org.apache.http.Header
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.NameValuePair
import org.apache.http.auth.InvalidCredentialsException
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils

import ssh.KeystoreInformation
import ssh.RESTClientHelper;
import ssh.SecureRESTClientHelper
import es.eci.utils.Stopwatch
import es.eci.utils.base.Loggable
import es.eci.utils.pom.MavenCoordinates
import groovy.json.JsonOutput;

/**
 * Esta clase implementa un cliente REST que ataca las APIS OSLC y RPT de RTC.
 */
class RTCClient extends Loggable {

	//-----------------------------------------------------------------
	// Constantes del cliente
	
	static String AUTHURL = "X-jazz-web-oauth-url";
	static String AUTHREQUIRED = "X-com-ibm-team-repository-web-auth-msg";
    // name of custom header that authentication messages are stored in
    private static final String FORM_AUTH_HEADER = "X-com-ibm-team-repository-web-auth-msg"; //$NON-NLS-1$
    // auth header value when authentication is required
    private static final String FORM_AUTH_REQUIRED_MSG = "authrequired"; //$NON-NLS-1$
    // auth header value when authentication failed
    private static final String FORM_AUTH_FAILED_MSG = "authfailed"; //$NON-NLS-1$
    // URI the server redirects to when authentication fails
    public static final String FORM_AUTH_FAILED_URI = "/auth/authfailed"; //$NON-NLS-1$
	
	// Coordenadas del keystore en Nexus
	private static final String KEYSTORE_GROUPID = "es.eci.rtc";
	private static final String KEYSTORE_ARTIFACTID = "keystore";
	private static final String KEYSTORE_FILENAME = "rtc.jks";
	private static final String KEYSTORE_PASSWORD = "changeit";
	
	//-----------------------------------------------------------------
	// Propiedades del cliente
	
	/** URL de RTC. */
	private String rtcURL;
	/** Usuario de RTC */
	private String rtcUser;
	/** Password de RTC */
	private String rtcPwd;
	/** URL de nexus */
	private String nexusURL;
	/** Versión del keystore a utilizar */
	private keystoreVersion;
	// Cliente http
	private HttpClient client = null;
	/** Caché de keystore. */	
	private SecureRESTClientHelper secureHelper = new SecureRESTClientHelper();
	/** Status http de la última llamada. */
	private Integer lastHttpStatus = null;
	
	//-----------------------------------------------------------------
	// Métodos del cliente
	
	
	
	/**
	 * @return the lastHttpStatus
	 */
	public Integer getLastHttpStatus() {
		return lastHttpStatus;
	}

	/**
	 * Construye un cliente con la información necesaria
	 * @param rtcURL URL base de RTC
	 * @param rtcUser Id de usuario
	 * @param rtcPwd Password
	 * @param keystoreVersion Versión del keystore
	 * @param nexusURL URL de nexus
	 */
	public RTCClient(
			String rtcURL, 
			String rtcUser, 
			String rtcPwd, 
			String keystoreVersion, 
			String nexusURL) {
		this.rtcURL = rtcURL;
		this.rtcUser = rtcUser;
		this.rtcPwd = rtcPwd;
		// Coordenadas para obtener el certificado
		this.keystoreVersion = keystoreVersion;
		this.nexusURL = nexusURL;
	}
	
	/**
	 * Este método hace un HTTP GET contra el recurso indicado, con los
	 * parámetros solicitados.  Devuelve el resultado 'crudo' para que
	 * la llamada invocante lo procese (json, xml, etc.)
	 * @param entityName Entidad REST o más bien ruta absoluta del servicio a llamar
	 * @param params Parámetros de la llamada en formato clave -> valor
	 * @return Respuesta 'cruda' del servidor
	 */
	public String get(String entityName, Map<String, String> params = null) {
		String ret = null;
		long millis = Stopwatch.watch {
			HttpGet get = RESTClientHelper.initGetMethod(
				rtcURL + "/" + entityName, params,
				["Accept":"application/rdf+xml",
				 "OSLC-Core-Version":"2.0"]);
			getHttpClient();
			log "Lanzando $get ..."
			HttpResponse response = getProtectedResource(get)
			HttpEntity entity = response.getEntity();
			ret = EntityUtils.toString(entity);
		}
		log "Completado GET ($lastHttpStatus) -> $millis msec."
		return ret;
	}
	
	/**
	 * Este método hace un HTTP POST contra el recurso indicado, con los
	 * parámetros solicitados.  Devuelve el resultado 'crudo' para que
	 * la llamada invocante lo procese (json, xml, etc.)
	 * @param entityName Entidad REST o más bien ruta absoluta del servicio a llamar
	 * @param params Parámetros de la llamada en formato clave -> valor
	 * @return Respuesta 'cruda' del servidor
	 */
	public String post(String entityName, Map<String, String> params = null) {
		def method = RESTClientHelper.initPostMethod(
			rtcURL + "/" + entityName,
			null,
			["Accept":"application/rdf+xml",
			 "Content-Type":"application/json",
			 "OSLC-Core-Version":"2.0"]);
		return sendUpdate(method, params);
	}
	
	/**
	 * Este método hace un HTTP PUT contra el recurso indicado, con los
	 * parámetros solicitados.  Devuelve el resultado 'crudo' para que
	 * la llamada invocante lo procese (json, xml, etc.)
	 * @param entityName Entidad REST o más bien ruta absoluta del servicio a llamar
	 * @param params Parámetros de la llamada en formato clave -> valor
	 * @return Respuesta 'cruda' del servidor
	 */
	public String put(String entityName, Map<String, String> params = null) {
		def method = RESTClientHelper.initPutMethod(
			rtcURL + "/" + entityName,
			null,
			["Accept":"application/rdf+xml",
			 "Content-Type":"application/json"])
		//method.addHeader("OSLC-Core-Version", "2.0");
		return sendUpdate(method, params);
	}
	
	// Implementación de una actualización http
	private String sendUpdate(HttpEntityEnclosingRequestBase method, Map<String, String> params = null) {
		String ret = null;
		long millis = Stopwatch.watch {
			if (params != null) {
				method.setEntity(new StringEntity(JsonOutput.toJson(params), "UTF-8"))
			}
			HttpClient client = getHttpClient();
			log "Lanzando $method ..."
			HttpResponse response = getProtectedResource(method);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				ret = EntityUtils.toString(entity);
			}
		}
		log "Completado ${method.getMethod()} (${this.lastHttpStatus}) -> $millis msec."
		return ret;
	}
	
	// Devuelve una instancia de cliente HTTP
	private HttpClient getHttpClient() {
		if (client == null) {
			MavenCoordinates coords =
				new MavenCoordinates(KEYSTORE_GROUPID, KEYSTORE_ARTIFACTID, keystoreVersion);
			coords.setPackaging("zip");
			secureHelper.initLogger(this);
			if (this.nexusURL != null) {
				secureHelper.setUrlNexus(this.nexusURL);
			}
			client = secureHelper.createSecureHttpClient(
				new KeystoreInformation(
					KEYSTORE_FILENAME,
					KEYSTORE_PASSWORD),
				coords);
		}
		return client;
	}
	
	// Este método implementa la forma recomendada de acceso a recursos protegidos en
	//	RTC: 
	
	// 1 - intentar el acceso
	// 2 - en caso de que esté protegido, simular la autenticación por formulario
	// 3 - reintentar el acceso
	
	// El cliente http se mantiene, de forma que conserva las cookies necesarias
	private HttpResponse getProtectedResource(HttpUriRequest request) {
		HttpResponse documentResponse = client.execute(request);
		log "--> " + documentResponse.toString()
		def firstResponseCode = documentResponse.getStatusLine().getStatusCode()
		this.lastHttpStatus = firstResponseCode;
		if ([200, 302].contains(firstResponseCode)) {
			Header header = documentResponse.getFirstHeader("x-com-ibm-team-repository-web-auth-msg");
			if ((header!=null) && ("authrequired".equals(header.getValue()))) {
				log "Acceso denegado ($firstResponseCode), realizando la autenticación por formulario..."
				documentResponse.getEntity().consumeContent();
				// The server requires an authentication: Create the login form
				HttpPost formPost = new HttpPost(rtcURL+"/auth/j_security_check");
				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				nvps.add(new BasicNameValuePair("j_username", rtcUser));
				nvps.add(new BasicNameValuePair("j_password", rtcPwd));
				formPost.setEntity(new UrlEncodedFormEntity(nvps, Charset.forName("UTF-8")));
				// Step (2): The client submits the login form
				HttpResponse formResponse = client.execute(formPost);
				header = formResponse.getFirstHeader(AUTHREQUIRED);
				if ((header!=null) && ("authfailed".equals(header.getValue()))) {
					// The login failed
					throw new InvalidCredentialsException("Authentication failed");
				} else {
					formResponse.getEntity().consumeContent();
					// The login succeed
					// Step (3): Request again the protected resource
					HttpUriRequest documentGet2 = request.clone();
					log "Relanzando $documentGet2 ..."
					documentResponse = client.execute(documentGet2);
					log "--> " + documentResponse.toString()
					this.lastHttpStatus = documentResponse.getStatusLine().getStatusCode()
				}
			}
		}
		return  documentResponse;
	}
}
