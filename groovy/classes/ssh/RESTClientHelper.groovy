package ssh

import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.HTTP

/**
 * Funcionalidad útil para clientes REST seguros.
 */
class RESTClientHelper {
	
	//-------------------------------------------------------------------------
	// Métodos del helper 	
	
	/**
	 * Este método añade parámetros a la URL base para lanzar un GET sobre el 
	 * endpoint.
	 * @param baseURL URL base del servicio
	 * @param params Parámetros de la llamada
	 * @param headers Lista opcional de cabeceras para el método
	 * @return Llamada GET resultante de añadir los parámetros
	 */
	public static HttpGet initGetMethod(
				String baseURL, 
				Map<String, String> params,
				Map<String, String> headers = null) {
		StringBuilder sb = new StringBuilder(baseURL);
		boolean first = true;
		for (String key: params?.keySet()) {
			if (first) {
				sb.append("?");
				first = false;
			}
			else {
				sb.append("&")
			}
			sb.append(key);
			sb.append("=");
			sb.append(URLEncoder.encode(params[key].toString(), "UTF-8"));
		}
		HttpGet ret = new HttpGet(sb.toString());
		headers?.keySet().each{ String key ->
			ret.addHeader(key, headers[key]);
		}
		return ret;
	}
	
	/**
	 * Este método prepara un PUT, inicializado con parámetros, para lanzarlo
	 * sobre la URL indicada
	 * @param baseURL URL base del servicio
	 * @param params Parámetros de la llamada
	 * @param headers Lista opcional de cabeceras para el método
	 * @return Llamada PUT resultante de añadir los parámetros
	 */
	public static HttpPut initPutMethod(
				String baseURL, 
				Map<String, String> params,
				Map<String, String> headers = null) {
		return setBody(new HttpPut(baseURL), params);
	}
				
	/**
	 * Este método prepara un POST, inicializado con parámetros, para lanzarlo
	 * sobre la URL indicada
	 * @param baseURL URL base del servicio
	 * @param params Parámetros de la llamada
	 * @param headers Lista opcional de cabeceras para el método
	 * @return Llamada POST resultante de añadir los parámetros
	 */
	public static HttpPost initPostMethod(
				String baseURL, 
				Map<String, String> params,
				Map<String, String> headers = null) {
		return setBody(new HttpPost(baseURL), params);
	}
	
	// Este método inicializa los parámetros en el cuerpo de un PUT o un POST
	// Devuelve el request inicializado para lanzarse
	private static HttpEntityEnclosingRequestBase setBody(
			HttpEntityEnclosingRequestBase request, 
			Map<String, String> params,
			Map<String, String> headers = null) {
		headers?.keySet().each{ String key ->
			request.addHeader(key, headers[key]);
		}
		List<BasicNameValuePair> nvps = new ArrayList<BasicNameValuePair>();
		for (String key: params?.keySet()) {
			nvps.add(new BasicNameValuePair(key, params[key]));
		}
		request.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		return request;
	}
}
