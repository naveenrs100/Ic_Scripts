//$JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/jenkins/config/writeComponents.groovy
import groovy.json.*
import es.eci.utils.*
import groovy.text.*

// ------- Objetos

//Hojas excel separadas
def acciones = ["BUILD", "DEPLOY", "RELEASE" , "FIX"]
technologies = ["gradle","maven"]

// ------- Funciones

def getTecnology(component){
	if (component.gradle.toInteger()!=0){
		return technologies[0]
	}
	if (component.maven.toInteger()!=0){
		if (component.component.toLowerCase().indexOf("rft")!=-1)
			return "rft"
		return technologies[1]
	}
	return "other"
}

def getConfigFromJson(configJobs,componentsJsonFile){
	def componentsFile = new File("${componentsJsonFile}")
	if (componentsFile.exists()){
		def text = new StringBuffer()
		componentsFile.eachLine { line -> text << line}
		def streams = new JsonSlurper().parseText(text.toString())
		streams.each { stream ->
			stream.components.each { component ->
				configJobs.add(["stream": "${stream.name}", "component": "${component.component}","tecnology": "${getTecnology(component)}"])
			}
		}
	}
	return configJobs
}

def calculate(configJobs, acciones){
	configJobs.each { config ->
		config.put("name", "${StringUtil.clean(config.stream)} -COMP- ${StringUtil.clean(config.component)}")
		acciones.each { accion ->
			config.put((String)"workspaceRTC_${accion.toLowerCase()}", "WSR - ${config.stream} - ${accion} - IC")
		}
		config.put("workspaceDir", "${StringUtil.cleanBlank(config.stream)}_${StringUtil.cleanBlank(config.component)}")
	}
	return configJobs
}

/**
 * IMPORTANTE CHARSET encoding: Jenkins está configurado con ISO-8859-1 por ello al leer la plantilla
 * (componente_config.xml) lo hace con este juego de caracteres, la única manera de poner acentos
 * dentro es poniendo la plantilla (componente_config.xml) en ISO-8859-1 y a la hora de escribir
 * hacerlo con UTF-8
 */
def writeConfig(config,jenkinsHome,defaults){
	if (config.tecnology=="maven" || config.tecnology=="gradle"){
		def outDir = "${jenkinsHome}/jobs/${config.name}"
		println "writing config.xml for ${config.name}"
		config = ConfigReader.setDefaults(config, defaults.get((String)config.tecnology))
		def templateFile = new File("${jenkinsHome}/jobs/ScriptsCore/workspace/templates/jenkins/componente_config.xml")
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
}

// ------- Variables ---

def jenkinsHome	= args[0]
def adminFile = args[1]
def workspace = args[2]
def componentsJson = args[3]

def configExcel = "${jenkinsHome}/jobs/JenkinsConfiguration/workspace/${adminFile}"
def componentsJsonFile = "${workspace}/${componentsJson}"

// ------- Ejecución ----

def configJobs = []
configJobs = getConfigFromJson(configJobs, componentsJsonFile)
configJobs = ConfigReader.getConfigFromExcel(configJobs, configExcel, "STREAM")
configJobs = ConfigReader.getConfigFromExcel(configJobs, configExcel, "COMPONENT")
acciones.each { accion ->
	configJobs = ConfigReader.getConfigFromExcel(configJobs, configExcel, "STREAM ${accion}",accion.toLowerCase())
	configJobs = ConfigReader.getConfigFromExcel(configJobs, configExcel, "COMPONENT ${accion}",accion.toLowerCase())
}
configJobs = calculate(configJobs,acciones)

def defaults = ConfigReader.getDefaults(configExcel, technologies)
configJobs.each { config ->
	writeConfig(config,jenkinsHome,defaults)
}