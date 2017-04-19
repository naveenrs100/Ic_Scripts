/** 
 * Este script unifica la invocación a procesos de notificación de portal
 * 
 * GetJobData: obtiene un JSON con toda la información relevante para QUVE
 * del proceso invocante (stream, componente, ad hoc o paso de construcción)
 * PostJobData: envía el JSON al portal QUVE
 */

import es.eci.utils.MultipartUtility;
import es.eci.utils.ParamsHelper
import es.eci.utils.Stopwatch;
import es.eci.utils.TmpDir
import groovy.io.FileType
import groovy.json.*
import hudson.model.*

//-----------------------------------------------------------------------
// Funciones de utilidad
//-----------------------------------------------------------------------

// Genera la información para arrancar un proceso de IC en QUVE
// Devuelve el json a pasar al servicio de ejecución
def generaStart(component, stream, process, jobInvokerType, workItem, instantanea, jobName) {
	def jsonInfo = null
	
	long millis = Stopwatch.watch {
		def types = ["streams": "STREAM", "components" : "COMPONENT", "adhoc":"ADHOC"]
		def type = types[jobInvokerType];
		if (type!=null){
			jsonInfo = [:]
			if (type == "ADHOC") {
				jsonInfo.put("jobName", jobName);
			}
			jsonInfo.put('componentName',component)
			jsonInfo.put('streamName',stream)
			jsonInfo.put('processName',process.toString().toUpperCase())
			jsonInfo.put('type',type)
			jsonInfo.put('workItem',workItem)
			jsonInfo.put('instantanea',instantanea)
			// Incidencia de lanzamientos repetidos: el parámetro scheduled a true
			//	permite a QUVE distinguir cuando jenkins simplemente solicita UUID
			//	correspondiente a una ejecución planificada ya existente, y no
			//	lanzar una nueva ejecución
			jsonInfo.put("scheduled", "true");
		}
	
		println ('Create Json Created...')
		println jsonInfo
	}
	println "generaStart: $millis ms."

	return jsonInfo
}

// Actualiza la información de un proceso existente en QUVE
// Devuelve el json a pasar al servicio de ejecución
def generaUpdate(lastBuild, job, jobInvokerType , 
		executionUuid, component, step, baseline, 
		version, nombrePadre) {
	
	def jsonInfo = [:]
	
	long millis = Stopwatch.watch {
		
		def numeroLineas = 200
	
		//Get Step data
		def runDuration = lastBuild.getDuration()
		def endDate = lastBuild.getTime()
		def runLog = ""
		lastBuild.getLog(numeroLineas).each(){ runLog += "${it}\n" }
		def actions = lastBuild.getActions();
		def jsonTest = [:];
		actions.each() { action ->
			def clase = action.getClass().getName();
			if (clase == "hudson.tasks.junit.TestResultAction"){
				jsonTest.put('junittotal',action.getTotalCount());
				jsonTest.put('junitfailed',action.getFailCount());
				jsonTest.put('junitskiped',action.getSkipCount());
				jsonTest.put('junithealthScaleFactor',action.getHealthScaleFactor());
			}else if (clase == "hudson.plugins.jacoco.JacocoBuildAction"){
				def jacocoResult = action.getResult();
				jsonTest.put('branchCoverage', jacocoResult.branch.percentage);
				jsonTest.put('complexityScore', jacocoResult.complexity.percentage);
				jsonTest.put('instructionCoverage', jacocoResult.instruction.percentage);
				jsonTest.put('methodCoverage', jacocoResult.method.percentage);
				jsonTest.put('lineCoverage', jacocoResult.line.percentage);
				jsonTest.put('classCoverage', jacocoResult.clazz.percentage);
			}
			//TODO: Parsear el action del plugin de cobertura y obtener los resultados como con jacoco
			/*else if (clase == "hudson.plugins.cobertura.CoberturaBuildAction"){
				def coberturaResult = action.getResult();
			}*/
		}
		
		def runResult = lastBuild.getResult().toString()
	
		//Buld json info
		def jsonResult = [:]
		jsonResult.put('duration',runDuration)
		jsonResult.put('version',version)
		jsonResult.put('baseline',baseline)
		jsonResult.put('log',runLog)
		jsonResult.put('finalStatus',runResult)
	
		jsonInfo.put('executionUuid',executionUuid)
		jsonInfo.put('result',jsonResult)
		jsonInfo.put('test',jsonTest)
		jsonInfo.put('componentName',component)
		jsonInfo.put('logPath',lastBuild.getLogFile().getAbsolutePath())
		def description = job.getDescription()
		description = description.substring(0, Math.min(description.length(), 254))
		jsonInfo.put('description',description)
		
		if(jobInvokerType == 'steps') {
			def descHeader = description.split(System.getProperty("line.separator")).find { !it.trim().equals("") }
			def stepCalculated = (descHeader == null || descHeader.trim().equals("")) ? nombrePadre : descHeader;
			println("El stepCalculated es ${stepCalculated} ya que descHeader es ${descHeader}")
			def stepName = step != null ? step : stepCalculated;
			println("stepName es ${stepName} porque step es ${step}")
			jsonInfo.put('stepName', stepName)
		}
		println ('Update Json Created...')
	}
	println "generaUpdate: $millis ms."
	return jsonInfo
}

def isTrue(variable){
	return variable!=null && variable=="true"
}

// Lee el token de sesión de QUVE almacenado en el fichero 
//	/jenkins/portalSessionKey
def getToken(def jenkinsHome){
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
 * @param jenkinsHome Directorio raíz de jenkins
 */
def sendHttp(String baseurl,
		String path,
		String jsonString,
		String contentType,
		File jobFiles,
		Integer timeout,
		def jenkinsHome){
	String ret = null;
	long millis = Stopwatch.watch {

		def url = "${baseurl}/${path}?quvetoken=" + getToken(jenkinsHome);
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

// Crea un zip con los ficheros indicados en el fileList
def zipJobFiles(jsonInfo, jobFiles, fileListTxt, workspace, dirPortal){
	def ant = new AntBuilder()

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


def jsonInfo = null;
//Get Step parameters
def resolver = build.buildVariableResolver

def quveConnection = build.getEnvironment(null).get("QUVE_CONNECTION")

if (isTrue(quveConnection)) {
	long millisGet = Stopwatch.watch {
		
		// -- Parametros que vienen de jobs superiores
		def jobInvokerType = resolver.resolve("jobInvokerType")
		def create = resolver.resolve("create")
		def component = resolver.resolve("component")
		def stream = resolver.resolve("stream")
		def process = resolver.resolve("action")
		def step = resolver.resolve("step")
		def executionUuid = resolver.resolve('executionUuid')
		def baseline = resolver.resolve('baseline')
		def version = resolver.resolve('builtVersion')
		def workItem = resolver.resolve('workItem')
		def instantanea = resolver.resolve('instantanea')
		def jobName = resolver.resolve("jobName")
		def QUVE_STARTER = build.getEnvironment(null).get("QUVE_STARTER")
	
	
		// -- Datos del build invoker
		def causa = build.getCause(Cause.UpstreamCause)
		def nombrePadre = causa.getUpstreamProject()
		def numeroPadre = causa.getUpstreamBuild()
		def job = Hudson.instance.getJob(nombrePadre)
		def lastBuild = job.getBuildByNumber(Integer.valueOf(numeroPadre))
		
		println('Getting info from job: ' + nombrePadre + ', build#: ' + numeroPadre)
		
		println "create: ->${create}<-"
		println "executionUuid: ->${executionUuid}<-"
		
		if (isTrue(QUVE_STARTER) && isTrue(create) && (executionUuid==null || executionUuid.length()==0)) {
			println "--CREATE EXECUTION------------"
			jsonInfo = generaStart(component, stream, process, jobInvokerType, workItem, instantanea,jobName)
		
		} else if (create==null && executionUuid!=null && executionUuid.length()>0) {
			println "--UPDATE EXECUTION-----executionUuid: ${executionUuid}-------"
			jsonInfo = generaUpdate(lastBuild, job,jobInvokerType , executionUuid, component, step, baseline, version, nombrePadre)
		}
		
		if (jsonInfo==null || Result.NOT_BUILT.equals(build.getResult())) {
			println ("No es posible generar peticiones contra el portal")
			build.setResult(Result.NOT_BUILT)
		}
	
	}
	
	println "---> TOTAL GetJobData: $millisGet ms."
	
	if (jsonInfo != null) {
	
		long millisPost = Stopwatch.watch {
			def workspace = resolver.resolve("parentWorkspace")
			def jobType = resolver.resolve("jobInvokerType")
			def quveurl = build.getEnvironment(null).get("QUVE_URL")
			def buildNumber = build.number
			def fileListTxt = resolver.resolve("fileList")
			def jenkinsHome = build.getEnvironment(null).get("JENKINS_HOME")
			def workspaceLocal = build.workspace
			def timeOutStreams = Integer.valueOf(build.getEnvironment(null).get("QUVE_TIMEOUT_STREAMS"))
			def timeOutComponents = Integer.valueOf(build.getEnvironment(null).get("QUVE_TIMEOUT_COMPONENTS"))
			def timeOutDefault = Integer.valueOf(build.getEnvironment(null).get("QUVE_TIMEOUT_DEFAULT"))
			
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
			
			def jobFilesName = "jobFiles.zip"
			def ant = new AntBuilder()
			
			if (build.getResult() != hudson.model.Result.SUCCESS) {
				TmpDir.tmp { File dirPortal ->			
					if (jsonInfo.type!=null){
		
						//def entity = new StringEntity(new JsonOutput().toJson(jsonInfo), ContentType.APPLICATION_JSON)
						def jsonRespTxt = sendHttp("${quveurl}/executions/","",
								new JsonOutput().toJson(jsonInfo),"application/json", null, TIMEOUT,
								jenkinsHome)
						println("jsonResponseTxt -> ${jsonRespTxt} ")
						// Informar el UUID de ejecución en los parámetros del job
						def causa = build.getCause(Cause.UpstreamCause)
						def nombrePadre = causa.getUpstreamProject()
						def numeroPadre = causa.getUpstreamBuild()
						def job = Hudson.instance.getJob(nombrePadre)
						def lastBuild = job.getBuildByNumber(Integer.valueOf(numeroPadre))
						
						def executionJson = new JsonSlurper().parseText(jsonRespTxt);
						def executionUuid = executionJson.executionId;
						ParamsHelper.deleteParams(lastBuild,["executionUuid"].toArray(new String[1]))
						ParamsHelper.addParams(lastBuild,["executionUuid":"${executionUuid}"])
		
					}
					else{
		
						def jobFiles = new File(dirPortal,jobFilesName)
		
						jsonInfo = zipJobFiles(jsonInfo, jobFiles, fileListTxt, workspace, dirPortal)
		
						def jsonText = new JsonOutput().toJson(jsonInfo)
		
						sendHttp("${quveurl}/executions/",jobType, jsonText,
							"multipart/form-data", jobFiles, TIMEOUT,
							jenkinsHome)
					}
				}
			}
			else {
				println "No se ejecuta PostJobData: resultado del job: ${build.getResult()}"
			}
		}
		
		println "---> TOTAL PostJobData: $millisPost ms."
	
	}

}
else {
	println "La conexión con QUVE está desactivada"
}
