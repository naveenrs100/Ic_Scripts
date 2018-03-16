package kiuwan

import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.DefaultProxyRoutePlanner
import org.apache.http.util.EntityUtils

import es.eci.utils.ParameterValidator
import es.eci.utils.base.Loggable
import ssh.RESTClientHelper
import ssh.SecureRESTClientHelper


class KiuwanApiClient extends Loggable {


	//-----------------------------------------------------------------
	// Propiedades del cliente
	/** User y Pass de Atlassian **/
	private String kiuwanUser;
	private String kiuwanPass;
	/** URL de gitlab. */
	private String urlKiuwanApi;

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
	public KiuwanApiClient(String urlKiuwanApi, String kiuwanUser, String kiuwanPass) {
		this(urlKiuwanApi, null, kiuwanUser, kiuwanPass)
	}

	/**
	 * Construye un cliente inicializado para comunicarse con el
	 * servidor indicado.
	 * @param urlAtlassian URL del servicio Atlassian.
	 * @param atlassianKeystoreVersion Versión de keystore a utilizar, si se indica.
	 * @param urlNexus URL de Nexus, si se indica
	 */
	public KiuwanApiClient(String urlKiuwanApi, String urlNexus, String kiuwanUser, String kiuwanPass) {
		ParameterValidator.builder().
				add("urlKiuwanApi", urlKiuwanApi).
				add("kiuwanUser", kiuwanUser).
				add("kiuwanPass", kiuwanPass).
				build().validate();

		this.urlKiuwanApi = urlKiuwanApi;

		if (urlNexus != null) {
			this.urlNexus = urlNexus;
		}
		this.kiuwanUser = kiuwanUser
		this.kiuwanPass = kiuwanPass
	}



	/**
	 * Este método hace una llamada GET a un servicio REST.
	 * @param entityName Entidad solicitada por REST.
	 * @param params Parámetros a la llamada.
	 * @param apiVersion Versión de la API que se consulta.
	 * @return Cadena JSON devuelta por el servicio Atlassian.
	 */
	public String get(String entityName = null, Map<String, String> params = null) {
		HttpGet get = RESTClientHelper.initGetMethod(
				urlKiuwanApi + "/" + entityName,
				params);
		HttpClient client = getHttpClient();

		// Autenticación "basic"
		UsernamePasswordCredentials creds = new UsernamePasswordCredentials(
				this.kiuwanUser,
				this.kiuwanPass);

		get.addHeader(BasicScheme.authenticate(creds,"UTF-8",false));
		log "Lanzando $get ..."
		HttpResponse response = client.execute(get);
		HttpEntity entity = response.getEntity();
		return EntityUtils.toString(entity);
	}


	// Instancia un cliente http con la seguridad apropiada
	private HttpClient getHttpClient() {
		HttpClient client = null;
		if (urlKiuwanApi.startsWith("https")) {
			secureHelper.initLogger(this);

			CredentialsProvider provider = new BasicCredentialsProvider();
			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(kiuwanUser, kiuwanPass);
			provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(kiuwanUser, kiuwanPass));
			
			HttpClientBuilder builder = HttpClientBuilder.create();
			if (provider != null) {
				builder = builder.setDefaultCredentialsProvider(provider);
			}

			HttpHost proxy = new HttpHost("proxyaplic01.eci.geci", 8080, "http");
			DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);			
			builder.setRoutePlanner(routePlanner);
			
			client = builder.build();

		}
		else {
			// cliente sin configuración adicional
			HttpClientBuilder builder = HttpClientBuilder.create();
			client = builder.build();
		}
		return client;
	}
}

