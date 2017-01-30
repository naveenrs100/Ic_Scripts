//$JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/jenkins/config/writeStreams.groovy
import es.eci.utils.*
import groovy.json.*
import groovy.text.*

// ------- Objetos

//Hojas excel separadas
def acciones = ["BUILD", "DEPLOY", "RELEASE" , "FIX"]

// ------- Valores por defecto

def getOnlyChanges(valor, option){
	if (valor==null || valor==""){
		switch (option){
			case "build":
				return true
			case "deploy":
				return true
			case "release":
				return false
			case "addFix":
				return false
			case "addHotfix":
				return false
		}
	}
	return valor
}

def getGetOrdered (valor, option){
	if (valor==null || valor==""){
		switch (option){
			case "build":
				return false
			case "deploy":
				return false
			case "release":
				return true
			case "addFix":
				return false
			case "addHotfix":
				return false
		}
	}
	return valor
}

def getMakeSnapshot(valor, option){
	if (valor==null || valor==""){
		switch (option){
			case "build":
				return false
			case "deploy":
				return true
			case "release":
				return true
			case "addFix":
				return true
			case "addHotfix":
				return true
		}
	}
	return valor
}

def getRetry(valor, option){
	if (valor==null || valor==""){
		switch (option){
			case "build":
				return false
			case "deploy":
				return false
			case "release":
				return true
			case "addFix":
				return true
			case "addHotfix":
				return true
		}
	}
	return valor
}

def getTiming(valor){
	def res = valor
	if (res!=null || valor==""){
		def tmp = new StringBuffer()
		tmp << "<hudson.triggers.TimerTrigger>"
		tmp << "<spec>${res}</spec>"
		tmp << "</hudson.triggers.TimerTrigger>"
		res = tmp.toString()
	}
	return res
}

def getDeleteDeploy(valor, option){
	if (valor==null || valor==""){
		switch (option){
			case "build":
				return false
			case "deploy":
				return false
			case "release":
				return true
			case "addFix":
				return false
			case "addHotfix":
				return false
		}
	}
	return valor
}

def transforma (option){
	if (option.startsWith("add"))
		return "fix"
	return option
}

def calculate(configJobs){
	def configJobsFinal = []
	configJobs.each { config ->
		config.get("options").each { option ->
			def s_option = transforma(option)
			def configFinal = [:]
			configFinal.name = "${StringUtil.clean(config.stream)} - ${option}"
			configFinal.stream = config.stream
			configFinal.workItem = config.workItem
			configFinal.streamTarget = config.streamTarget
			configFinal.workspaceRTC = "WSR - ${config.stream} - ${option.toUpperCase()} - IC"
			configFinal.timing = getTiming(config["timing_${s_option}"])
			configFinal.onlyChanges = getOnlyChanges(config["onlyChanges_${s_option}"], option )
			configFinal.makeSnapshot = getMakeSnapshot (config["makeSnapshot_${s_option}"], option)
			configFinal.retry = getRetry (config["retry_${s_option}"], option)
			configFinal.getOrdered = getGetOrdered (config["getOrdered_${s_option}"], option)
			configFinal.deleteDeploy = getDeleteDeploy(config["deleteDeploy_${s_option}"],option)
			configFinal.streamNoW = "${StringUtil.cleanBlank(config.stream)}"
			configFinal.action = option
			configJobsFinal.add(configFinal)
		}
	}
	return configJobsFinal
}

/**
 * IMPORTANTE CHARSET encoding: Jenkins está configurado con ISO-8859-1 por ello al leer la plantilla
 * (triggerStream_config.xml) lo hace con este juego de caracteres, la única manera de poner acentos
 * dentro es poniendo la plantilla (triggerStream_config.xml) en ISO-8859-1 y a la hora de escribir
 * hacerlo con UTF-8
 */
def writeConfig(config,jenkinsHome){
	def outDir = "${jenkinsHome}/jobs/${config.name}"
	println "writing config.xml for ${config.name}"
	def templateFile = new File("${jenkinsHome}/jobs/ScriptsCore/workspace/templates/jenkins/triggerStream_config.xml")
	def engine = new GStringTemplateEngine()
	def result = engine.createTemplate(templateFile).make(config)
	def outFile = new File("${outDir}/config.xml")
	if (outFile.exists()){
		outFile.delete()
	}else{
		new File(outDir).mkdir()
	}
	outFile.write(result.toString(),"UTF-8")
}

// ------- Variables ---

def jenkinsHome	= args[0]
def adminFile = args[1]
def workspace = args[2]

def configExcel = "${jenkinsHome}/jobs/JenkinsConfiguration/workspace/${adminFile}"

// ------- Ejecución ----

def configJobs = []

configJobs = ConfigReader.getConfigFromExcel(configJobs, configExcel, "STREAM")

acciones.each { accion ->
	configJobs = ConfigReader.getConfigFromExcel(configJobs, configExcel, "STREAM ${accion}",accion.toLowerCase())
}

def configJobsFinal = calculate(configJobs)
configJobsFinal.each { config ->
	writeConfig(config,jenkinsHome)
}