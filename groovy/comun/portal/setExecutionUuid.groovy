//$JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/portal/setExecutionUuid.groovy
import hudson.model.*
import groovy.json.*
import es.eci.utils.Stopwatch;

def setParams(build,params){
  def paramsIn = build?.actions.find{ it instanceof ParametersAction }?.parameters
  def index = build?.actions.findIndexOf{ it instanceof ParametersAction }
  def paramsTmp = []
  if (paramsIn!=null){
	//No se borra nada para compatibilidad hacia atrás.
	paramsTmp.addAll(paramsIn)
	//Borra de la lista los paramaterAction
	build?.actions.remove(index)
  }
  paramsTmp.removeAll { paramTmp -> params.find(){param -> param.getName().equals(paramTmp.getName())}!=null}
  paramsTmp.addAll(params)
  println "-------PARAMETROS RESULTANTES--------"
  paramsTmp.each() { println " ${it}" }
  println "-------------------------------"
  build?.actions.add(new ParametersAction(paramsTmp))
}

if (build.getResult() != hudson.model.Result.SUCCESS) {
	long millis = Stopwatch.watch {
		
		
		def causa = build.getCause(Cause.UpstreamCause)
		def nombrePadre = causa.getUpstreamProject()
		def numeroPadre = causa.getUpstreamBuild()
		def job = Hudson.instance.getJob(nombrePadre)
		def lastBuild = job.getBuildByNumber(Integer.valueOf(numeroPadre))
		
		def executionFile = new File ("${build.workspace}/execution.json")
		if (executionFile.exists()){
		  def executionJson = new JsonSlurper().parseText(executionFile.getText("UTF-8"))
		  def executionUuid = executionJson.executionId;
		  def params = []
		  params.add(new StringParameterValue("executionUuid","${executionUuid}"))
		  setParams(lastBuild,params)
		}else{
		  println "${executionFile} doesn't exists!!"
		}
		new AntBuilder().delete(file: executionFile)
	}
	
	println "---> TOTAL setExecutionUuid: $millis ms."
}
else {
	println "No se ejecuta PostJobData: resultado del job: ${build.getResult()}"
}