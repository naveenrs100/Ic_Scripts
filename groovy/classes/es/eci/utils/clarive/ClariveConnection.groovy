package es.eci.utils.clarive;

import java.io.File;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import es.eci.utils.base.Loggable;
import groovy.io.FileType;
import groovy.json.*;

public class ClariveConnection extends Loggable {

	/**
	 * Ejecución del servicio SWC01 de Clarive el cual 
	 * crea y actualiza el tópico referenciado por el 
	 * parámetro "id_proceso".
	 * @param nakedUrl
	 * @param api_key_clarive
	 * @param nombre_producto
	 * @param nombre_subproducto
	 * @param nombre_corriente
	 * @param nombre_componente
	 * @param num_version
	 * @param version_maven
	 * @param proceso
	 * @param id_proceso
	 * @param paso
	 * @param resultado
	 * @param metrica
	 */
	public Map<String,String> swc01(nakedUrl, api_key_clarive, eci_proxy_url, eci_proxy_port, nombre_producto,
			nombre_subproducto, tipo_corriente, nombre_componente, num_version, version_maven,
			proceso, id_proceso, paso, resultado) {

		def url = "${nakedUrl}/rule/json/SWC01?" +
				"api_key=${api_key_clarive}&" +
				"producto=${nombre_producto}&"+
				"subproducto=${nombre_subproducto}&"+
				"corriente=${tipo_corriente}&"+
				"componente=${nombre_componente}&"+
				"version=${num_version}&"+
				"version_maven=${version_maven}&"+
				"proceso=${proceso}&"+
				"id_proceso=${id_proceso}&"+
				"paso=${paso}&"+
				"resultado_paso=${resultado}";

		def proxy = "${eci_proxy_url}";
		def port = "${eci_proxy_port}";

		def ret = connectToService(url, eci_proxy_url, eci_proxy_port);

		return ret;
	}

	/**
	 * Ejecución del servicio SWC02 de Clarive el cual 
	 * valida la existencia y validez del tópico de Clarive
	 * indicado por "id_proceso".
	 * Solo se usará cuando un usuario pida una Release desde QUVE,
	 * ante lo cual se usará SWC02 para preguntar a Clarive si es posible y,
	 * en caso de que lo sea, mandar de vuelta el "id_proceso" que se utilizará
	 * durante toda la interacción IC - CLARIVE.
	 * @param nakedUrl
	 * @param api_key_clarive
	 * @param eci_proxy_url
	 * @param eci_proxy_port
	 * @param proceso
	 * @param codigo_release
	 * @param nombre_producto
	 * @param nombre_subproducto
	 * @param nombre_corriente
	 */
	public Map<String,String> swc02(nakedUrl, api_key_clarive, eci_proxy_url, eci_proxy_port,
			proceso, codigo_release, nombre_producto, nombre_subproducto, tipo_corriente) {

		def url = "${nakedUrl}/rule/json/SWC02?" +
				"api_key=${api_key_clarive}&" +
				"proceso=${proceso}&"+
				"codigo_release=${codigo_release}&"+
				"producto=${nombre_producto}&"+
				"subproducto=${nombre_subproducto}&"+
				"corriente=${tipo_corriente}";
		def proxy = "${eci_proxy_url}";
		def port = "${eci_proxy_port}";

		def ret = connectToService(url, eci_proxy_url, eci_proxy_port);

		def response = ret.get("response");
		def id_proceso = ret.get("id_proceso");
		def cod_release = ret.get("cod_release");
		def instantanea = ret.get("instantanea");
		
		log("ID_PROCESO:");
		log(id_proceso);
		
		log("INSTANTANEA DEVUELTA:");
		log(instantanea);
		
		return ret;

	}

	/**
	 * Ejecución del servicio SWC03 de Clarive el cual 
	 * informa del fin del proceso asociado al "id_proceso".
	 * @param nakedUrl
	 * @param api_key_clarive
	 * @param tipo_proceso
	 * @param producto
	 * @param subproducto
	 * @param corriente
	 * @param componente
	 * @param version
	 * @param id_proceso
	 * @param resultado
	 * @param release_clarive
	 */
	public void swc03(nakedUrl, api_key_clarive, eci_proxy_url, eci_proxy_port, tipo_proceso,
			nombre_producto, nombre_subproducto, tipo_corriente, nombre_componente, num_version,
			id_proceso,	resultado, metrica_PU, metrica_PC) {

		def url = "${nakedUrl}/rule/json/SWC03?" +
				"api_key=${api_key_clarive}&" +
				"producto=${nombre_producto}&"+
				"subproducto=${nombre_subproducto}&"+
				"corriente=${tipo_corriente}&"+
				"componente=${nombre_componente}&"+
				"version=${num_version}&"+
				"tipo_proceso=${tipo_proceso}&"+
				"id_proceso=${id_proceso}&"+
				"resultado=${resultado}&"+
				"metrica_PU=${metrica_PU}&"+
				"metrica_PC=${metrica_PC}";
		def proxy = "${eci_proxy_url}";
		def port = "${eci_proxy_port}";

		def ret = connectToService(url, eci_proxy_url, eci_proxy_port);

	}

	/**
	 * Método de apoyo que hace POST a la url indicada
	 * TODO: Montarlo para que sea capaz de conectar por https.
	 * @param url
	 * @return
	 */
	private Map<String,String> connectToService(url, eci_proxy_url, eci_proxy_port) {
		url = url.replaceAll(" ", "%20");
		log("Conectando con Clarive mediante la url:\n${url}");
		URL server = new URL(url);
		Properties systemProperties = System.getProperties();
		//systemProperties.setProperty("http.proxyHost","${eci_proxy_url}");
		//systemProperties.setProperty("http.proxyPort","${eci_proxy_port}");

		def encoding = "UTF-8";

		HttpURLConnection con = (HttpURLConnection)server.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.flush();
		wr.close();

		// Resultado
		int responseCode = 0;
		try {
			responseCode = con.getResponseCode();
		} catch (Exception e) {
			throw new Exception(e);
		} finally {
			log("Response Code : " + responseCode);
		}

		BufferedReader inReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = inReader.readLine()) != null) {
			response.append(inputLine);
		}
		inReader.close();
		
		JsonSlurper jSlurper = new JsonSlurper();
		def jsonResponse = jSlurper.parseText(response.toString());

		// Se recoge el id_proceso y el codigo de release de la cabecera (No tienen porqué venir dados).
//		def id_proceso = con.getHeaderField("id_proceso");
//		def cod_release = con.getHeaderField("cod_release");
		def id_proceso = jsonResponse.id_proceso;
		def cod_release = jsonResponse.cod_release;
		def instantanea = jsonResponse.instantanea;
		def msg = jsonResponse.msg;
		def res = jsonResponse.res;

		def responseMap = [:];
		responseMap.put("msg", "${msg}");
		responseMap.put("res", "${res}");
		responseMap.put("id_proceso", "${id_proceso}");
		responseMap.put("cod_release", "${cod_release}");
		responseMap.put("instantanea", "${instantanea}");
		responseMap.put("json","${response.toString()}");
		
		log("Resultado: ${res}");
		log("Respuesta: ${msg}")		
		log("Response Json: ${response.toString()}")
		
		return responseMap;
	}

}
