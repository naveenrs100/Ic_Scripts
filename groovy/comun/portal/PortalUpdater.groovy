//$JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/portal/PortalUpdater.groovy
@GrabResolver(name='nexuseci', root='http://nexus.elcorteingles.int/content/groups/public/')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')
@Grab(group='org.apache.httpcomponents', module='httpmime', version='4.4')

//imports

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.*
import static groovyx.net.http.ContentType.*
import groovy.json.*

import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody

// Parametros de entrada

workspace = args[0]
jobType = args[1]
quveurl = args[2]
jenkinsHome = args[4]
jsonTxt = "{"+
  "\"executionUuid\": \"${args[3]}\","+
    "\"result\": {"+
        "\"duration\": 1000,"+
        "\"version\": null,"+
        "\"baseline\": null,"+
        "\"log\": \"Trabajo finalizado\","+
        "\"finalStatus\": \"NOT_BUILT\""+
    "},"+
    "\"description\": \"Parada a mano\""+
"}";

//Variables calculadas

jobFilesName = "jobFiles.zip"
nameJson = "portal.json"
ant = new AntBuilder()

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


def sendHttp(baseurl, path,entity, contentType, user, pass){
  
  def query = [:]
  query.put("quvetoken", getToken())
  
  def http = new HTTPBuilder(baseurl)
  http.auth.basic user,pass
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

def zipJobFiles(jobFiles){
  def file = new File("dummy")
  file << ""
  ant.zip(
    destfile: jobFiles,
    basedir: workspace,
    level: 9,
    encoding: "UTF-8",
    excludes: "*.groovy"
  )
}


//-------------------------


def jobFiles = new File(jobFilesName)

jsonInfo = zipJobFiles(jobFiles)

MultipartEntity entity = new MultipartEntity()
entity.addPart("file", new FileBody(jobFiles))
entity.addPart("updateCommand",new StringBody(jsonTxt))

sendHttp("${quveurl}/executions/",jobType,entity,"multipart/form-data","","")