//Obtiene los jobs que han sido modificados y existen
//$JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/jenkins/setJobsFromStream.groovy
import components.MavenComponent
import es.eci.utils.RTCBuildFileHelper
import es.eci.utils.Stopwatch;
import es.eci.utils.TmpDir
import groovy.io.*
import groovy.json.*
import hudson.model.*

tecnologias = ["maven":"pom\\.xml","gradle":"build\\.gradle"]

def build = Thread.currentThread().executable
def resolver = build.buildVariableResolver



//----- FUNCIONES GENERALES -------

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

// Construye la cadena de jobs (filtrando por los componentes de la release)
def getJobsString(jobs, listaComponentesRelease = null){
	def jobsString = ""
	List<String> tmp = []
	def addValue = { valor ->
		if (listaComponentesRelease == null
		|| listaComponentesRelease.size() == 0
		|| listaComponentesRelease.find { listJob -> listJob.toString().equals(valor.toString()) } != null) {
			tmp << valor
		}
	}
	if (jobs instanceof Map) {
		jobs.keySet().each { key ->
			String valor = jobs[key]
			addValue(valor)
		}
	}
	else {
		jobs.each { valor ->
			addValue(valor)
		}
	}
	if(tmp.size() > 0) {
		for (int i = 0; i < tmp.size(); i++) {
			if (i != 0) {
				jobsString += ','
			}
			jobsString += (tmp[i])
		}
	}
	return jobsString
}

def clean(cadena){
	cadena = cadena.replaceAll("/","-")
	return cadena
}

def addJob(jobs,stream,componente){
	def nombreJob = "${clean(stream)} -COMP- ${clean(componente)}"
	def job = Hudson.instance.getJob(nombreJob)
	if (job!=null){
		if (!job.disabled){
			if (jobs[componente].find{ j -> j==nombreJob}==null){
				jobs.put(componente,nombreJob)
			}
		}else{
			println "JOB: ${nombreJob} esta disabled en jenkins!"
		}
	}else{
		println "JOB: ${nombreJob} no existe en jenkins!"
	}
	return jobs
}

/**
 * Este método decide qué jobs se lanzan en esta construcción.  
 * @param componentsCompareFile Fichero con el log de comparación de cambios
 * @param onlyChanges true/false - Si es true, el disparo de la construcción está
 * condicionado a que haya habido cambios en RTC
 * @param stream Corriente de los jobs a ejecutar
 * @param todos_o_ninguno true/false - Granularidad de la construcción.  
 * 
 * 
 * 					  Ha habido cambios en un componente  	|  No ha habido cambios
 * ----------------------------------------------------------------------------- 
 * onlyChanges &&
 * !todos_o_ninguno	| CONSTRUYE EL COMP. CON CAMBIOS		|	NO CONSTRUYE NADA
 * 
 * 
 * !onlyChanges		|	CONSTRUYE TODO						|	CONSTRUYE TODO
 * 
 * onlyChanges &&	|	CONSTRUYE TODO						| 	NO CONSTRUYE NADA
 * todos_o_ninguno
 */
def getJobs(componentsCompareFile,onlyChanges,stream,todos_o_ninguno){
	def jobs = [:]
	def componente = null
	List<String> todosComponentes = []
	List<String> componentesConCambios = []

	// Generar el listado de componentes
	componentsCompareFile.eachLine { line ->
		// Mensaje generado en ocasiones por RTC 5, se descarta
		if (!line.startsWith("Job found still running after platform shutdown.")) {
			if (line.indexOf("Component: (") != -1 || line.indexOf("Component (") != -1) {
				componente = line.split("\"")[1]
				// Tomar nota del componente
				if (!todosComponentes.contains(componente)) {
					todosComponentes << componente
				}
			}
			else if (componente!=null && onlyChanges=="true"){
				if (!componentesConCambios.contains(componente)) {
					componentesConCambios << componente
				}
			}
		}
	}
	println "Componentes"
	todosComponentes.each { comp ->
		def hayCambios = componentesConCambios.contains(comp)
		println comp + (hayCambios?" -> HAY CAMBIOS":"")
	}
	println "onlyChanges: ${onlyChanges}"
	println "todos o ninguno: ${todos_o_ninguno}"
	// ¿Qué jobs se lanzan?
	if (onlyChanges != 'true') {
		// hay que lanzar todo
		todosComponentes.each { c ->
			jobs = addJob(jobs, stream, c)
		}
	}
	else {
		// Solo cambios
		// ¿Granularidad?
		if (todos_o_ninguno == 'true') {
			// Si ha habido algún cambio
			if (componentesConCambios != null && componentesConCambios.size()) {
				// Se compilan todos
				todosComponentes.each { c ->
					jobs = addJob(jobs, stream, c)
				}
			}
		}
		else {
			// solo se lanzan los componentes con cambios
			componentesConCambios.each { c ->
				jobs = addJob(jobs, stream, c)
			}
		}
	}
	return jobs
}


def writeJobsInVersionFile(jobs,jenkinsHome,home, listaComponentesRelease = null){
	/*def versionFinalFile = new File("${home}/version.txt")
	 versionFinalFile.delete()
	 versionFinalFile << "\njobs=\"${getJobsString(jobs, listaComponentesRelease)}\""*/
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

/**
 * Recorre el baseTmpDir de jenkins recuperando el artifactId de cada
 * uno de los pom.xml de los workspaces de los componentes hijos.
 */
/* def getArtifactsJobsMap(jobs, baseTmpDir){
	def res = [:]
	jobs.each {job ->
		// Aquí job puede valer clave=valor
		// Separar el valor
		String tmpJob = job.toString();
		if (tmpJob.contains("=")) {
			tmpJob = tmpJob.substring(0, tmpJob.indexOf("="))
		}
		def pomFile = getFirstFile("${baseTmpDir.canonicalPath}/${tmpJob}","pom.xml");
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
} */

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

/**
 * @param jobs Lista de jobs
 * @param reactor 
 * @param stream Corriente RTC 
 */ 
def getOrderedJobs(jobs, List<MavenComponent> reactor, stream){
	
	// def artifactsJobsMap = getArtifactsJobsMap(jobs, baseTmpDir)
	
	// def reactorFile = new File("${home}/reactor.log")
	// def orderedComps = []
	// def orderedJobs = []
	// Patrón para buscar los nombres de los artefactos
	// def pattern = /\[INFO\] Building (.*) [\d\.*]+(-SNAPSHOT)*/
	// def artifacts = getArtifactsStream("${build.workspace}")
	/* if (reactorFile!=null && reactorFile.exists()){
		println "reactor.log encontrado"
		reactorFile.eachLine { line ->
			def matcher = line.trim() =~ pattern
			if (matcher.matches()) {
				println line
				// Se asume que el artifactId no tiene espacios
				def identifier = matcher[0][1]
				artifacts.find{a ->
					if (a.artifactId == identifier){
						if (orderedComps.contains(a.component)){
							orderedComps.remove(a.component)
						}
						orderedComps.add(a.component)
					}
				}
			}
		}*/
	
		def orderedJobs = []
	
		println "Componentes ordenados: ${reactor}"
		
		reactor.each { tmp ->
			String componente = tmp.getName()
			if (Hudson.instance.getJob(componente) != null) {
				orderedJobs << componente
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
	//}
	return orderedJobs
}

/**
 * Elimina el último carácter de un String.
 * Lo usamos porque la lista de jobs vendrá
 * con una coma al final que hay que eliminar.
 * @param (String)text
 * @return (String)result
 */
private String removeLastComma(String text) {
	def result;
	if(text.endsWith(",")) {
		result = text.substring(0, text.length() - 1);
	} else {
		result = text;
	}
	return result;
}

/**
 * Devuelve los parámetros de un job formado como "${gitGroup} -COMP- ${component}";
 * @param gitGroup
 * @param component
 * @return
 */
def getJobParameters(jobName) {
	def job = hudson.model.Hudson.instance.getJob(jobName);
	def jobParameters = [];
	if(job != null) {
		job.getProperties().values().each { value ->
			if(value instanceof hudson.model.ParametersDefinitionProperty) {
				jobParameters = value.getParameterDefinitionNames();
			}
		}
	} else {
		println("[WARNING] El job ${jobName} no existe en Jenkins");
	}

	return jobParameters;
}

/**
 * Devuelve un String con los componentesUrbancode de cada job (si este está definido)
 * @param jobs
 * @return String componentsUrban
 */
def getComponentsUrban(jobsString) {
	def componentsUrban = "";
	if(jobsString != null) {
		def jobsList = jobsString.split(",");
		jobsList.each { String thisJob ->
			if(thisJob.contains("-COMP-")) {
				def component = thisJob.split("-COMP- ")[1];
				def job = hudson.model.Hudson.instance.getJob(thisJob);
				def componentUrban;
				def jobParameters = getJobParameters(thisJob);
				if(jobParameters.contains("componenteUrbanCode")) {
					if(job != null) {
						job.getProperties().values().each { value ->
							if(value instanceof hudson.model.ParametersDefinitionProperty) {
								def paramValue = value.getParameterDefinition("componenteUrbanCode").getDefaultParameterValue().getValue();
								if(!paramValue.trim().equals("")) {
									componentUrban = "${component}:" + paramValue
								} else {
									componentUrban = "${component}:NULL";
								}
							}
						}
					}
				} else {
					componentUrban = "${component}:NULL";
				}
				componentsUrban = componentsUrban + componentUrban +","
			}
		}
	}
	return removeLastComma(componentsUrban);
}


// ##########################################
// 				FIN RELEASE
// ##########################################

//---- SCRIPT ------------>

def stream = resolver.resolve("stream")
def streamOrigen = stream
// Se puede necesitar cargar los jobs de una stream de carga inicial, que puede
//	ser ficticia.  Esta necesidad nace de las streams con número variable
//	de componentes, que a efectos de jenkins corresponden a un job único frontal
//	de componente que se invoca desde la corriente con la stream apropiada
def streamCargaInicial = resolver.resolve("streamCargaInicial")
if (streamCargaInicial != null && streamCargaInicial.trim() != '') {
	stream = streamCargaInicial
}
def streamTarget = resolver.resolve("streamTarget")
def action = resolver.resolve("action")
def onlyChanges = resolver.resolve("onlyChanges")
// En algunos casos podemos especificar que se desea que, si hay cambios en cualquier
//	componente, se compile toda la corriente
def todos_o_ninguno = resolver.resolve("todos_o_ninguno")
def makeSnapshot = resolver.resolve("makeSnapshot")
def getOrdered = resolver.resolve("getOrdered")
def workspaceRTC = resolver.resolve("workspaceRTC")
def jenkinsHome	= build.getEnvironment(null).get("JENKINS_HOME")
def componentsCompareFile = new File("${build.workspace}/componentsCompare.txt")
// En el caso, bastante común, de necesitar una release parcial, se aplica este parámetro
// Contiene los componentes que entran en la construcción, separados por comas
String componentesRelease = resolver.resolve("componentesRelease")
List<String> listaComponentesRelease = null
if (componentesRelease != null && componentesRelease.trim().length() > 0) {
	listaComponentesRelease = new LinkedList<String>()
	def partesListaComponentes = componentesRelease.split(",")
	partesListaComponentes.each { tmp ->
		String componente = tmp.trim()
		if (Hudson.instance.getJob(componente) != null) {
			listaComponentesRelease << componente
		}
		else {
			def nombreJob = "${clean(stream)} -COMP- ${clean(componente)}"
			if (Hudson.instance.getJob(nombreJob) != null) {
				listaComponentesRelease << nombreJob
			}
			else {
				println "AVISO: Se ha especificado un componente desconocido: ${componente}"
			}
		}
	}
}

if (listaComponentesRelease != null && listaComponentesRelease.size() > 0) {
	println "================================================================"
	println "--> AVISO: Tomando solo los componentes: "
	listaComponentesRelease.each { comp ->
		println comp
	}
	println "================================================================"
}

def jobs = null

if (componentsCompareFile.exists()){
	jobs = getJobs(componentsCompareFile,onlyChanges,stream,todos_o_ninguno)
	println("Jobs calculados a partir del \"componentsCompare.txt\" -> ${jobs}")
	tecnologias.each { name, pattern ->
		def file = new File("${build.workspace}/${pattern.replace('\\','')}")
		file.delete()
	}

	if (getOrdered=="true" && jobs.size()>0){
		//jobs = writeReactorFiles(jobs,jenkinsHome,build.workspace, action=="release"?"deploy":action)

		String urlRTC = build.getEnvironment(null).get("urlRTC");
		String userRTC= build.getEnvironment(null).get("userRTC");
		String pwdRTC = resolver.resolve("pwdRTC");
		// String mavenExecutable = build.getEnvironment(null).get("MAVEN_HOME") + System.getProperty("file.separator") + "bin/mvn";
		def jobsSinOrden = jobs
		println "Jobs antes del reactor: ${jobsSinOrden}"

		// Construye el reactor al vuelo sobre un directorio temporal
		long millis = Stopwatch.watch {
			TmpDir.tmp { tmp ->
				RTCBuildFileHelper helperReactor = new RTCBuildFileHelper(action, new File(build.workspace.toString()));
				helperReactor.initLogger { println it }
				// Crea el reactor y el artifacts.json al vuelo
				List <MavenComponent> reactor = helperReactor.createStreamReactor(
						tmp,
						streamOrigen,
						"maven",
						userRTC,
						pwdRTC,
						urlRTC,
						componentesRelease);
				// Una vez creados reactor y artifacts.json
				jobs = getOrderedJobs(jobs, reactor, stream)
			}
		}
		println "Construcción del reactor al vuelo: ${millis} mseg."
		
		def jobsTemp = []
		int cont = 0;
		jobs.each {job ->
			jobsSinOrden.each{j ->
				if (j.getValue().equals(job)){
					jobsTemp[cont] = job
					//jobsSinOrden.remove(j.key)
					cont ++
				}
			}
		}

		jobs = jobsTemp

		println "==========================================================="
		println "---> Jobs ordenados: "
		jobs.each { println it }
		println "==========================================================="
	}

	if (makeSnapshot=="true" && jobs!=null && jobs.size()>0) {
		writeJobsInVersionFile(jobs, jenkinsHome, build.workspace, listaComponentesRelease)
	}
}

if (jobs!=null){
	def params = []
	if (action!="release" && action != "addFix" && action != "addHotfix") {
		params.add(new StringParameterValue("streamTarget",stream))
	}
	def jobsString = getJobsString(jobs, listaComponentesRelease);
	params.add(new StringParameterValue("jobs","${jobsString}"))
	params.add(new StringParameterValue("homeStream","${build.workspace}"))
	File fArtifacts = new File(build.workspace.toString() + '/artifacts.json')
	// Así aseguramos que el artifacts.json pasa a los esclavos
	if (fArtifacts.exists()) {
		String artifacts = fArtifacts.text;
		params.add(new StringParameterValue("artifactsFile", artifacts))
		params.add(new StringParameterValue("artifactsJson", artifacts))
	}
	def componentsUrban = getComponentsUrban(jobsString);
	params.add(new StringParameterValue("componentsUrban","${componentsUrban}"));
	setParams(build,params)
}
if (jobs==null || jobs.size()==0){
	build.setResult(Result.NOT_BUILT)
}