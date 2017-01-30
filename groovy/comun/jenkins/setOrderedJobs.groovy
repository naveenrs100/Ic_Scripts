// OBSOLETO
// La funcionalidad queda incluida en setJobsFromStreamLight

//Ordena los jobs segÃºn la validaciÃ³n de MAVEN
//Formatea el string jobs para que lo entienda Trigger
//$JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/jenkins/setOrderedJobs.groovy
import groovy.io.*
import groovy.json.JsonSlurper
import hudson.model.*

// --- Funciones -----
def clean(cadena){
	cadena = cadena.replaceAll("/","-")
	return cadena
}

def setParams(build,params){
	def paramsIn = build?.actions.find{ it instanceof ParametersAction }?.parameters
	def index = build?.actions.findIndexOf{ it instanceof ParametersAction }
	def paramsTmp = []
	if (paramsIn!=null){
		//No se borra nada para compatibilidad hacia atrÃ¡s.
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

def getWorkspace(job, action){
	job = job.replaceAll(" - ","_")
	job = job.replaceAll(" -COMP- ","_")
	job = job.replaceAll(" ","_")
	return "${job}_${action}"
}

/**
 * Recorre el home de jenkins recuperando el artifactId de cada
 * uno de los pom.xml de los workspaces de los componentes hijos.
 * Para entrar en cada workspace, compone el nombre con la acciÃ³n
 * en la que estamos (build, deploy, etc.)
 */
def getArtifactsJobsMap(jobs, home, action){
	def res = [:]
	jobs.each {job ->
		def pomFile = getFirstFile("${home}/${getWorkspace(job,action)}","pom.xml")
		println "intenta obtener artifact de: ${pomFile}"
		if (pomFile.exists()){
			def pom = new XmlParser().parse(pomFile)
			res.put(pom.artifactId.text(),job)
			println "asocia: ${pom.artifactId.text()} a ${job}"
			// maven muestra en el reactor la propiedad name si estÃ¡ informada.  Se
			//	incluye este fragmento para corregir este problema
			if (pom.name != null && pom.name.text() != null) {
				res.put(pom.name.text(), job)
			}
			
		}
	}
	return res
}

def getOrderedJobs(jobs,home, action, stream){
	def artifactsJobsMap = getArtifactsJobsMap(jobs, home, action)
	def reactorFile = new File("${home}/reactor.log")
	def orderedComps = []
	def orderedJobs = []
	// PatrÃ³n para buscar los nombres de los artefactos
	def pattern = /\[INFO\] Building (.*) [\d\.*]+(-SNAPSHOT)*/
	def artifacts = getArtifactsStream("${build.workspace}")
	if (reactorFile!=null && reactorFile.exists()){
		reactorFile.eachLine { line ->
		def matcher = line.trim() =~ pattern
		if (matcher.matches()) {
			println line
			// Se asume que el artifactId no tiene espacios
			//def artifactId = line.split()[2]
			def identifier = matcher[0][1]
					artifacts.find{a ->
					if (a.artifactId == identifier){
						if (orderedComps.contains (a.component)){
							orderedComps.remove(a.component)
						}
						orderedComps.add(a.component)
					}
				}
			}
		}
		orderedComps.each { tmp ->
			String componente = tmp.trim()
			if (Hudson.instance.getJob(componente) != null) {
				orderedJob << componente
			}
			else {
				def nombreJob = "${clean(stream)} -COMP- ${clean(componente)}"
				if (Hudson.instance.getJob(nombreJob) != null) {
					orderedJobs << nombreJob
				}
				else {
					println "AVISO: Se ha especificado un componente desconocido: ${componente}"
				}
			}
		}
	}
	return orderedJobs
}

def getJobsString(jobs){
	jobsString = ""
	jobs.each { jobsString += "${it},"}
	return jobsString
}

def getArtifactsStream(homeStream){
	def artifactsFile = new File("${homeStream}/artifacts.json")
	def artifacts = []
	if (artifactsFile.exists()){
		def text = new StringBuffer()
		artifactsFile.eachLine { line -> text << line}
		artifacts = new JsonSlurper().parseText(text.toString())
	}
	return artifacts
}

def getFirstFile(fromDirName,fileMatch){
	def fromDir = new File(fromDirName)
	def file = null
	def size = 0
	fromDir.traverse(
		type: groovy.io.FileType.FILES,
		preDir: { if (it.name.startsWith(".") || it.name == 'target') return FileVisitResult.SKIP_SUBTREE},
		nameFilter: ~/${fileMatch}/,
		maxDepth: -1
	){
		if (size==0 || it.getPath().length()<size){
			file = it
			size = file.getPath().length()
		}
	}
	return file
}

// --- Script ---------

def build = Thread.currentThread().executable
def resolver = build.buildVariableResolver
def snapshot = resolver.resolve("snapshot")
def stream = resolver.resolve("stream")
def action = resolver.resolve("action")
def getOrdered = resolver.resolve("getOrdered")
def jenkinsHome	= build.getEnvironment(null).get("JENKINS_HOME")

def jobsString = resolver.resolve("jobs")
evaluate("jobs = ${jobsString}")

println "Inicio setOrderedJobs."
def reactorLog = new File("${build.workspace}/reactor.log")
println "Se lee el reactorLog."
println "Lista de jobs: " + jobs.size()
println "Valor de getOrdered: ${getOrdered}"
println "Existe reactorLog: " + reactorLog.exists() 
if (jobs.size()>1 && reactorLog.exists() && getOrdered=="true"){
	jobs = getOrderedJobs(jobs,build.workspace, action=="release"?"deploy":action,stream)
	println "Lista ordenada de jobs: " + jobs
}

def params = []
params.add(new StringParameterValue("jobs","${getJobsString(jobs)}") )
setParams(build,params)