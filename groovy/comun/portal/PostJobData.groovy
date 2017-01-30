//$JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/portal/PostJobData.groovy
import es.eci.utils.Stopwatch;
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

workspace = args[0]
jobType = args[1]
quveurl = args[2]
buildNumber = args[3]
fileListTxt = args[4]
jenkinsHome = args[5]
workspaceLocal = args[6]
timeOutStreams = Integer.valueOf(args[7])
timeOutComponents = Integer.valueOf(args[8])
timeOutDefault = Integer.valueOf(args[9])

// Eficiencia: se pone un timeout mínimo para todas las notificaciones que no
//	sean de corriente
Integer TIMEOUT =  timeOutDefault
if (jobType == "streams") {
	TIMEOUT =  timeOutStreams
}
if (jobType == "components") {
	TIMEOUT = timeOutComponents
}

//Variables calculadas

jobFilesName = "jobFiles.zip"
nameJson = "portal_${buildNumber}.json"
dirPortal = new File("${workspaceLocal}/portal");
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

def sendHttp(baseurl, path,entity, contentType, user, pass, timeout){
	
  def ret = null; 
  long millis = Stopwatch.watch {
	
	  def query = [:]
	  query.put("quvetoken", getToken())
	  
	  def http = new HTTPBuilder(baseurl)
	  http.auth.basic user,pass
	  // Timeout de 60 seg. para evitar que una llamada se quede colgada bloqueando a las demás
	  http.client.getParams().setParameter("http.socket.timeout", timeout);
	  http.request( POST, TEXT ) { req ->
		headers.Accept = 'application/json'
		uri.path = path
		uri.query = query
		requestContentType = contentType
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
  
  }
  
  println "sendHttp: $millis ms."
  return ret;
}

def zipJobFiles(jsonInfo, jobFiles){
  
  long millis = Stopwatch.watch {
	
	  def fileMap = [:]
	  if (fileListTxt!=null && fileListTxt.length()>0) {
		  // Citando la documentación del paso:
		  /*
		   * - fileList (OPCIONAL): Lista de nombres de ficheros/directorios a enviar al portal, separados por coma (el log del job padre siempre se incluye): 
	  			file1.txt, file2.txt, file3.txt, reportsDir. En el caso de directorios, debe ser la ruta relativa al workspace del padre (por ejemplo, target/surefire-reports 
		   */
		  def tmpBaseDir = new File(workspace)
		  def tmpFileNames = fileListTxt.split(',')
		  int idx = 0;
		  tmpFileNames.each { def tmpFileName ->
			  File tmpFile = new File(tmpBaseDir, tmpFileName)
			  println "Comprobando ${tmpFile.canonicalPath} ..."
			  if (tmpFile.exists()) {
				  fileMap[idx++] = tmpFileName
			  }
			  else {
				  println "${tmpFile.canonicalPath} no existe"
			  }
		  }
	  }
	  fileMap.put("Log",jsonInfo.get("logPath"))
	
	  List<String> keys = []
	  keys.addAll(fileMap.keySet());
	  // Copia todo al directorio Portal para comprimirlo
	  keys.each(){ name ->
		def path = fileMap.get(name)
		def file = new File(path)
	
		//Comprobar si es relativo
		if (!file.exists())
		  file = new File("${workspace}/${path}")
	
		if (file.exists()){
	
		  if (file.isDirectory()){
			  ant.copy(todir: "${dirPortal}/${file.getName()}") {
				  fileset(dir: "$file.canonicalPath")
			  }
			  ant.zip(
				destfile: "${dirPortal}/${file.getName()}.zip",
				basedir: "${dirPortal}/${file.getName()}",
				level: 9,
				encoding: "UTF-8",
				excludes: "${dirPortal}/${file.getName()}.zip"
			  )
			  ant.delete(dir: "${dirPortal}/${file.getName()}")
		  }else{
		  	  def extension = ""
			  if (name == "Log") {
				  extension = ".txt"
			  }
			  ant.copy( file:"$file.canonicalPath", tofile:"${dirPortal}/${file.getName()}${extension}")
		  }
		  file = new File("${dirPortal}/${file.getName()}")
		  fileMap.put(name,file.getName());
		}else{
		  println "${name}-->NO existe!! no se puede enviar al portal"
		  fileMap.remove(name);
		}
	  }
	
	  println "Empaquetando ${dirPortal} para enviar al portal"
	  dirPortal.eachFileRecurse (FileType.FILES) { file ->
		  println file
	  }
	  ant.delete(file: jobFiles, failonerror: false)
	  ant.zip(
		destfile: jobFiles,
		basedir: dirPortal,
		level: 9,
		encoding: "UTF-8",
		excludes: "*.json"
	  )
	  jsonInfo.remove("logPath")
  
  }
  
  println "zipJobFiles: $millis ms."
  return jsonInfo
}


//-------------------------


long millis = Stopwatch.watch {
	dirPortal.mkdir();
	def jsonFile = new File(dirPortal,nameJson)
	
	if (jsonFile.exists()){
	  try{
		def jsonInfo = new JsonSlurper().parseText(jsonFile.getText("UTF-8"))
		
		if (jsonInfo.type!=null){
		  
		  def entity = new StringEntity(new JsonOutput().toJson(jsonInfo), ContentType.APPLICATION_JSON)
		  def jsonRespTxt = sendHttp("${quveurl}/executions/","",entity,"application/json","","", TIMEOUT)
	
		  def executionFile = new File ("${workspaceLocal}/execution.json")
		  if (executionFile.exists()){
			println("Elimina fichero con executionUuid")
			  executionFile.delete();
		  }
		  println("escribe fichero con executionUuid")
		  executionFile.write(jsonRespTxt,"UTF-8")
	
		}else{
		  
		  def jobFiles = new File(dirPortal,jobFilesName)
		 
		  jsonInfo = zipJobFiles(jsonInfo, jobFiles)
		  
		  def jsonText = new JsonOutput().toJson(jsonInfo)
		  MultipartEntity entity = new MultipartEntity()
		  entity.addPart("file", new FileBody(jobFiles))
		  entity.addPart("updateCommand",new StringBody(jsonText))
	   
		  sendHttp("${quveurl}/executions/",jobType,entity,"multipart/form-data","","", TIMEOUT)
		}
	  }finally{
		  // Elimina el directorio portal
		ant.delete(dir: dirPortal)
	  }
	}else{
	  println "fichero '${dirPortal}/${nameJson}' no encontrado"
	}
}

println "---> TOTAL PostJobData: $millis ms."