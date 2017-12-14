import hudson.model.*
import jenkins.model.*

def workspaceJob = build.getEnvironment(null).get("WORKSPACE")
def mavenHome = build.getEnvironment(null).get("MAVEN_HOME")
def mavenBin = "${mavenHome}/bin/mvn"
def stream = build.buildVariableResolver.resolve("stream")
def componente = build.buildVariableResolver.resolve("component")
def deploy_env = build.buildVariableResolver.resolve("deploy_env")
def deployBasePath = build.getEnvironment(null).get("DEPLOYMENT_BASE_PATH")
def deployProjectPath = "${deployBasePath}/$stream/$componente/$deploy_env"
def mavenPhase = "validate"
def deployPom = "${deployProjectPath}/pom.xml"
def deployProfile = build.getEnvironment(null).get("DEPLOY_PROFILE")
def deployProfileGestionDocumental65 = build.getEnvironment(null).get("DEPLOY_PROFILE_GESTION_DOCUMENTAL_6.5")
def deployProfileGestionDocumental67 = build.getEnvironment(null).get("DEPLOY_PROFILE_GESTION_DOCUMENTAL_6.7")
def deployProfile32Bits = build.getEnvironment(null).get("DEPLOY_PROFILE_32Bits")
def deployProfilePre = build.getEnvironment(null).get("DEPLOY_PROFILE_PRE")
def deployProfilePreGestionDocumental65 = build.getEnvironment(null).get("DEPLOY_PROFILE_PRE_GESTION_DOCUMENTAL_6.5")
def deployProfilePreGestionDocumental67 = build.getEnvironment(null).get("DEPLOY_PROFILE_PRE_GESTION_DOCUMENTAL_6.7")
def deployProfilePre32Bits = build.getEnvironment(null).get("DEPLOY_PROFILE_PRE_32Bits")
def deployProfilePro = build.getEnvironment(null).get("DEPLOY_PROFILE_PRO")
def deployProfile85 = build.getEnvironment(null).get("DEPLOY_PROFILE_8.5")
def deployProfilePre85 = build.getEnvironment(null).get("DEPLOY_PROFILE_PRE_8.5")
def deployProfilePro85 = build.getEnvironment(null).get("DEPLOY_PROFILE_PRO_8.5")
def deployProfileMulticanal = build.getEnvironment(null).get("DEPLOY_PROFILE_MULTICANAL")
def deployProfileIntMulticanal = build.getEnvironment(null).get("DEPLOY_PROFILE_INT_MULTICANAL")
def deployProfilePreMulticanal = build.getEnvironment(null).get("DEPLOY_PROFILE_PRE_MULTICANAL")
def deployProfileProMulticanal = build.getEnvironment(null).get("DEPLOY_PROFILE_PRO_MULTICANAL")
def deployProfileIntegracion85 = build.getEnvironment(null).get("DEPLOY_PROFILE_INTEGRACION_8.5")
def uDeployer ="-DuDeployer="+ build.getEnvironment(null).get("uDeployer")
def uDeployerPass = "-DuDeployerPass="+ build.buildVariableResolver.resolve("uDeployerPass")
def WAS = build.getEnvironment(null).get("WAS")
if (WAS == null){
	WAS = "7"
}
def deployProcCmd = ""
println "Version de was en el que se despliega la aplicacion: ${WAS}"


if (deploy_env != null && deploy_env.equals("PRE") && WAS != null
	&& (WAS.equals("7") || WAS.equals(""))){
	println "Despliegue en el entorno de preproduccion de was 7"
	deployProcCmd = "${mavenBin} -f \"${deployPom}\" ${deployProfilePre} ${uDeployer} ${uDeployerPass} ${mavenPhase}"
}else if (deploy_env != null && deploy_env.equals("PRO") && WAS != null
	&& (WAS.equals("7") || WAS.equals(""))){
	println "Despliegue en el entorno de produccion de was 7"
	deployProcCmd = "${mavenBin} -f \"${deployPom}\" ${deployProfilePro} ${uDeployer} ${uDeployerPass} ${mavenPhase}"
}else if (WAS != null && (WAS.equals("7") || WAS.equals(""))){ 
	println "Despliegue en el entorno de desarrollo de was 7"
	deployProcCmd = "${mavenBin} -f \"${deployPom}\" ${deployProfile} ${uDeployer} ${uDeployerPass} ${mavenPhase}"
}else if (deploy_env != null && deploy_env.equals("PRE") && WAS != null
		&& WAS.equals("7_32")){
		println "Despliegue en el entorno de preproduccion de was 7 con 32 bits"
		deployProcCmd = "${mavenBin} -f \"${deployPom}\" ${deployProfilePre32Bits} ${uDeployer} ${uDeployerPass} ${mavenPhase}"
}else if (WAS != null && (WAS.equals("7_32") || WAS.equals(""))){
	println "Despliegue en el entorno de desarrollo de was 7 con 32 bits"
	deployProcCmd = "${mavenBin} -f \"${deployPom}\" ${deployProfile32Bits} ${uDeployer} ${uDeployerPass} ${mavenPhase}"
}else if (deploy_env != null && deploy_env.equals("PRE") && WAS != null
	&& WAS.equals("8.5")){
	println "Despliegue en el entorno de preproduccion de was 8.5"
	deployProcCmd = "${mavenBin} -f \"${deployPom}\" ${deployProfilePre85} ${uDeployer} ${uDeployerPass} ${mavenPhase}"
}else if (deploy_env != null && deploy_env.equals("PRO") && WAS != null
	&& WAS.equals("8.5")){
	println "Despliegue en el entorno de produccion de was 8.5"
	deployProcCmd = "${mavenBin} -f \"${deployPom}\" ${deployProfilePro85} ${uDeployer} ${uDeployerPass} ${mavenPhase}"
}else if (WAS != null && WAS.equals("8.5")){ 
	println "Despliegue en el entorno de desarrollo de was 8.5"
	deployProcCmd = "${mavenBin} -f \"${deployPom}\" ${deployProfile85} ${uDeployer} ${uDeployerPass} ${mavenPhase}"
}else if (deploy_env != null && deploy_env.equals("PRE") 
	&& WAS != null && WAS.equals("MULTICANAL")){ 
	println "Despliegue en el entorno de preproduccion de was multicanal"
	deployProcCmd = "${mavenBin} -f \"${deployPom}\" ${deployProfilePreMulticanal} ${uDeployer} ${uDeployerPass} ${mavenPhase}"
}else if (deploy_env != null && deploy_env.equals("PRO") 
	&& WAS != null && WAS.equals("MULTICANAL")){ 
	println "Despliegue en el entorno de produccion de was multicanal"
	deployProcCmd = "${mavenBin} -f \"${deployPom}\" ${deployProfileProMulticanal} ${uDeployer} ${uDeployerPass} ${mavenPhase}"
}else if (deploy_env != null && deploy_env.equals("INT") 
	&& WAS != null && WAS.equals("MULTICANAL")){ 
	println "Despliegue en el entorno de integración de was multicanal"
	deployProcCmd = "${mavenBin} -f \"${deployPom}\" ${deployProfileIntMulticanal} ${uDeployer} ${uDeployerPass} ${mavenPhase}"
}else if (WAS != null && WAS.equals("MULTICANAL")){ 
	println "Despliegue en el entorno de desarrollo de was multicanal."
	deployProcCmd = "${mavenBin} -f \"${deployPom}\" ${deployProfileMulticanal} ${uDeployer} ${uDeployerPass} ${mavenPhase}"
}else if (WAS != null && WAS.equals("INTEGRACION85")){ 
	println "Despliegue en el entorno de desarrollo de was 8.5 integracion."
	deployProcCmd = "${mavenBin} -f \"${deployPom}\" ${deployProfileIntegracion85} ${uDeployer} ${uDeployerPass} ${mavenPhase}"
}else if (deploy_env != null && deploy_env.equals("PRE") 
	&& WAS != null && WAS.equals("GESTIONDOCUMENTAL65")){ 
	println "Despliegue en el entorno de preproduccion de was7 gestion documental 6.5."
	deployProcCmd = "${mavenBin} -f \"${deployPom}\" ${deployProfilePreGestionDocumental65} ${uDeployer} ${uDeployerPass} ${mavenPhase}"
}else if (deploy_env != null && deploy_env.equals("PRE") 
	&& WAS != null && WAS.equals("GESTIONDOCUMENTAL67")){ 
	println "Despliegue en el entorno de preproduccion de was7 gestion documental 6.7."
	deployProcCmd = "${mavenBin} -f \"${deployPom}\" ${deployProfilePreGestionDocumental67} ${uDeployer} ${uDeployerPass} ${mavenPhase}"
}else if (WAS != null && WAS.equals("GESTIONDOCUMENTAL65")){
	println "Despliegue en el entorno de desarrollo de was7 de gestion documental 6.5."
	deployProcCmd = "${mavenBin} -f \"${deployPom}\" ${deployProfileGestionDocumental65} ${uDeployer} ${uDeployerPass} ${mavenPhase}"
}else if (WAS != null && WAS.equals("GESTIONDOCUMENTAL67")){
	println "Despliegue en el entorno de desarrollo de was7 de gestion documental 6.7."
	deployProcCmd = "${mavenBin} -f \"${deployPom}\" ${deployProfileGestionDocumental67} ${uDeployer} ${uDeployerPass} ${mavenPhase}"
}


build.getActions().each() {
	if (it instanceof hudson.model.ParametersAction) {
	   build.getActions().remove(it)
	}
}

def paramsTotal = []
paramsTotal.add(new StringParameterValue("deployProcCmd", "${deployProcCmd}"))
build.addAction(new ParametersAction(paramsTotal))