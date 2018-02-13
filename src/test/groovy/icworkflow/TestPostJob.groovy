package icworkflow


import groovy.io.FileType
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.*
import static groovyx.net.http.ContentType.*
import groovy.json.*

import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.entity.StringEntity

import groovyx.net.http.HTTPBuilder

import org.junit.Test


class TestPostJob {


	//@Test
	public void testPostJob() {
		String baseurl = "http://portalgrupo.elcorteingles.pre/icqaportal/executions/"
		File json = new File("c:/temp/20160614/file.json");
		File zip = new File("c:/temp/20160614/file.zip");

		def query = [:]
		query.put("quvetoken", "{\"sessionKey\":\"055ef85e-5480-477a-bc97-cf36d8256ab0\"}")

		def user = ""
		def pass = ""
		def timeout = 60000
		def path = "steps"
		String ret = "";
		
		
		MultipartEntity entity = new MultipartEntity()
		entity.addPart("file", new FileBody(zip))
		entity.addPart("updateCommand",new StringBody(json.text))
		
		HTTPBuilder http = new HTTPBuilder(baseurl)
		http.auth.basic user,pass
		
		// Timeout de 60 seg. para evitar que una llamada se quede colgada bloqueando a las demás
		http.client.getParams().setParameter("http.socket.timeout", timeout);
		//http.setProxy('localhost', 8080, 'http')
		http.request( POST, TEXT ) { req ->
			headers.Accept = 'application/json'
			uri.path = path
			uri.query = query
			requestContentType = "multipart/form-data"
			req.entity = entity
			response.failure = { resp, reader ->
				if (reader!=null)
					println reader.text
				ant.delete(dir: dirPortal)
				throw new Exception("Server Error: ${resp.status}")
			}
			response.success = { resp, reader ->
				ret = reader.text
			}
		}
		
		println ret
	}
}
