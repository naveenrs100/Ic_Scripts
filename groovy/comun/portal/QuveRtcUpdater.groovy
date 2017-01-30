//$JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/portal/QuveRtcUpdater.groovy
@GrabResolver(name='nexuseci', root='http://nexus.elcorteingles.int/content/groups/public/')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')
@Grab(group='org.apache.httpcomponents', module='httpmime', version='4.4')

//imports

import groovy.io.FileType
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.*
import static groovyx.net.http.ContentType.*
import groovy.json.*
import java.util.UUID

import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.entity.StringEntity

// Parametros de entrada

quveurl = args[0]
jenkinsHome = args[1]
workspace = args[2]

//------ funciones --------

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

def sendQuve(baseurl,path,entity,contentType){
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

//-------------------------

def base = new File(workspace)
if (base.exists() && base.isDirectory()){
	def files = []
	base.listFiles().sort{ it.name }.reverse().each { file ->
	  println "Processing ${file.name}..."
		if ("all.json".equals(file.name)){
			def all = new JsonSlurper().parseText(file.getText("UTF-8"))
			def json = new JsonOutput().toJson(all);
			def entity = new StringEntity(json, ContentType.APPLICATION_JSON)
			sendQuve("${quveurl}/","scm",entity,"multipart/form-data")
		}else if (file.name.startsWith("_")){
			def one = new JsonSlurper().parseText(file.getText("UTF-8"))
			def json = new JsonOutput().toJson(one);
			def entity = new StringEntity(json, ContentType.APPLICATION_JSON)
			sendQuve("${quveurl}/","scm/${file.name}",entity,"multipart/form-data")
		}
	}
}