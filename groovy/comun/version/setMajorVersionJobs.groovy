//Obtiene los jobs que han sido modificados y existen
//$JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/version/setMajorVersionJobs.groovy
import hudson.model.*

def build = Thread.currentThread().executable
def resolver = build.buildVariableResolver

// --- FUNCIONES ---

def getWorkspace(job, action){
	job = job.replaceAll(" - ","_")
	job = job.replaceAll(" -COMP- ","_")
	job = job.replaceAll(" ","_")
	return "${job}_${action}"
}

def writeMajorVersionFile(jobs,jenkinsHome,home,action){
	def versions = []
	jobs.each { job ->
		def versionFile = new File("${jenkinsHome}/workspace/${getWorkspace(job,action)}/version.txt")
		println "setMajorVersionJobs :: Tratando de leer el fichero ${versionFile.canonicalPath} ..."
		if (versionFile!=null && versionFile.exists()){
			def config = new ConfigSlurper().parse(versionFile.toURL())
			versions.add(config.version)
		}else{
			println "WARNING: ${versionFile} doesn't exists for ${job}"
		}
	}
	versions.sort()
	def versionFinalFile = new File("${home}/version.txt")
	versionFinalFile.delete()
	if (versions.size()>=1) {
		println "setMajorVersionJobs :: Actualizando el fichero ${versionFinalFile.canonicalPath} ..."
		versionFinalFile << "version=\"${versions[versions.size()-1]}\""
	}
}

def action = resolver.resolve("action")
def jenkinsHome	= build.getEnvironment(null).get("JENKINS_HOME")
def jobs = resolver.resolve("jobs")

println "setMajorVersionJobs :: Lista de jobs -> ${jobs}"

if (jobs!=null && jobs.length()>0)
	writeMajorVersionFile(jobs.split(","),jenkinsHome,build.workspace,action)