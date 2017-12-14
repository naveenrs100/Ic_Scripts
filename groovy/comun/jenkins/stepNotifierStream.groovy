package jenkins

import hudson.model.*

def causa = build.getCause(Cause.UpstreamCause)
def jenkinsHome	= build.getEnvironment(null).get("JENKINS_HOME")

//-------- FUNCIONES---

def getWorkspace(job, action){
	job = job.replaceAll(" - ","_")
	job = job.replaceAll(" -COMP- ","_")
	job = job.replaceAll(" ","_")
	return "${job}_${action}"
}

def deleteJob(home,job){
	def versionFinalFile = new File("${home}/version.txt")
	if (versionFinalFile.exists()){
		def config = new ConfigSlurper().parse(versionFinalFile.toURL())
		if (config.jobs!=null){
			if (config.jobs.toString().indexOf("'${job}',")!=-1){
				config.jobs=config.jobs.replace("'${job}',","")
				versionFinalFile.withWriter { writer ->
					config.writeTo(writer)
				}
			}else{
				println "No existe el job: ${job} en ${versionFinalFile}" 
			}
		}
	}
}

def deleteDeploy(jenkinsHome,job){
	def dirComponent = new File("${jenkinsHome}/workspace/${getWorkspace(job,'deploy')}")
	if (dirComponent.exists()){
		println "Borrando ${dirComponent}"
		dirComponent.deleteDir()
	}else{
		println "WARNING: el directorio ${dirComponent} no existe!!"
	}
}

//---------------------

if (causa!=null){
	def nombrePadre = causa.getUpstreamProject()
	def numeroPadre = causa.getUpstreamBuild()
	def padre = Hudson.instance.getJob(nombrePadre).getBuildByNumber(Integer.valueOf(numeroPadre))
	
	def homeStream = build.buildVariableResolver.resolve("homeStream")
	def action = build.buildVariableResolver.resolve("action")
	def delDeploy = build.buildVariableResolver.resolve("deleteDeploy")
	
	if (padre.getResult() == Result.SUCCESS){
		deleteJob(homeStream,nombrePadre)
		// Para release borra el workspace de deploy para que se tenga que ejecutar deploy obligatoriamente
		if (action=="release" && delDeploy!="false")
			deleteDeploy(jenkinsHome,nombrePadre)
	}
}else{
	println "ESTE JOB NECESITA SER LLAMADO SIEMPRE DESDE OTRO!!"
	build.setResult(Result.FAILURE)
}