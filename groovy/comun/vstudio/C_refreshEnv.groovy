package vstudio
import vs.*
import es.eci.utils.NexusHelper
import es.eci.utils.TmpDir
import es.eci.utils.Utiles
import es.eci.utils.ZipHelper
import groovy.io.FileType
import groovy.io.FileVisitResult

/**
 * Se ejecuta en el esclavo.
 * Este script descarga una biblioteca de nexus y deja:
 * - Cabeceras y libs en el entorno de compilación
 * - Binarios en la ruta indicada del entorno de ejecución (si así lo 
 * indica la variable entornoEjecucionCompleto)
 */
 
def env = System.getenv()
def action = env['action']
def artifactId = env['artifactId']
def groupId = env['groupId']
def workspace = env['WORKSPACE']
def version = env['version']
def stream = env['stream']
def entornoEjecucionCompleto = env['entornoEjecucionCompleto']
// Las bibliotecas en versión cerrada se considera que están compiladas en release 
def modo = 'Release'
def entornoCompilacion = env['WINDOWS_VS_ENTORNO_COMPILACION']
		
// Intenta bajar de nexus un entregable a un directorio temporal
		
println "Versión del entregable: " + version
def isRelease = !version.endsWith("-SNAPSHOT")
		
println "Es release: " + isRelease
def nexusSnapshotsC = env["C_NEXUS_SNAPSHOTS_URL"]
def nexusReleaseC = env["C_NEXUS_RELEASES_URL"]
def pathNexus = ""
if (isRelease) {
	println "Repositorio destino: " + nexusReleaseC
	pathNexus = nexusReleaseC
} 
else {
	println "Repositorio destino: " + nexusSnapshotsC
	pathNexus = nexusSnapshotsC
}

def baseEntorno = "${entornoCompilacion}/${stream}/${modo}/"
String baseEntornoEjecucion = "${workspace}"

TmpDir.tmp { File directorio ->
	File lib = NexusHelper.downloadLibraries(groupId, artifactId, version, directorio.getCanonicalPath(), "zip", pathNexus)
	File xmlEntorno = null;
	// ¿Tiene un xml?
	def patronXML = /.*\.xml/
	ZipHelper.unzipFile(lib, directorio, [patronXML], null)
	File descriptor = new File(directorio, artifactId + ".xml")
	
	C_VS_CompilationEnvironment confEntCompilacion = C_VS_CompilationEnvironment.parsearEntornoCompilacion(descriptor)
	println "Entorno de compilación definido para la biblioteca ->"
	println confEntCompilacion
	// Descomprimir el fichero al entorno de compilación
	if (confEntCompilacion == null) {
		ZipHelper.unzipFile(lib, new File(baseEntorno), [/.+\.lib$/,/.+\.h$/,/.+\.hr$/,/.+\.bmp$/], [/.+\.dll$/])
	}
	else {
		// Descomprimir a un directorio temporal y llevar los .h y los .lib a donde sea necesario
		TmpDir.tmp { File dir ->
			ZipHelper.unzipFile(lib, dir, [/.+\.lib$/,/.+\.h$/,/.+\.hr$/,/.+\.bmp$/], [/.+\.dll$/])
			def patronCabeceras = /.+\.h[r]?$/
			def patronLib = /.+\.lib$/
			def patronBmp = /.+\.bmp$/
			dir.traverse (
				type : FileType.FILES,
				maxDepth: -1) 
			{ it ->
				if (it.getName() =~ patronCabeceras) {
					def dirDestino = null
					dirDestino = [ baseEntorno, confEntCompilacion.include ].join(System.getProperty("file.separator"))		
					println "Copiando ${it.name} a ${dirDestino}..."	
					Utiles.copy(it, dirDestino)
				}
				else if (it.getName() =~ patronLib) {
					def dirDestino = null
					dirDestino = [ baseEntorno, confEntCompilacion.lib ].join(System.getProperty("file.separator"))	
					println "Copiando ${it.name} a ${dirDestino}..."		
					Utiles.copy(it, dirDestino)
				}else if (it.getName() =~ patronBmp) {
					def dirDestino = null
					dirDestino = [ baseEntorno, confEntCompilacion.bitmap ].join(System.getProperty("file.separator"))	
					println "Copiando ${it.name} a ${dirDestino}..."		
					Utiles.copy(it, dirDestino)
				}
			}
		}
	}
	// Descomprimir el binario al entorno de ejecución
	def binarios = [/.+\.bmp$/,/.+\.jpg$/,/.+\.dll$/,/.+\.ocx$/,/.+\.bam$/]
	ZipHelper.unzipFile(lib, directorio, binarios)
	
	// Por defecto, se copia
	if (entornoEjecucionCompleto == null || entornoEjecucionCompleto == '' || entornoEjecucionCompleto == 'true') {
		if (descriptor.exists()) {		
			def xml = new XmlParser().parse(descriptor)
			def directorioBibliotecas = new File(directorio, "lib")
			if (directorioBibliotecas.exists() && directorioBibliotecas.isDirectory()) {
				directorioBibliotecas.eachFile { File directorioPlataforma ->
					xml.lib.platforms.platform.each { platform ->
						def idPlataforma = platform.platId.text()
						if (idPlataforma.equals(directorioPlataforma.getName())) {
							directorioPlataforma.eachFile { biblioteca ->
								def platSubId = ""
								if (platform.platSubId != null && platform.platSubId.text().trim().length() > 0) {
									platSubId = "-" + platform.platSubId.text()
								}
								def rutatar = ""
								if (platform.rutatar != null) {
									rutatar = platform.rutatar.text()
								}
								// Copiarlo a las plataformas indicadas en el fichero XML
								// Ruta de destino
								File rutaDestino = new File(new File(baseEntornoEjecucion), idPlataforma + platSubId + System.getProperty("file.separator") + rutatar)
								File rutaLib = new File(rutaDestino, biblioteca.getName())
								if (!rutaDestino.exists()) {
									rutaDestino.mkdirs()
								}
								if (!rutaLib.exists()) {
									rutaLib.createNewFile()
								}
								println "Copiando ${biblioteca} a ${rutaLib}..."
								rutaLib.bytes = biblioteca.bytes
							}
						}
					}
				}
			}
		}
	}
	lib.delete()
}