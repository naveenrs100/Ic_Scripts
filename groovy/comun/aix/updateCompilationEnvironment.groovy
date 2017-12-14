package aix

import es.eci.utils.ComponentVersionHelper
import es.eci.utils.EnvironmentCatalog
import es.eci.utils.NexusHelper
import es.eci.utils.Stopwatch
import es.eci.utils.TarHelper
import es.eci.utils.TmpDir
import es.eci.utils.transfer.FTPClient

/**
 * Este script descarga el componente compilado, en la versión indicada, al entorno de compilación
 * en la máquina destino, actualizando el entorno indicado (normal o storeflow).
 * 
 * El propósito de este script es prescindir de un directorio /jenkins/entornosCompilacion
 * en la máquina master de jenkins, e ir atando cabos sueltos de un proceso con demasiados elementos
 * innecesarios.
 * 
 * Además, en release, actualiza el fichero compila*.env adecuado cerrando la versión de la biblioteca.
 */

import groovy.io.FileType
import groovy.io.FileVisitResult
 
/*
 * Busca el componente descargado de nexus en el directorio indicado
 */
def File buscarFicheroTar(File directorio, String component) {
	File ret = null
	directorio.traverse (
		type : FileType.FILES,
		maxDepth: 1) { fichero ->
			def nombre = fichero.getName().toLowerCase()
			if (nombre.contains(component.toLowerCase()) && nombre.endsWith(".tar")) {
				ret = fichero
			}
	}				
	return ret
}

// Copia remota por FTP a la máquina de compilación
// Copias: lista de arrays de dos posiciones: fichero y ruta
def flushFTP(usuario, password, copias, String maquina) {
	
	FTPClient client = new FTPClient(usuario, password, maquina);
	client.initLogger({println it})
    try {      

		copias.each { copia ->
			File fichero = copia[0]
			String ruta = copia[1]
			client.copy(fichero, ruta)
		}
		
		client.flush()
    } catch (IOException e) {
        e.printStackTrace();
    }
}


def stream = build.buildVariableResolver.resolve("stream")
def streamRecortada = stream.replaceAll("-DESARROLLO", "")
def component = build.buildVariableResolver.resolve("component")
def workspace = build.getEnvironment().get("WORKSPACE")
def componenteCatalogo = build.buildVariableResolver.resolve("environmentCatalogC")
def release = build.buildVariableResolver.resolve("release")
def maquina = build.buildVariableResolver.resolve("maquina")
def groupId = 'es.eci.elcorteingles.cc.servidorc.ssp'
def isLibrary = build.buildVariableResolver.resolve("isLibrary")
def urlRTC = build.getEnvironment().get("urlRTC")
def userRTC= build.getEnvironment().get("userRTC") 
def pwdRTC = build.buildVariableResolver.resolve("pwdRTC") 
def workItem = build.buildVariableResolver.resolve("workItem") 
def nexusSnapshotsC = build.getEnvironment().get("C_NEXUS_SNAPSHOTS_URL") 
def nexusReleaseC = build.getEnvironment().get("C_NEXUS_RELEASES_URL") 
	

ComponentVersionHelper cvh = new ComponentVersionHelper()
cvh.initLogger({println it})
println "Leyendo versión de ${stream} - ${component}..."
def version = cvh.getVersion(component, stream, userRTC, pwdRTC, urlRTC)
println "${component}: ${version}"

build.getEnvironment().get
if (release == "true") {
	version = version.replace("-SNAPSHOT", "")	
}

if (isLibrary == 'true') {
	EnvironmentCatalog envCat = new EnvironmentCatalog({println it})  
	
	def pathNexus = ""
	if (release == 'true') {
		pathNexus = nexusReleaseC
	} else {
		pathNexus = nexusSnapshotsC
	}
	
	
	// El componente puede corresponder a uno u otro entorno, y por lo tanto hay que moverlo a uno
	//	u otro sitio
	// Determinar a partir del componente (al vuelo) a qué entorno pertenece el componente
	println("Buscando el entorno al que pertenece $component ...")
	// Esto devuelve 'compila' o 'compila_sf'
	List<String> entornos = envCat.getEnvironments(stream, componenteCatalogo, component, urlRTC, userRTC, pwdRTC)
	println("Listado de entornos:")
	entornos.each { entorno ->
		println "entorno: ${entorno}"
	}
	
	// Descargar el tar de la biblioteca en la versión indicada (cerrada o abierta) a un temporal
	//	y copiarlo por ssh a la máquina de destino
	if (entornos == null || entornos == []) {
		throw new Exception("No se ha podido encontrar en la corriente $stream un fichero *.env en el que el componente $component figure con versión abierta")
	}
	TmpDir.tmp { File dirTar ->
		println("Descargando de nexus ${groupId}:${component}:${version}...")
		long tiempoNexus = Stopwatch.watch {
			NexusHelper.downloadLibraries(groupId, component, version, dirTar.getCanonicalPath(), "tar", pathNexus)
		}
		println("$component descargado de Nexus -> $tiempoNexus mseg")	
		// Encontrar el fichero recién descargado
		File tar = buscarFicheroTar(dirTar, component)
		if (tar != null) {
			TmpDir.tmp { File dirTarDescomprimido ->
				TarHelper.untarFile(tar, dirTarDescomprimido)
				// En dirTarDescomprimido está el tar abierto
				// Encontrar el fichero
				// Para cada .h y .a:
				// Copiar por ssh el fichero a la ruta indicada:
				//	/jenkins/entornosCompilacion/${streamRecortada}/compila
				//	o bien
				//	/jenkins/entornosCompilacion/${streamRecortada}/compila_sf
				// con el usuario ujenkins
				def copias = []
				dirTarDescomprimido.traverse (
					type : FileType.FILES,
					maxDepth: -1) { fichero ->
						def nombre = fichero.getName()
						String ruta = null
						entornos.each { entorno ->
							String destinoCabeceras = 	"/jenkins/entornosCompilacion/${streamRecortada}/${entorno}/LAST/include" 
							String destinoLibs = 		"/jenkins/entornosCompilacion/${streamRecortada}/${entorno}/LAST/lib"				
							if (nombre.endsWith(".h")) {
								ruta = destinoCabeceras
							}
							else if(nombre.endsWith(".a")) {
								ruta = destinoLibs
							}
							if (ruta != null) {
								copias << [ fichero, ruta ]
							}
						}
				}
				long tiempoCopia = Stopwatch.watch {
					flushFTP(build.getEnvironment().get("userAIXDesaC"), build.buildVariableResolver.resolve("pwdAIXDesaC"), copias, maquina)
				}		
				println "Tiempo para copiar todos los ficheros: $tiempoCopia mseg."
			}
		}
	}
	// Si es release: actualizar el fichero de entorno y hacer scm checkin
	if (release == 'true') {
		new EnvironmentCatalog({ println it}).closeVersion(stream, componenteCatalogo, component, version, entornos, workItem, urlRTC, userRTC, pwdRTC)
	}
}
else {
	println "El componente ${component} no requiere refresco del entorno de compilación al no ser una biblioteca"
}