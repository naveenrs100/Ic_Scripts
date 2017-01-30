import hudson.model.*

def build = Thread.currentThread().executable
def paramsIn = build?.actions.find{ it instanceof ParametersAction }?.parameters
def index = build?.actions.findIndexOf{ it instanceof ParametersAction }
def resolver = build.buildVariableResolver

//Obtiene parametros pasados al Job
def parentWorkspace = resolver.resolve("parentWorkspace")

def configFile = new File("${parentWorkspace}/version.txt")
if (configFile.exists()){
	def config = new ConfigSlurper().parse(configFile.toURL())

	println "-------ANTES--------"
	paramsIn.each() { println " ${it}" };
	println "---------------"

	def params = []
	if (paramsIn!=null){
		//No se borra nada para compatibilidad hacia atrás.
		params.addAll(paramsIn)
		//Borra de la lista los paramaterAction
		build?.actions.remove(index)
	}

	params.add(new StringParameterValue("groupId",config.groupId))
	params.add(new StringParameterValue("version",config.version))

	println "-------DESPUES--------"
	params.each() { println " ${it}" };
	println "---------------"

	//introduce los nuevos parametros
	build?.actions.add(new ParametersAction(params))
}else{
	println "WARNING: Imposible encontrar fichero ${parentWorkspace}/version.txt, debes usar save=true en stepFileVersioner"
}