package aix

import es.eci.utils.ComponentVersionHelper
import es.eci.utils.NexusHelper
import es.eci.utils.Utiles

/**
 * Este script contiene la implementación del paso de subida a nexus de los
 * entregables para el workflow nuevo de C AIX
 */

def stream = build.buildVariableResolver.resolve("stream")
def component = build.buildVariableResolver.resolve("component")
def workspace = build.getEnvironment().get("WORKSPACE")


def urlRTC = build.getEnvironment().get("urlRTC")
def userRTC= build.getEnvironment().get("userRTC") 
def pwdRTC = build.buildVariableResolver.resolve("pwdRTC") 

def uDeployUser = build.buildVariableResolver.resolve("DEPLOYMENT_USER")
def uDeployPass = build.buildVariableResolver.resolve("DEPLOYMENT_PWD")
def gradleHome = build.getEnvironment().get("GRADLE_HOME")
def gradleBin = "$gradleHome/bin/gradle"
def cScriptsHome = build.getEnvironment().get("C_SCRIPTS_HOME")
def nexusPublicC = build.getEnvironment().get("C_NEXUS_PUBLIC_URL")

def release = build.getEnvironment().get("release")
	
ComponentVersionHelper componentVersionHelper = new ComponentVersionHelper()

String version = componentVersionHelper.getVersion(component, stream, userRTC, pwdRTC, urlRTC)
println('Current version:' + version)

println('Enviando a Nexus...')
println('Using stream= ' + stream)
println('Using component= ' + component)
println('Using workspace=' + workspace)

groupId='es.eci.elcorteingles.cc.servidorc.ssp'

// Almacenar la versión en el fichero version.txt
Utiles.creaVersionTxt(version, groupId, workspace.toString())

artifactId=component
filePath = workspace + '/' + component + '_RESULT.tar'
File artifactPath = new File(filePath)
println('Deploying file: ' + filePath)

boolean isRelease = (release == "true")
def nexusSnapshotsC = build.getEnvironment().get("C_NEXUS_SNAPSHOTS_URL")
def nexusReleaseC = build.getEnvironment().get("C_NEXUS_RELEASES_URL")
def pathNexus = ""
if (isRelease) {
	println "Repositorio destino: " + nexusReleaseC
	version = version.replaceAll("-SNAPSHOT", "")
	pathNexus = nexusReleaseC
} else {
	println "Repositorio destino: " + nexusSnapshotsC
	pathNexus = nexusSnapshotsC
}

type='tar'
println('argumentos a uploadTarNexus:')
println "-> " + uDeployUser
println "-> " + uDeployPass
println "-> " + gradleBin
println "-> " + cScriptsHome
println "-> " + nexusPublicC
println "-> " + groupId
println "-> " + artifactId
println "-> " + version
println "-> " + pathNexus
println "-> " + version.endsWith("-SNAPSHOT")?"false":"true"
println "-> " + filePath
println "-> " + "tar"
NexusHelper.uploadTarNexus(uDeployUser, uDeployPass, gradleBin, cScriptsHome, nexusPublicC, groupId, artifactId, version, pathNexus, release, filePath, "tar", { println it })
