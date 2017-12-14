package jenkins

/** 
 * Este script toma el fichero de componentes de RTC (creado por getComponents.sh) y
 * lo parsea para obtener el listado de componentes de la corriente.  Dada esa lista
 * de componentes, obtiene la versión de cada uno consultándola de su buildFile respectivo
 * y, si no está abierta, lo elimina de la lista de jobs que usará el trigger.
 */

import hudson.model.*

import components.ComponentsParser
import components.RTCComponent

import es.eci.utils.ScmCommand
import es.eci.utils.TmpDir

def urlRTC = build.getEnvironment(null).get("urlRTC")
def userRTC= build.getEnvironment(null).get("userRTC") 
def pwdRTC = build.buildVariableResolver.resolve("pwdRTC") 
def tecnologias = ["maven":"pom.xml","gradle":"build.gradle"]
		
/**
 * Extrae la versión del componente a partir del fichero de construcción
 * @param technology maven/gradle
 * @param buildFile Fichero de construcción
 */
def parseVersion(String technology, File buildFile) {
	String ret = ""
	if (buildFile.isFile() && buildFile.exists()) {
		if ("maven".equals(technology)) {
			def pom = new XmlSlurper().parse(buildFile);
			// ¿Tiene versión?
			def version = null
			if (pom.version != null && pom.version.text().trim().length() > 0) {
				version = pom.version.text()
			}
			/*else if (pom.parent.version != null && pom.parent.version.text().length() > 0) {
				version = pom.parent.version.text()
			}*/
			if (version != null) {
				// Puede estar definida como propiedad
				ret = PomVersionHelper.solve(version)
			}
		}
		else if ("gradle".equals(technology)) {
			// Recorrer el build.gradle buscando la línea concreta
			buildFile.eachLine { line ->
				def mVersion = line =~ /.*version\s?=\s?["|'](.*)["|']/
				if (mVersion.matches()) version = mVersion[0][1]
			}	
			ret = version
		}
	}
	return ret
}

// Parsea la cadena de jobs a una lista
def crearLista(String jobs) {
	List<String> ret = new LinkedList<String>()
	if (jobs != null && jobs.trim().length() > 0) {
		def REGEX = /\'([^,\[\]]+)\'/
		def matcher = (jobs =~ REGEX)
		matcher.each { 
			ret << it[1]
		}
	}
	return ret
}

// Crea el nombre normalizado del job correspondiente a un componente
//	en una corriente
def nombreJobComponente(String stream, String comp) {
	def clean = { cadena ->
		cadena = cadena.replaceAll("/","-")
		return cadena
	}
	def nombreJob = "${clean(stream)} -COMP- ${clean(componente)}"
}

// Genera la cadena apropiada para la lista de jobs
def getJobsString(jobs){
	def jobsString = "["
	jobs.each {jobsString += "'${it}',"}
	jobsString += "]"
	return jobsString
}

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
	//println "-------PARAMETROS RESULTANTES--------"
	//paramsTmp.each() { println " ${it}" };
	//println "-------------------------------"
	build?.actions.add(new ParametersAction(paramsTmp))
}

/**
 * Limpia de la lista de componentes aquellas líneas correspondientes a componentes cerrados
 * @param stream Corriente del componente
 * @param technology maven/gradle
 * @param componentsCompareFile Fichero de componentes generado por getComponents.sh
 * @param listaJobs Lista de cadenas con los jobs a ejecutar (parámetro de entrada/salida)
 * @param userRTC Usuario RTC
 * @param pwdRTC Password del usuario RTC
 * @param urlRTC URL del repositorio RTC
 * @param tecnologias Mapa con las tecnologías disponibles
 */
def purgeJobs(String stream, String technology, File componentsCompareFile, List<String> listaJobs, String userRTC, String pwdRTC, String urlRTC, Map tecnologias) {
	if (componentsCompareFile.exists()) {
		// Copia temporal del fichero		
		
		List<RTCComponent> componentes = new ComponentsParser().parse(componentsCompareFile)
		
		// Para cada componente, ejecutar la descarga del fichero de construcción
		componentes.each { comp ->
			// En un directorio temporal
			TmpDir.tmp { dir ->
				// Descarga del fichero de construcción
				try {
					new ScmCommand().ejecutarComando("load \"${stream}\" \"${comp.nombre}/${tecnologias[technology]}\"", userRTC, pwdRTC, urlRTC, dir)
				}
				catch (Exception e) {
					println "Problemas bajando el fichero de construcción: " + e.getMessage()
				}
				File fichero = new File(dir.getCanonicalPath() + System.getProperty("file.separator") + tecnologias[technology])
				if (fichero.exists()) {
	 				String version = parseVersion(technology, fichero)
					if (!version.endsWith("-SNAPSHOT")) {
						println "Descartando componente ${comp.nombre} debido a que la versión está cerrada en RTC"
						listaJobs.remove(nombreJobComponente(stream, comp.nombre))
					}
				}
			}
		}
	}
}
		
def stream = build.buildVariableResolver.resolve("stream")
def technology = build.buildVariableResolver.resolve("technology")
def componentsCompareFile = new File("${build.workspace}/componentsCompare.txt")
def jobs = build.buildVariableResolver.resolve("jobs")

// Recorrer el fichero de componentes
List<String> listaJobs = crearLista(jobs)
purgeJobs(stream, technology, componentsCompareFile, listaJobs, userRTC, pwdRTC, urlRTC, tecnologias);
def params = []
params.add (new StringParameterValue("jobs", "${getJobsString(listaJobs)}"))
setParams(build, params)
