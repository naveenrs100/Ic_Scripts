package es.eci.utils

import java.io.File

import static groovyx.net.http.ContentType.*
import es.eci.utils.base.Loggable
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.*

/**
 * Esta clase agrupa funciones de utilidad para comunicarse con QUVE
 */
class QuveHelper extends Loggable {

	//---------------------------------------------------
	// Propiedades del helper

	/** Directorio raíz de Jenkins */
	private File jenkinsHome;
	/** URL de servicios de QUVE */
	private String quveURL;

	//---------------------------------------------------
	// Métodos del helper

	/**
	 * Crea un helper con la información necesaria para atacar
	 * servicios de QUVE.
	 * @param jenkinsHome Directorio raíz de Jenkins
	 * @param quveURL
	 */
	public QuveHelper(String jenkinsHome, String quveURL) {
		super();
		this.jenkinsHome = new File(jenkinsHome);
		this.quveURL = quveURL;
	}

	// Obtiene la clave de sesión que permite interactuar con Jenkins
	private String getToken(){
		def sessionKeyFile = new File(jenkinsHome, "portalSessionKey")
		log "Consultando el fichero $sessionKeyFile ..."
		def sessionKey = "";
		sessionKeyFile.eachLine { line ->
			sessionKey = line
			return
		}
		def token = "{\"sessionKey\":\"${sessionKey}\"}"
		log "Token de sesión de QUVE: $token"
		return token;
	}

	// Llama a un servicio de QUVE
	public String sendQuve(baseurl,path,entity,contentType){
		def query = [:]
		query.put("quvetoken", getToken())
		def http = new HTTPBuilder(baseurl)
		http.request( POST, TEXT ) { req ->
			headers.Accept = 'application/json'
			uri.path = path
			uri.query = query
			requestContentType = contentType
			req.entity = entity
			response.failure = { resp, reader ->
				if (reader!=null)
					println reader.text
				throw new Exception("Server Error: ${resp.status}")
			}
			response.success = { resp, reader ->
				return reader.text
			}
		}
	}
}
