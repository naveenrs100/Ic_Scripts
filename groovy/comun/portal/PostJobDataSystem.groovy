//$JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/portal/PostJobData.groovy
import java.nio.channels.Channel
import java.nio.channels.Channels

import es.eci.utils.MultipartUtility;
import es.eci.utils.Stopwatch
import groovy.io.FileType
import groovy.json.*

def resolver = build.buildVariableResolver
// Parametros de entrada
workspace = resolver.resolve("parentWorkspace")
jobType = resolver.resolve("jobInvokerType")
quveurl = build.getEnvironment(null).get("QUVE_URL")
buildNumber = build.number
fileListTxt = resolver.resolve("fileList")
jenkinsHome = build.getEnvironment(null).get("JENKINS_HOME")
workspaceLocal = build.workspace
timeOutStreams = Integer.valueOf(build.getEnvironment(null).get("QUVE_TIMEOUT_STREAMS"))
timeOutComponents = Integer.valueOf(build.getEnvironment(null).get("QUVE_TIMEOUT_COMPONENTS"))
timeOutDefault = Integer.valueOf(build.getEnvironment(null).get("QUVE_TIMEOUT_DEFAULT"))

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

/**
 * Este método implementa la comunicación con QUVE.
 * @param baseurl URL base de QUVE.
 * @param path Indica si notificamos: steps/components/streams/adhoc.
 * @param jsonString Cadena json a enviar a QUVE.
 * @param contentType Tipo de contenido a informar.
 * @param jobFiles Fichero zipeado con la información requerida del job.
 * @param timeout Número de milisegundos a esperar antes de cortar.
 */
def sendHttp(String baseurl,
		String path,
		String jsonString,
		String contentType,
		File jobFiles,
		Integer timeout){
	String ret = null;
	long millis = Stopwatch.watch {

		def url = "${baseurl}/${path}?quvetoken=" + getToken();
		def encoding = "UTF-8"

		if (contentType == "application/json") {
			// Envío del comando
			HttpURLConnection con = new URL(url).openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", contentType);
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.write(jsonString.getBytes(encoding));
			wr.flush();
			wr.close();
			// Resultado
			int responseCode = con.getResponseCode();
			System.out.println("Response Code : " + responseCode);
	
			BufferedReader inReader = new BufferedReader(
			        new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
	
			while ((inputLine = inReader.readLine()) != null) {
				response.append(inputLine);
			}
			inReader.close();
			
			//print result
			ret = response.toString();
		}
		else if (contentType == "multipart/form-data") {	
			// Implementación pura en java
			MultipartUtility util = new MultipartUtility(url, encoding);
			util.addFormField("updateCommand", jsonString);
			if (jobFiles != null) {
				util.addFilePart("file", jobFiles);
			}
			List<String> lines = util.finish();
			// Respuesta del servidor
			lines.each { println it }
		}
	}

	println "sendHttp: $millis ms."
	return ret
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
		dirPortal.eachFileRecurse (FileType.FILES) { file -> println file }
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

if (build.getResult() != hudson.model.Result.SUCCESS) {
	long millis = Stopwatch.watch {
		dirPortal.mkdir();
		def jsonFile = new File(dirPortal,nameJson)
	
		if (jsonFile.exists()){
			try{
				def jsonInfo = new JsonSlurper().parseText(jsonFile.getText("UTF-8"))
	
				if (jsonInfo.type!=null){
	
					//def entity = new StringEntity(new JsonOutput().toJson(jsonInfo), ContentType.APPLICATION_JSON)
					def jsonRespTxt = sendHttp("${quveurl}/executions/","",
							new JsonOutput().toJson(jsonInfo),"application/json", null, TIMEOUT)
	
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
	
					sendHttp("${quveurl}/executions/",jobType, jsonText,"multipart/form-data", jobFiles, TIMEOUT)
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
}
else {
	println "No se ejecuta PostJobData: resultado del job: ${build.getResult()}"
}