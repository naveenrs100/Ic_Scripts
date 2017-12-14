package aix
import groovy.lang.Closure;
import rtc.RTCHelper

import java.text.SimpleDateFormat;

import es.eci.utils.ComponentVersionHelper
import es.eci.utils.NexusHelper
import es.eci.utils.TarHelper
import es.eci.utils.TmpDir
import es.eci.utils.Utiles
import es.eci.utils.transfer.FTPClient

def stream = build.buildVariableResolver.resolve("stream")
def streamTarget = build.buildVariableResolver.resolve("streamTarget")
def componenteCatalogo = build.buildVariableResolver.resolve("environmentCatalogC")
def componentePlantilla = build.buildVariableResolver.resolve("tarTemplateC")
def release = build.buildVariableResolver.resolve("release")
def workspace = build.getEnvironment().get("WORKSPACE")
def groupId = 'es.eci.elcorteingles.cc.servidorc.ssp'
def urlRTC = build.getEnvironment().get("urlRTC")
def userRTC= build.getEnvironment().get("userRTC") 
def pwdRTC = build.buildVariableResolver.resolve("pwdRTC")  
def nexusSnapshotsC = build.getEnvironment().get("C_NEXUS_SNAPSHOTS_URL") 
def nexusReleaseC = build.getEnvironment().get("C_NEXUS_RELEASES_URL") 
def m2home = build.getEnvironment().get("MAVEN_HOME")

def uDeployUser = build.buildVariableResolver.resolve("DEPLOYMENT_USER")
def uDeployPass = build.buildVariableResolver.resolve("DEPLOYMENT_PWD")
def gradleHome = build.getEnvironment().get("GRADLE_HOME")
def gradleBin = "$gradleHome/bin/gradle"
def daemonsHome = build.getEnvironment().get("DAEMONS_HOME")
def cScriptsHome = build.getEnvironment().get("C_SCRIPTS_HOME")
def nexusPublicC = build.getEnvironment().get("C_NEXUS_PUBLIC_URL")
def artifactType = "tar"
def subirFTP = build.getEnvironment().get("subirFTP")
println "SubirFTP: $subirFTP"

// Esta función abre un tar, eliminando los directorios que queden vacíos
void explotar(File f) {
	File dir = f.getParentFile()
	TarHelper.untarFile(f, dir)
	f.delete()
	// Cruzar los directorios y eliminarlos si están vacíos
	dir.traverse(maxDepth: 0, type: groovy.io.FileType.DIRECTORIES) { File directory ->
		if (directory.list().length == 0) {
			// Eliminarlo si está vacío
			directory.deleteDir()
		}
	}
	// Si existe un fichero GROUPID.info, lo elimina también
	File gid = new File(dir.getCanonicalPath() + System.getProperty("file.separator") + "GROUPID.info")
	if (gid.exists()) {
		gid.delete()
	}
}

// FTP de producción
String rutaFTP = build.getEnvironment().get("CC_PRODUCCION_FTP_DIR")
String direccionFTP = build.getEnvironment().get("CC_PRODUCCION_FTP")
String usuarioFTP = build.getEnvironment().get("CC_PRODUCCION_FTP_USER")
String pwdFTP = build.buildVariableResolver.resolve("CC_PRODUCCION_FTP_PWD")


// Sacar los componentes de la corriente
RTCHelper rtcHelper = new RTCHelper(userRTC, pwdRTC, urlRTC)
rtcHelper.initLogger { println it }
File daemonConfigDir = null;
List<String> componentesCorriente = null;
// Sacar la versión de cada componente
Map<String, String> componentesVersiones = new HashMap<String, String>()
TmpDir.tmp { File daemonDir ->
	daemonConfigDir = new File(daemonsHome + "/" + daemonDir.getCanonicalPath())
	componentesCorriente = rtcHelper.listComponents(stream, daemonDir)
	ComponentVersionHelper cvh = new ComponentVersionHelper()
	componentesCorriente.each { componente ->
		componentesVersiones.put(componente, cvh.getVersion(componente, stream, userRTC, pwdRTC, urlRTC))
	}
	println "Descargando los tars:"
	componentesVersiones.keySet().each { key ->
		println "${key} -> ${componentesVersiones[key]}"
	}
}
if (daemonConfigDir != null && daemonConfigDir.exists()) {
	println "Eliminando directorio de configuración del daemon: " + daemonConfigDir
	daemonConfigDir.deleteDir();
}
// Sobre dir se compone el entregable.  Luego se construirá un tar completo sobre el directorio dir
//	(además de los tar parciales sobre cada subdirectorio de dir)
TmpDir.tmp { File dir ->
	// Montar la plantilla sobre un temporal
	def pathNexusPlantilla = ""
	if (!componentesVersiones.get(componentePlantilla).endsWith("-SNAPSHOT")) {
		pathNexusPlantilla = nexusReleaseC
	} else {
		pathNexusPlantilla = nexusSnapshotsC
	}
	File fichPlantilla = NexusHelper.downloadLibraries(groupId, componentePlantilla, componentesVersiones.get(componentePlantilla), dir.getCanonicalPath(), "tar", pathNexusPlantilla)
	// Descomprimir el tar en local y eliminarlo a continuación
	explotar(fichPlantilla);
	// Bajarlos de nexus y agruparlos en el directorio temporal de la plantilla
	componentesCorriente.each { componente ->
		if (componente != componenteCatalogo && componente != componentePlantilla) {
			String version = componentesVersiones.get(componente)
			def pathNexus = ""
			if (!version.endsWith("-SNAPSHOT")) {
				pathNexus = nexusReleaseC
			} else {
				pathNexus = nexusSnapshotsC
			}
			TmpDir.tmp { File dirTarComponente ->
				// Mezclar sobre el directorio entregable según el deploy.properties
				File tar = NexusHelper.downloadLibraries(groupId, componente, version, dirTarComponente.getCanonicalPath(), "tar", pathNexus)
				TarHelper.untarFile(tar, dirTarComponente)
				tar.delete()
				File deployProperties = new File(dirTarComponente.getCanonicalPath() + System.getProperty("file.separator") + "deploy.properties")
				if (deployProperties.exists()) {
					def props = new Properties()
					def commandList2 = []
					deployProperties.withInputStream {propiedad -> props.load(propiedad)}
					props.each{propiedad, valor  ->
						// Cada propiedad se corresponde con un fichero o bien un directorio del directorio dirTarComponente
						// Cada valor es una ruta de destino, relativa al directorio dir (donde estamos componiendo el tar final)
						File origen = new File("${dirTarComponente.canonicalPath}/${propiedad}")
						File destino = new File("${dir}/${valor}")
						if (origen.exists()) {
							if (origen.isDirectory()) {
								destino.mkdirs()
								Utiles.copyDirectories(origen, destino)
							}
							else if (origen.isFile()) {
								destino.mkdirs()
								File fDest = new File(destino.getCanonicalPath() + System.getProperty("file.separator") + origen.getName())
								fDest.createNewFile()
								fDest.bytes = origen.bytes
							}
						}
					}
				}
			}
		}
	}
	// Agrupar con la versión del environment y subir a Nexus
	TmpDir.tmp { File dirEntregable ->
		String versionTar = componentesVersiones.get(componenteCatalogo)
		List<File> tars = []
		// En el caso de la release, se sube a FTP un tar por cada directorio de primer nivel contenido en el tar completo
		// Efectivamente, se sube a FTP información repetida; y ya puestos, podríamos decir que subir a FTP el entregable
		//	es innecesario dado que se sube a nexus.  Así y todo, esto es petición de producción
		if (subirFTP.toString().toLowerCase() == "true") {
			// Tars parciales
			File[] directorios = dir.listFiles()
			directorios.each { directorio ->
				if (directorio.isDirectory()) {
					String nombreTar = "${streamTarget}-${directorio.name}-${versionTar}.tar"
					File tarParcial = new File(dirEntregable, nombreTar)
					TarHelper.tarFolder(directorio, tarParcial)
					// Si es release, luego habrá que utilizar esta lista para pasarlos por FTP
					tars << tarParcial
				}
			}
		}
		// Tar completo
		String nombreTar = "${streamTarget}-${versionTar}.tar"
		File tarCompleto = new File(dirEntregable, nombreTar)
		TarHelper.tarFile(dir, tarCompleto)
		// Subirlo a nexus
		def pathNexus = null
		if (!versionTar.endsWith("-SNAPSHOT")) {
			pathNexus = nexusReleaseC
		} else {
			pathNexus = nexusSnapshotsC
		}
		NexusHelper.uploadTarNexus(uDeployUser, uDeployPass, gradleBin, cScriptsHome, nexusPublicC, groupId, streamTarget, versionTar, pathNexus, release, tarCompleto.getCanonicalPath(), artifactType, { println it })
		// Si es release, luego habrá que utilizar esta lista para pasarlos por FTP
		tars << tarCompleto
		println "subirFTP: " + subirFTP.toString().toLowerCase()
		String fecha = new SimpleDateFormat("yyyyMMdd").format(new java.util.Date())
		if (subirFTP.toString().toLowerCase() == "true") {
			String rutaCompletaFTP = "${rutaFTP}/${fecha}-${streamTarget}/${versionTar}"
			println "Desplegando tar por FTP a ${direccionFTP} en la ruta ${rutaCompletaFTP} ..."
			// Lanzar por ftp
			FTPClient client = new FTPClient(usuarioFTP, pwdFTP, direccionFTP)
			client.initLogger { println it }
			tars.each { File tar ->
				println "Copiando ${tar.name} ... "
				client.copy(tar, rutaCompletaFTP)
			}
			client.flush()
		}
	}
}