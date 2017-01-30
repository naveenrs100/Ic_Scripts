//$JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/portal/GetJobData.groovy
import es.eci.utils.Stopwatch;
import hudson.model.*
import groovy.json.*

long millis = Stopwatch.watch {

	//Get Step parameters
	def resolver = build.buildVariableResolver
	numeroLineas = 200
	nameJson = "portal_${build.number}.json"
	
	// -- Parametros que vienen de jobs superiores
	def jobInvokerType = resolver.resolve("jobInvokerType")
	def create = resolver.resolve("create")
	def component = resolver.resolve("component")
	def stream = resolver.resolve("stream")
	def process = resolver.resolve("action")
	def step = resolver.resolve("step")
	def executionUuid = resolver.resolve('executionUuid')
	def baseline = resolver.resolve('baseline')
	def version = resolver.resolve('version')
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
	def jsonInfo = null;
	
	println "create: ->${create}<-"
	println "executionUuid: ->${executionUuid}<-"
	
	if (isTrue(QUVE_STARTER) && isTrue(create) && (executionUuid==null || executionUuid.length()==0)) {
		println "--CREATE EXECUTION------------"
		jsonInfo = generaStart(component, stream, process, jobInvokerType, workItem, instantanea,jobName)
	
	} else if (create==null && executionUuid!=null && executionUuid.length()>0) {
		println "--UPDATE EXECUTION-----executionUuid: ${executionUuid}-------"
		jsonInfo = generaUpdate(lastBuild, job,jobInvokerType , executionUuid, component, step, baseline, version, nombrePadre)
	}
	
	if (jsonInfo!=null && !Result.NOT_BUILT.equals(build.getResult())) {
		new File("${build.workspace}/portal").mkdir()
		def jsonFile = new File ("${build.workspace}/portal/${nameJson}")
		if (!jsonFile.exists()) {
			jsonFile.getParentFile().mkdirs();
			jsonFile.createNewFile();
		}
		jsonFile.write(new JsonOutput().toJson(jsonInfo),"UTF-8")
	} else {
		println ("No es posible generar peticiones contra el portal")
		build.setResult(Result.NOT_BUILT)
	}

}

println "---> TOTAL GetJobData: $millis ms."


// ----------- Functions --------------

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
		}
	
		println ('Create Json Created...')
	}
	println "generaStart: $millis ms."

	return jsonInfo
}

def generaUpdate(lastBuild, job, jobInvokerType , executionUuid, component, step, baseline, version, nombrePadre) {
	
	def jsonInfo = [:]
	
	long millis = Stopwatch.watch {
	
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
