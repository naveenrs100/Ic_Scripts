/**
 * Función que introduce como variable status el estado de la ejecución del job padre.
 * Util para mostrar resultados en terceras herramientas, como RTC, de los jobs de jenkins.
 */
import hudson.model.*

def build = Thread.currentThread().executable
def causa = build.getCause(Cause.UpstreamCause)

//----- Funciones
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
	paramsTmp.addAll(params)
	
	println "-------PARAMETROS RESULTANTES--------"
	paramsTmp.each() { println " ${it}" };
	println "-------------------------------"
	build?.actions.add(new ParametersAction(paramsTmp))
}
//----

if (causa!=null){
	def nombrePadre = causa.getUpstreamProject()
	def numeroPadre = causa.getUpstreamBuild()
	def padre = Hudson.instance.getJob(nombrePadre).getBuildByNumber(Integer.valueOf(numeroPadre))
	
	println "upstream job: ${padre}" 
	def status = "${padre.getResult()}"
	println "upstream job status: ${status}"

	def params = []
	status = status!="SUCCESS"?"ERROR":"OK"
	params.add(new StringParameterValue("status",status) )
	params.add(new StringParameterValue("pasosEnvio","") )
	setParams(build,params)
}else{
	println "ESTE JOB NECESITA SER LLAMADO SIEMPRE DESDE OTRO!!"
	build.setResult(Result.FAILURE)
}