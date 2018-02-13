package jenkins

/**
 * Versión que devuelve la lista de componentes y no la lista de jobs a partir
 * del stream.  Se usa expresamente para el wf nuevo de C, y contiene lógica para identificar
 * las bibliotecas. 
 * 
 * ENTRADAS Y SALIDAS DEL SCRIPT:
 * 
 * Si le viene informada una variable "environmentCatalogC", parsea el fichero
 * y devuelve una variable "compilationEnvironmentC" con la lista de componentes
 * que son bibliotecas.  El DSL del TriggerComponentes recoge luego esa lista
 * y etiqueta los componentes que efectivamente son bibliotecas para que ejecuten el
 * último paso del wf de compilación C (el que refresca el entorno de compilación) 
 */

import es.eci.utils.ComponentVersionHelper;
import es.eci.utils.EnvironmentCatalog;
import groovy.io.FileVisitResult
import groovy.json.*
import hudson.model.*

public class Artifact {
	public String version
	public String groupId
	public String artifactId
	public boolean equals (object){
		if (object!=null){
			if (object.groupId==groupId && object.artifactId == this.artifactId)
				return true
		}
		return false
	}
}

//---- SCRIPT ------------>

def stream = build.buildVariableResolver.resolve("stream")
def streamTarget = build.buildVariableResolver.resolve("streamTarget")
def action = build.buildVariableResolver.resolve("action")
def onlyChanges = build.buildVariableResolver.resolve("onlyChanges")
// En algunos casos podemos especificar que se desea que, si hay cambios en cualquier 
//	componente, se compile toda la corriente
def todos_o_ninguno = build.buildVariableResolver.resolve("todos_o_ninguno")
def makeSnapshot = build.buildVariableResolver.resolve("makeSnapshot")
def retry = build.buildVariableResolver.resolve("retry")
def getOrdered = build.buildVariableResolver.resolve("getOrdered")
def workspaceRTC = build.buildVariableResolver.resolve("workspaceRTC")
def jenkinsHome	= build.getEnvironment(null).get("JENKINS_HOME")
def componentsCompareFile = new File("${build.workspace}/componentsCompare.txt")

def components = null

if (retry=="true"){
	components = getComponents(build.workspace)
	if (components!=null){
		componentsCompareFile.delete()
		println "RETRYING.... ${components}"
	}
}

if (componentsCompareFile.exists()){
	components = getComponents(componentsCompareFile, onlyChanges, stream, todos_o_ninguno, build)

	if (makeSnapshot=="true" && components!=null && components.size()>0){
		writeJobsInVersionFile(components,jenkinsHome,build.workspace)
	}
}

if (components!=null){
	def params = []
	if (action!="release")
	params.add(new StringParameterValue("streamTarget",stream))
	// Reordenar para dejar primero el catálogo
	java.util.Collections.sort(components, new java.util.Comparator<String>() {
		int compare(String o1, String o2) {
			if (o1.endsWith("environmentCatalogoC")) {
				return -1;
			}
			else if (o2.endsWith("environmentCatalogoC")) {
				return 1;
			}
			else {
				return o1.compareTo(o2);
			}
		}
	})
	params.add(new StringParameterValue("componentsStream","${getComponentsString(components)}"))
	params.add(new StringParameterValue("homeStream","${build.workspace}"))
	params.add(new StringParameterValue("job", "$stream - COMPNew"))
	//params.add(new StringParameterValue("job", "$stream - COMP"))
	params.add(new StringParameterValue("stream", "$stream"))
	setParams(build,params)
}

if (components==null || components.size()==0){
	build.setResult(Result.NOT_BUILT)
}
else {
	//----- FUNCIONES ESPECÍFICAS DEL WF DE C SERVIDOR ------
	def environmentCatalog = build.buildVariableResolver.resolve("environmentCatalogC")
	if (environmentCatalog != null && environmentCatalog.trim().length() > 0) {
		// En la variable espero el nombre del componente de catálogo dentro de la corriente
		def urlRTC = build.getEnvironment(null).get("urlRTC")
		def userRTC= build.getEnvironment(null).get("userRTC") 
		def pwdRTC = build.buildVariableResolver.resolve("pwdRTC") 
		// Bajar el componente del catálogo a un temporal
		// Parsear ahí todos los ficheros .env
		def envCat = new EnvironmentCatalog({ println it })
		List<String> bibliotecas = envCat.getLibraries(stream, environmentCatalog, urlRTC, userRTC, pwdRTC)
		def params = []
		params.add(new StringParameterValue("compilationEnvironmentC", bibliotecas.join(",")))
		setParams(build,params)
	}
}


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

def getComponentsString(components){
	/*def componentsString = "["
	components.each {componentsString += "'${it}',"}
	componentsString += "]"
	componentsString=componentsString.replace(", ]","]")
	componentsString=componentsString.replace(",]","]")*/
	def componentsString = components.toString()
	return componentsString
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
			if (jobs.find{ j -> j==nombreJob}==null){
				jobs.add(nombreJob)
			}
		}else{
			println "JOB: ${nombreJob} esta disabled en jenkins!"
		}
	}else{
		println "JOB: ${nombreJob} no existe en jenkins!"
	}
	return jobs
}

def getComponents(home){
	def versionFinalFile = new File("${home}/version.txt")
	if (versionFinalFile.exists()){
		def config = new ConfigSlurper().parse(versionFinalFile.toURL())
		evaluate("jobs = ${config.jobs}")
		return jobs!=null && jobs.size()>0?jobs:null
	}
	return null
}

/**
 * Este método decide qué jobs se lanzan en esta construcción.  
 * @param componentsCompareFile Fichero con el log de comparación de cambios
 * @param onlyChanges true/false - Si es true, el disparo de la construcción está
 * condicionado a que haya habido cambios en RTC
 * @param stream Corriente de los jobs a ejecutar
 * @param todos_o_ninguno true/false - Granularidad de la construcción.  
 * @param build Referencia al build de jenkins  
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
def getComponents(componentsCompareFile, onlyChanges, stream, todos_o_ninguno, build){
	def componentsList = []
	def component = null
	List<String> todosComponentes = []
	List<String> componentesConCambios = []
	
	
	def urlRTC = build.getEnvironment(null).get("urlRTC")
	def userRTC= build.getEnvironment(null).get("userRTC") 
	def pwdRTC = build.buildVariableResolver.resolve("pwdRTC") 
	
	// Generar el listado de componentes
	componentsCompareFile.eachLine { line ->
		// Mensaje generado en ocasiones por RTC 5, se descarta
		if (!line.startsWith("Job found still running after platform shutdown.")) {
			if (line.indexOf("Component: (") != -1 || line.indexOf("Component (") != -1) {
				component = line.split("\"")[1]
				// Tomar nota del componente
				if (!todosComponentes.contains(component)) {
					todosComponentes << component
				}
			}
			else if (component!=null && onlyChanges=="true"){
				if (!componentesConCambios.contains(component)) {
					componentesConCambios << component
				}
			}
		}
	}
	println "Components"
	todosComponentes.each { comp ->
		def hayCambios = componentesConCambios.contains(comp)
		println comp + (hayCambios?" -> HAY CAMBIOS":"")
	}
	println "onlyChanges: ${onlyChanges}"
	println "todos o ninguno: ${todos_o_ninguno}"
	// ¿Qué jobs se lanzan?
	def jobs = []

	if (onlyChanges != 'true') {
		// hay que lanzar todo
		todosComponentes.each { c ->
			jobs << c
		}
	}
	else {
		// Solo cambios
		// ¿Granularidad?
		if (todos_o_ninguno == 'true') {
			// Si ha habido algún cambio
			if (componentesConCambios != null && componentesConCambios.size() > 0) {
				// Se compilan todos
				todosComponentes.each { c ->
					jobs << c
				}
			}
		}
		else {
			// solo se lanzan los componentes con cambios
			componentesConCambios.each { c ->
				jobs << c
			}
		}
	}
	
	// Este groovy debe descartar todos aquellos que estén en versión cerrada
	
	def tmp = []
	ComponentVersionHelper cvh = new ComponentVersionHelper()
	
	jobs.each { componente ->
		def v = cvh.getVersion(componente, stream, userRTC, pwdRTC, urlRTC)
		if (!v.endsWith("-SNAPSHOT")) {
			println "--> DESCARTANDO $componente por encontrarse en versión cerrada ($v)"
		}
		else {
			tmp << componente
		}
	}
	
	jobs = tmp

	return jobs
}

def getWorkspace(job, action){
	job = job.replaceAll(" - ","_")
	job = job.replaceAll(" -COMP- ","_")
	job = job.replaceAll(" ","_")
	return "${job}_${action}"
}

def writeJobsInVersionFile(jobs,jenkinsHome,home){
	def versionFinalFile = new File("${home}/version.txt")
	versionFinalFile.delete()
	versionFinalFile << "\ncomponents=\"${getComponentsString(jobs)}\""
}

// #########################################################
//			FUNCIONES SOLO PARA RELEASE
// #########################################################

def writeReactorFiles(jobs,jenkinsHome,home, action){
	def resJobs = []
	def ficherosMaven = []
	//def ficherosGradle = []
	jobs.each { job ->
		def ws = getWorkspace(job,action)
		println("Workspace de $job: $ws")
		def dirComponent = new File("${jenkinsHome}/workspace/${ws}")
		if (dirComponent.exists()){
			def ficheros = getAllFiles("${dirComponent}")
			if (ficheros!=null && ficheros.size()>0){
				if (ficheros[0].getName()=="pom.xml"){
					println "${job} is maven"
					ficherosMaven.addAll(ficheros)
				}else if (ficheros[0].getName()=="build.gradle"){
					//ficherosGradle.addAll(ficheros)
					println "${job} is gradle, implemented only for maven!"
				}
				resJobs.add(job)
			}else{
				println "There is no descriptor file for this component: ${dirComponent}"
			}
		}else{
			println "${dirComponent} doesn't exist, you must execute ${action} before for this component"
		}
	}

	// PARA PROBAR!!!!
	//if (ficherosMaven.size()>0)
	if (ficherosMaven.size()>0 && resJobs.size()>1)
		writeReactorFileMaven(ficherosMaven,home,jenkinsHome,resJobs,action)
	return resJobs
}

// ----- RELEASE MAVEN --------->

def processSnapshotMaven(ficheros,home){
	def result = false
	def err = new StringBuffer()
	def artifacts = getArtifactsMaven(ficheros)
	ficheros.each { fichero ->
		def pom = new XmlParser().parse(fichero)
		pom.dependencies.dependency.each { dependency ->
			def artifact = getArtifactMaven(dependency)
			if (artifact.version!=null){
				if (artifact.version.toLowerCase().indexOf("snapshot")>0){
					if (artifacts.find{it.equals(artifact)}!=null){
						result = true
					}else{
						err << "There is a snapshot version in ${dependency.artifactId.text()} inside ${fichero}\n"
					}
				}
			}
		}
	}
	if (err.length()>0)
		throw new NumberFormatException("${err}")
	// escribe artifacts para ser usado por stepFileVersioner.groovy para quitar los snapshots
	writeJsonArtifactsMaven(artifacts,home)
	return result
}

def writeJsonArtifactsMaven(artifacts,home){
	def file = new File("${home}/artifacts.json")
	file.delete()
	file << "["
	artifacts.each { artifact ->
		file << "{\"version\":\"${artifact.version}\",\"groupId\":\"${artifact.groupId}\",\"artifactId\":\"${artifact.artifactId}\"},"
	}
	file << "]"
}

def getArtifactMaven(pom){
	def artifact = new Artifact()
	artifact.version = pom.version.text()
	if (artifact.version==null || artifact.version.length()==0)
		artifact.version = pom.parent.version.text()
	artifact.artifactId = pom.artifactId.text()
	artifact.groupId = pom.groupId.text()
	if (artifact.groupId==null || artifact.groupId.length()==0)
		artifact.groupId = pom.parent.groupId.text()
	return artifact
}

def getArtifactsMaven(ficheros){
	def artifacts = []
	ficheros.each { fichero ->
		println fichero
		def pom = new XmlParser().parse(fichero)
		artifacts.add(getArtifactMaven(pom))
	}
	return artifacts
}

def writeReactorFileMaven(ficheros,home,jenkinsHome,jobs,action){
	if (processSnapshotMaven(ficheros,home)){
		def l = -1
		def parent = null
		ficheros.each { fichero ->
			def fromDir = "${jenkinsHome}/workspace"
			def destFile = new File("${fichero.getParent().replace(fromDir,home.toString())}/${fichero.getName()}")
			new AntBuilder().copy( file:"${fichero.canonicalPath}", tofile:"${destFile.canonicalPath}")
			def pom = new XmlParser().parse(fichero)
			if (pom.packaging.text()=="pom" || ficheros.size()==1){
				// garantiza que coge el pom más profundo
				if (fichero.getPath().length()<l || l<0){
					parent = pom.parent
					l = fichero.getPath().length()
				}
			}
		}
		createParentReactor(jobs,home, parent,action)
	}
}

def createParentReactor(jobs,home, parent,action) {
	def parentReactor = new File("${home}/pom.xml")
	parentReactor.delete()
	parentReactor << "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
	parentReactor << "\t<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
	parentReactor << "\txsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n"
	parentReactor << "\t\t<modelVersion>4.0.0</modelVersion>\n"
	if (parent!=null){
		parentReactor << "\t\t<groupId>${parent.groupId.text()}</groupId>\n"
		parentReactor << "\t\t<artifactId>${parent.artifactId.text()}</artifactId>\n"
		parentReactor << "\t\t<version>${parent.version.text()}</version>\n"
	}else{
		parentReactor << "\t\t<groupId>es.eci.reactor</groupId>\n"
		parentReactor << "\t\t<artifactId>eci-pom-Reactor</artifactId>\n"
		parentReactor << "\t\t<version>1.0.0</version>\n"
	}

	parentReactor << "\t\t<packaging>pom</packaging>\n"
	parentReactor << "\t\t<modules>\n"
	jobs.each { job ->
		def ws = getWorkspace(job, action)
		parentReactor << "\t\t\t<module>${ws}</module>\n"
	}
	parentReactor << "\t\t</modules>\n"
	parentReactor << "\t</project>\n"
}

//----------------------|

// Todas las Tecnologías ------------

def getAllFiles(dirBase){
	def ficheros = []
	for (def tec in tecnologias){
		println "Probando ${tec.key}..."
		ficheros = getAllFiles(dirBase,tec.value)
		if (ficheros!=null && ficheros.size()>0){
			break
		}
	}
	return ficheros
}

def getAllFiles(fromDirName,fileMatch){
	def fromDir = new File(fromDirName)
	def files = []
	fromDir.traverse(
		type: groovy.io.FileType.FILES,
		preDir: { if (it.name.startsWith(".") || it.name == 'target') return FileVisitResult.SKIP_SUBTREE},
		nameFilter: ~/${fileMatch}/,
		maxDepth: -1
	){
		files << it
	}
	return files
}

// ##########################################
// 				FIN RELEASE
// ##########################################

