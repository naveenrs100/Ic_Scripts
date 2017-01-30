//$JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/maven/was/prepareDeploy.groovy
import hudson.model.*
import jenkins.model.*
import es.eci.utils.*

def copyDir = { originalDirPath, copyDirPath ->
	println "copyFile: ${originalDirPath}, ${copyDirPath}"
	( new AntBuilder ( ) ).copy( toDir : copyDirPath, overwrite : 'true', verbose : 'true' ){
		fileset(dir : originalDirPath)
	}
}

def build = Thread.currentThread().executable
def resolver = build.getBuildVariableResolver()
def workspaceJob = build.getEnvironment(null).get("WORKSPACE")
def mavenHome = build.getEnvironment(null).get("MAVEN_HOME")
def mavenBin = "${mavenHome}/bin/mvn"
def stream = resolver.resolve("stream")
def componente = resolver.resolve("component")
def deployBasePath = build.getEnvironment(null).get("DEPLOYMENT_BASE_PATH")
def uDeployerPass = resolver.resolve("uDeployerPass")
def deploy_env = resolver.resolve("deploy_env")
def buildFilePath =  resolver.resolve("buildFilePath")

if (resolver.resolve("jobInvoker").equals(build.getEnvironment(null).get("MAVEN_GENERIC_DEPLOYER"))) return 0;

println "Prepare deploy ..."
println "stream: $stream"
println "componente: $componente"
println "deploy_env: $deploy_env"
println "mvn path: $mavenBin"
println "deployBasePath: $deployBasePath"
println "workspaceJob: $workspaceJob"
println "buildFilePath: $buildFilePath"
def modelPath = workspaceJob + "/target/model/" + stream + "/" + componente + "/" + deploy_env

def deployProjectPath = "${deployBasePath}/$stream/$componente/$deploy_env"
File deployProjectDir = new File(deployProjectPath.toString())
if (!deployProjectDir.exists()){
	deployProjectDir.mkdirs()
}

if (new File(modelPath).exists())
	copyDir(modelPath.toString(),deployProjectPath.toString())
else{
	println "NO EXISTE CONFIGURACIÓN DE DESPLIEGUE PARA ESTE COMPONENTE"
	def params = []
	params.add(new StringParameterValue("deployAborted", "true"))
	JenkinsUtils.setParams(build,params,false)
}