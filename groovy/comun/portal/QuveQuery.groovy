package portal

@GrabResolver(name='nexuseci', root='http://nexus.elcorteingles.int/content/groups/public/')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')
@Grab(group='org.apache.httpcomponents', module='httpmime', version='4.4')

//imports
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

// Parametros de entrada

request = args[0]
quveurl = args[1]
jenkinsHome = args[2]
fileName = args[3]

// FUNCTION---- (a meter en librería para evitar copypaste)

def getToken(){
  def sessionKeyFile = new File(jenkinsHome, "portalSessionKey")
  def sessionKey = "";
  sessionKeyFile.eachLine { line ->
	sessionKey = line
	return
  }
  def token = "{\"sessionKey\":\"${sessionKey}\"}"
  return token;
}

def sendHttp(baseurl, user, pass){
  
  println "baseurl: ${baseurl}"
  def query = [:]
  query.put("quvetoken", getToken())
  
  def http = new HTTPBuilder(baseurl)
  http.auth.basic user,pass
  // Timeout de 60 seg. para evitar que una llamada se quede colgada bloqueando a las demás
  http.client.getParams().setParameter("http.socket.timeout", new Integer(60000));
  http.request( GET, TEXT ) { req ->
	headers.Accept = 'application/json'
	uri.query = query
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

// ---------- INICIO SCRIPT

def jsonRespTxt = sendHttp("${quveurl}/${request}","","")
def file = new File(fileName)
file.write(jsonRespTxt,"UTF-8");