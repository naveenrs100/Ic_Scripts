package vstudio
/**
 * Este script se ejecuta de forma externa en el esclavo windows, de forma
 * que no tiene acceso al build.
 * 
 * Construye un componente con tecnología Visual Studio, lanzando el compilador
 * que esté asociado a la tecnología concreta.  Refresca los entornos de compilación
 * asociados.
 * 
 * SALIDA DEL SCRIPT: en caso de ser una release, escribe el version.txt en el 
 * que se apoya el paso stepRTCTagger
 */

import java.text.MessageFormat;

import es.eci.utils.NexusHelper
import es.eci.utils.TmpDir
import es.eci.utils.Utiles
import es.eci.utils.ZipHelper
import groovy.io.FileType
import groovy.io.FileVisitResult
import vs.*

def env = System.getenv()
def action = env['action']
println "action <- ${action}"
String stream = env['stream']
String component = env['component']
String groupId = env['groupId']
String workspace = env['WORKSPACE']
String parentWorkspace = env['parentWorkspace']
String entornoEjecucion = env['entornoEjecucion']
String version = env['version']
if (action == 'release' || action=='addFix') {
	// Cerrar la versión
	version = Utiles.versionCerrada(version);
	// Generar el fichero de version.txt
	Utiles.creaVersionTxt(version, groupId, workspace); 
}

def entornoCompilacion = env['WINDOWS_VS_ENTORNO_COMPILACION']
		
// Variables de entorno para eVC 4
def includeEVC4 = env['WINDOWS_VS_EVC_INCLUDE']
def libEVC4 = env['WINDOWS_VS_EVC_LIB']
def pathEVC4 = env['WINDOWS_VS_EVC_PATH']
def tmpEVC4 = env['WINDOWS_VS_EVC_TMP']
		
// Variables de entorno para vs60
def includeVS60 = env['WINDOWS_VS_60_INCLUDE']
def pathVS60 = env['WINDOWS_VS_60_PATH']
def tmpVS60 = env['WINDOWS_VS_60_TMP']
def libVS60 = env['WINDOWS_VS_60_LIB']
		
// Configuración del entorno de compilación
C_VS_CompilationEnvironment confEntCompilacion = null;


/**
 * Copia los binarios resultado de la compilación al entorno de ejecución
 * @param workspace Directorio base de la biblioteca/aplicación
 * @param entornoEjecucion Directorio base del entorno de ejecución
 * @param entregable Objeto con la información del entregable
 */
def actualizarEntornoEjecucion(String workspace, String entornoEjecucion, C_VS_Deliverable entregable) {
	// Binarios
	def extBinarios = [".exe", ".dll"]
	def esBinario = { extensiones, nombre ->
		boolean ret = false;
		for (String extension: extensiones) {
			ret |= nombre.endsWith(extension)
		}
		return ret;
	}
	// Ruta a la que copiarlos
	def ruta = entregable.ruta
	// Buscar las plataformas generadas en el workspace de proyecto
	def directoriosPlataforma = []
	
	entregable.platforms.each { platform ->
		String rutaPlataforma = platform.platId
		if (platform.platSubId != null && platform.platSubId.length() > 0) {
			rutaPlataforma += ("-" + platform.platSubId)
		}
		File directorioPlataforma = new File([workspace, entregable.ruta, platform.platId].
				join(System.getProperty("file.separator")))
		// Copiar los binarios
		directorioPlataforma.traverse (
			type : FileType.FILES,
			maxDepth: -1) { fichero ->
				def nombre = fichero.getName().toLowerCase()
				if (esBinario(extBinarios, nombre)) {
					String destino = [ entornoEjecucion, rutaPlataforma, 
						platform.rutatar, fichero.getName() ].join(System.getProperty("file.separator"))
					println "Copiando ${nombre} a ${destino}..."
					File fichDestino = new File(destino)
					new File(fichDestino.getParent()).mkdirs()
					if (!fichDestino.exists()) {
						fichDestino.createNewFile()
					}
					fichDestino.bytes = fichero.bytes
				}
		}		
	}
	
}

/**
 * Si lo que hemos compilado es una biblioteca, debemos actualizar el entorno de 
 * compilación.  El entorno se encuentra en 
 * ${entornoCompilacion}/{stream}/{modo}/
 * @param workspace Directorio base de la biblioteca
 * @param stream Nombre de la corriente
 * @param modo Debug/Release
 * @param plataforma Plataforma de compilación
 * @param entornoCompilación Directorio sobre el que se construye el entorno de compilación
 * @param confEntCompilacion Configuración de entorno de compilación (rutas de include y lib) definidas
 * en el xml
 */
def actualizarEntornoCompilacion(String workspace, String stream, 
		String modo, String plataforma, String entornoCompilacion, 
		C_VS_CompilationEnvironment confEntCompilacion) {
	println "Actualizando el entorno de compilación..."
	// Construir la ruta
	def baseEntorno = "${entornoCompilacion}/${stream}/${modo}/"
	def patronCabeceras = /.+\.h[r]?$/
	def patronLib = /.+\.lib$/
	File root = new File(workspace)	
	// Búsqueda de cabeceras en la carpeta del proyecto
	root.traverse (
			type : FileType.FILES,
			maxDepth: -1) 
		{ it ->
			if (it.getName() =~ patronCabeceras) {
				def dirDestino = null
				if (confEntCompilacion == null) {
					dirDestino  = baseEntorno + "include" + System.getProperty("file.separator") + 
						Utiles.rutaRelativa(root, it) + System.getProperty("file.separator")
				}
				else {
					dirDestino = [ baseEntorno, confEntCompilacion.include ].
						join(System.getProperty("file.separator"))							
				}
				println "Copiando ${it.name} a ${dirDestino}..."
				Utiles.copy(it, dirDestino)
			}
		}
	// Búsqueda de bibliotecas en la carpeta del proyecto
	File libRoot = new File(workspace + System.getProperty("file.separator") + plataforma + 
		System.getProperty("file.separator") + modo)
	if (libRoot.exists()) {
		libRoot.traverse (
				type : FileType.FILES,
				maxDepth: -1) 
			{ it ->
				if (it.getName() =~ patronLib) {
					def dirDestino = null 
					if (confEntCompilacion == null) {
						dirDestino  = baseEntorno + "lib"  + 
							System.getProperty("file.separator") + plataforma + 
							System.getProperty("file.separator") +  Utiles.rutaRelativa(libRoot, it) + 
							System.getProperty("file.separator")
					}
					else {
						dirDestino = [ baseEntorno, confEntCompilacion.lib ].
							join(System.getProperty("file.separator"))
					}
					println "Copiando ${it.name} a ${dirDestino}..."
					Utiles.copy(it, dirDestino)
				} 
			}
	}
}

/**
 * Elimina el modo (Debug o Release) del final de una ruta de directorios.
 */
def eliminarModo(String ruta) {
	def ret = ruta;
	if (ruta != null && ruta.trim().length() > 0) {
		String[] partes = ruta.split("\\"+System.getProperty("file.separator"))
		if (partes[partes.length - 1].toLowerCase() == 'release' || partes[partes.length - 1].toLowerCase() == 'debug') {
			ret = partes[0..partes.length - 2].join(System.getProperty("file.separator"))
		}
	}
	return ret;
}

/**
 * Este método va acumulando en el directorio temporal el producto de la compilación de un entregable.
 * @param tempDir Directorio base 
 * @param workspace Directorio de trabajo de la biblioteca
 */
def acumularEntregableLib(File tempDir, String workspace) {
	def patronCabeceras = /.+\.h[r]?$/
	def patronLib = /.+\.lib$/
	def patronDLL = /.+\.dll$/
	
	File root = new File(workspace);
	root.traverse (
			type : FileType.FILES,
			maxDepth: -1) 
		{ it ->
			if (it.getName() =~ patronCabeceras) {
				def dirDestino = [ tempDir.getCanonicalPath(), "include", 
					Utiles.rutaRelativa(root, it)].join(System.getProperty("file.separator")) 
				println "acumularEntregableLib: Copiando ${it.name} a $dirDestino ..."
				Utiles.copy(it, dirDestino)
			}
			if (it.getName() =~ patronLib || it.getName() =~ patronDLL) {
				def dirDestino = [ tempDir.getCanonicalPath(), "lib", 
					eliminarModo(Utiles.rutaRelativa(root, it))].join(System.getProperty("file.separator"))  
				println "acumularEntregableLib: Copiando ${it.name} a $dirDestino ..."
				Utiles.copy(it, dirDestino)
			}
		}
	
}

def acumularXml(File tempDir, String workspace) {
	def patronXml = /.+\.xml$/
	File root = new File(workspace);
	root.traverse (
		type : FileType.FILES,
		maxDepth: 0) 
		{ it ->
			if (it.getName() =~ patronXml) {
					def dirDestino = tempDir.getCanonicalPath() + System.getProperty("file.separator")  
					println "acumularEntregableXml: Copiando ${it.name} a $dirDestino ..."
					Utiles.copy(it, dirDestino)
			}
		}
}

/** 
 * Este método hace un zip del directorio entregable y lo somete a nexus en las
 * coordenadas indicadas en el entregable.
 * @param groupId Coordenadas GAV: grupo
 * @param artifactId Coordenadas GAV: artefacto
 * @param version Coordenadas GAV: versión
 * @parma tempDir Directorio base del zip
 */
def deployNexusEntregable(String groupId, String artifactId, String version, File tempDir) {
	File tmpZip = ZipHelper.addDirToArchive(tempDir) 
	// Desplegar a nexus el fichero
	def env = System.getenv()
	
	//def nexusPublicC = env["C_NEXUS_PUBLIC_URL"]
	
	println "Versión del entregable: " + version
	def isRelease = !version.endsWith("-SNAPSHOT")
			
	println "Es release: " + isRelease
	def nexusSnapshotsC = env["C_NEXUS_SNAPSHOTS_URL"]
	def nexusReleaseC = env["C_NEXUS_RELEASES_URL"]
	def pathNexus = ""
	if (isRelease) {
		println "Repositorio destino: " + nexusReleaseC
		pathNexus = nexusReleaseC
	} else {
		println "Repositorio destino: " + nexusSnapshotsC
		pathNexus = nexusSnapshotsC
	}
	
	NexusHelper.uploadToNexus(env['WINDOWS_VS_MAVEN_ROOT'] + '/bin/mvn.bat', 
		groupId, artifactId, version, tmpZip.getCanonicalPath(), pathNexus, 'zip')
	
	tmpZip.delete()
}

// Este método indica si existe una unidad de compilación en el fichero, es decir,
//	que exista un elemento apps/lib o bien apps/app en el mismo
def existeUnidadCompilacion(File fichero) {
	boolean ret = false;
	if (fichero != null && fichero.isFile() && fichero.exists()) {
		try {
			def xml = new XmlParser().parse(fichero);	
			// Buscar elementos app o lib
			xml.app.each { app ->
				ret = true
			}
			xml.lib.each { lib ->
				ret = true			
			}
		}
		catch (Exception e) {
			println "El fichero ${fichero.getCanonicalPath()} no es un fichero xml"
		}
	}
	return ret;
}

/** 
 * Obtiene el nombre sin ruta ni extensión de un fichero
 */
def obtenerNombre(String file) {
	String ret = file
	int indice = ret.lastIndexOf(System.getProperty("file.separator"))
	if (indice != -1) {
		ret = ret.substring(indice + 1)
	}
	int indicePunto = ret.lastIndexOf(".")
	if (indicePunto != -1) {
		ret = ret.substring(0, indicePunto)
	}
	return ret
}

/**
 * Este método compila un entregable según la tecnología y las plataformas
 * que tenga definido
 * @param workspace Directorio base de la aplicación
 * @param entornoEjecucion Directorio donde se construye el entorno de ejecución
 * @param tecnologias Lista de tecnologías disponibles
 * @param entregable Objeto con la información del entregable
 * @param stream Nombre de la corriente a la que pertenece el entregable
 * @param componente Nombre del componente al que corresponde el entregable
 * @param entornoCompilacion Directorio raíz del entorno de compilación
 * @param confEntCompilacion Configuración de entorno de compilación (rutas de include y lib) definidas
 * en el xml
 * @param tempDir Directorio temporal donde se acumula el entregable para Nexus (bibliotecas)
 */
def compilarEntregable(workspace, entornoEjecucion, tecnologias, entregable, stream, 
		componente, String entornoCompilacion, C_VS_CompilationEnvironment confEntCompilacion, File tempDir)  {
	File rootDir = new File(workspace)
	// ¿Dónde está el makefile/vcproj/etc.?
	println "********* ENTREGABLE: ${entregable.type} ${entregable.ruta} ${entregable.id}"
	File directorioEntregable = new File(workspace + System.getProperty("file.separator") + 
		entregable.ruta)
	println "DIRECTORIO ENTREGABLE: " + directorioEntregable.getCanonicalPath()
	// Validar si había definidos validadores
	if (tecnologias[entregable.ide] != null && tecnologias[entregable.ide].validadores.size() > 0) {
		List<C_Validator> validadores = tecnologias[entregable.ide].validadores
		if (validadores != null) {
			println "Validando el entregable en ${entregable.ruta}..."
			validadores.each { C_Validator validador ->
				validador.validate(directorioEntregable)
			}
		}
	}
	
	String fichero = null;
	
	if (entregable.fichero != null) {
		fichero = entregable.fichero
	}
	else {
		directorioEntregable.traverse(
			type : FileType.FILES,
			maxDepth: 0) 
			{it -> if (it.getName() =~ tecnologias[entregable.ide].pattern)
				fichero = it.getCanonicalPath() 
			};
		if (fichero == null) {
			throw new Exception("No se ha encontrado ningún fichero correspondiente a la tecnología ${entregable.ide}")
		}
	}
	println "Compilando entregable en ${entregable.ruta}..."
	println "Ficheros a compilar: $fichero"
	println "Compilador a utilizar: ${tecnologias[entregable.ide].compiler}"
	println "Plataformas a compilar: "
	entregable.platforms.each { platform ->
		println platform.platId
	}
	def comando = new SimpleCompilerCommand()
	comando.initLogger { println it }
	def plataformasCompiladas = []
	println "********** PLATAFORMAS: " + entregable.platforms.size()
	entregable.platforms.each { platform ->
		println "*********** PLATAFORMA: " + platform
		// Compilar el fichero
		def modos = [ "Release" ]
		if (entregable.debug) {
			modos << "Debug"
		}
		println "*************MODOS: " + modos.size()
		println "**** MODOS: " + modos
		modos.each { modo ->
			//ficheros.each { fichero ->
				def cadenaFichero = MessageFormat.format(tecnologias[entregable.ide].file, fichero)
				def cadenaClean = null;
				// Por defecto, la identidad
				def closureFormato = { it }
				if (tecnologias[entregable.ide].targetFormat != null) {
					closureFormato = tecnologias[entregable.ide].targetFormat
				}
				if (tecnologias[entregable.ide].clean != null) {
					cadenaClean = MessageFormat.format(tecnologias[entregable.ide].clean, 
						closureFormato(modo), closureFormato(platform.platId), 
						closureFormato(entregable.ruta), obtenerNombre(fichero))
				}
				def cadenaTarget = MessageFormat.format(tecnologias[entregable.ide].target, 
					closureFormato(modo), closureFormato(platform.platId), 
					closureFormato(entregable.ruta), obtenerNombre(fichero)) 
				
				if (cadenaClean != null) {
					String salidaClean = comando.compilar(tecnologias[entregable.ide].compiler, 
						cadenaFichero, cadenaClean, directorioEntregable, tecnologias[entregable.ide].envp)
					println "Limpieza: " + salidaClean[0]
					File directorioPlataforma = new File(directorioEntregable.getCanonicalPath() +
						 System.getProperty("file.separator") + platform.platId)
					if (directorioPlataforma.exists() && directorioPlataforma.isDirectory()) {
						println "Limpiando directorio de salida ${directorioPlataforma.canonicalPath}..."
						directorioPlataforma.delete()
					}
				}
				String[] salida = comando.compilar(tecnologias[entregable.ide].compiler, cadenaFichero, cadenaTarget, directorioEntregable, tecnologias[entregable.ide].envp)
				println "Salida estándar: "
				println salida[0]
				println "Salida de error: "
				println salida[1]
				OutputParser parser = tecnologias[entregable.ide].parser
				if (!parser.validate(salida[0], salida[1])) {
					throw new Exception("ERROR DE COMPILACION -> " + entregable.ide)
				}
				if (entregable.type == 'lib') {
					println "Incorporando el entregable al entorno de compilación..."
					// Actualizar el entorno de compilación con los .h y los .lib generados 
					//	para cada plataforma
					actualizarEntornoCompilacion(workspace + System.getProperty("file.separator") + 
						entregable.ruta, stream, modo, platform.platId, entornoCompilacion, 
						confEntCompilacion)
					// Copiar el descriptor al entregable
					File destinoDescriptor = new File([tempDir.getCanonicalPath(), 
						(componente + ".xml")].join(System.getProperty("file.separator")));
					File descriptor = new File([workspace, componente + ".xml"].join(
						System.getProperty("file.separator")));
					destinoDescriptor.bytes = descriptor.bytes
				}
				if (entregable.type == 'lib' || entregable.type == 'libAux') {
					println "Acumulando el entregable..."
					acumularEntregableLib(tempDir, directorioEntregable.getCanonicalPath())
				}
			//}
		}
	}
	
	println "Actualizando el entorno de ejecución..."
	actualizarEntornoEjecucion(workspace, entornoEjecucion, entregable)		
}


// Por defecto, VS 6.0		
def DEFAULT_IDE = "vc60"
def DEFAULT_PLATFORM = "win32"

def TECNOLOGIAS = new HashMap<String, Map<String, Object>>()
//Compiladores indexados por IDE

// Tecnología:

// parser: objeto que parsea la salida del compilador, implementa OutputParser.  Sin parser, 
//	no se pueden detectar errores de compilación
// clean: cadena para hacer un clean del directorio de compilación (puede ser null)
// compiler: comando de compilación
// pattern: expresión regular para encontrar ficheros de compilación (vcprojs, makefiles, etc.)
// file: cadena para la inclusión del fichero de compilación en la línea de comandos
// target: cadena para construir los fuentes.  Parámetros: 0 - debug/release; 1 - plataforma; 2 - ruta relativa; 3 - nombre del fichero de construcción (sin ruta ni extensión)
// envp: variables de entorno para el lanzamiento de la compilación
// validadores: lista de validaciones previas a la compilación (puede ser null)
// targetFormat: closure que pasar a los parámetros del target para componer el format
TECNOLOGIAS.put("vc60", [
	"parser": new C_VS60_MSDEV_OutputParser(),
	"clean": null, "compiler":"msdev", 
	"pattern":/.+\.dsp$/, "file":" \"{0}\"", 
	"target":"/MAKE \"{3} - {1} {0}\" /REBUILD", 
	"envp":["TMP=${tmpVS60}", "lib=${libVS60}", "path=${pathVS60}", "include=${includeVS60}"], 
	"validadores":[], 
	"targetFormat": { String cadena -> cadena.capitalize() } ])
TECNOLOGIAS.put("EVC4", ["parser": new C_VS_OutputParser(), 
	"clean": "CFG=\"{0}|{1}\" clean", 
	"compiler":"nmake", "pattern":/.+\.vcn$/, 
	"file":"/f \"{0}\"", "target":"CFG=\"{0}|{1}\"", 
	"envp":["TMP=${tmpEVC4}", "path=${pathEVC4}", "lib=${libEVC4}", "include=${includeEVC4}"], 
	"validadores":[ /*new C_MakefileValidator()*/ ], 
	"targetFormat": null ])
TECNOLOGIAS.put("VS2005", ["parser": new C_VS2005_OutputParser(), 
	"clean": null, "compiler":"vcbuild", "pattern":/.+\.vcproj$/, "file":"\"{0}\"", 
	"target":"\"{0}|{1}\"", "envp":[], "validadores":[], "targetFormat": null ])

def ide = DEFAULT_IDE
def compilador = TECNOLOGIAS[DEFAULT_IDE].compiler
def plataformas = [ DEFAULT_PLATFORM ]
		
def listaBibliotecas = []
def listaEntregables = []
def listaCfgs = []

// Buscar el descriptor de componente
File descriptor = new File("${workspace}/${component}.xml")

//Aunque no exista unidad de compilación en el fichero, puede haber rutas
//	de entorno de compilación	
confEntCompilacion = C_VS_CompilationEnvironment.parsearEntornoCompilacion(descriptor)
if (confEntCompilacion != null) {
	println "Entorno de compilación definido para la biblioteca ->"
	println confEntCompilacion
}

if (descriptor != null && descriptor.exists() && existeUnidadCompilacion(descriptor)) {
	def componente = new C_VS_Component(descriptor)
	println componente
	
	listaBibliotecas = componente.bibliotecas
	listaEntregables = componente.entregables
	listaCfgs = componente.cfgs
	
}
else {
	// No hay fichero descriptor, tomando las opciones por defecto
	println "Tomando opciones por defecto"
		
	// Buscar los ficheros a compilar
	File rootDir = new File(workspace);
	def ficheros = [];
	rootDir.traverse(
	        type         : FileType.FILES,
	        preDir       : { if (it.name.startsWith('.')) return FileVisitResult.SKIP_SUBTREE })
	        {it -> if (it.getName() =~ TECNOLOGIAS[DEFAULT_IDE].pattern) 
	        	ficheros << it 
	        };
	ficheros.each { fichero ->
		C_VS_Deliverable entregable = new C_VS_Deliverable()
		entregable.type = 'app'
		println "*****FICHERO: " + fichero
		println "*****COMPONENT: " + component
		println "*****VERSION: " + version
		entregable.ruta = Utiles.rutaRelativa(rootDir, fichero)
		entregable.ide = DEFAULT_IDE
		entregable.groupId = groupId
		entregable.debug = false
		C_VS_Platform platform = new C_VS_Platform()
		platform.platId = DEFAULT_PLATFORM
		platform.platSubId = ''
		platform.rutatar = ''
		entregable.addPlatform(platform)
		entregable.version = version
		entregable.id = component		
		entregable.fichero = fichero
		listaEntregables << entregable
	}
}

// Mostrar qué se va acompilar
if (listaBibliotecas == null || listaBibliotecas.size() == 0) {
	println "No hay bibliotecas auxiliares a compilar"
}

if (listaEntregables == null || listaEntregables.size() == 0) {
	println "No hay entregables a compilar"
}

// Convertir las bibliotecas auxiliares en entregables tomando las opciones del primer 
//	entregable
def listaEntregablesBibliotecas = []
if (listaBibliotecas != null && listaBibliotecas.size() > 0
		&& listaEntregables != null && listaEntregables.size() > 0) {
	def entregableApp = listaEntregables.get(0)
	listaBibliotecas.each { biblioteca ->
		C_VS_Deliverable lib = new C_VS_Deliverable()
		lib.type = 'libAux'
		lib.debug = entregableApp.debug
		lib.ruta = biblioteca
		if (entregableApp.groupId != null && entregableApp.groupId.trim().length() > 0) {
			lib.groupId = entregableApp.groupId
		}
		else {
			lib.groupId = groupId
		}
		lib.ide = entregableApp.ide
		entregableApp.platforms.each { platform ->
			lib.addPlatform(platform)
		}
		
		listaEntregablesBibliotecas << lib
	}
}

// Unificar ambas listas
listaEntregablesBibliotecas.addAll(listaEntregables)
listaEntregables = listaEntregablesBibliotecas

// Sobre dir se va construyendo el entregable de aplicación (.exe, .dll y similares)
TmpDir.tmp { File dir ->
	def groupidEnt = null
	def versionEnt = version
	def ejecutable = false
	
	boolean erroresCompilacion = false;
	
	println "***********LISTA ENTREGABLES: " + listaEntregables.size()
	// Sobre tmpDir se va construyendo el entregable de biblioteca (.h, .lib y similares)
	TmpDir.tmp { File tmpDir ->
		listaEntregables.each { entregable ->
			entregable.version = version
			try {
				compilarEntregable(workspace, entornoEjecucion, TECNOLOGIAS, 
					entregable, stream, env['component'], entornoCompilacion, confEntCompilacion, tmpDir)
				println "ENTREGABLE TYPE: ${entregable.type}"
				groupidEnt = entregable.groupId
				versionEnt = entregable.version
				if (entregable.type == 'app') {
					ejecutable = true
				}
			}
			catch(Exception e) {
				erroresCompilacion = true;
				println (e.getMessage());
				println("Continuando después de un error...");
				e.printStackTrace();
			}	
		}	
	
		if (erroresCompilacion) {
			throw new Exception("Se han producido errores de compilación")
		}
	 
		// Actualizar los ficheros de configuración en el entorno de ejecución
		listaCfgs.each { cfg ->
			File fichConfig = new File([workspace, cfg.ruta].join(System.getProperty("file.separator")))
			cfg.platforms.each { platform ->
				String rutaPlataforma = platform.platId
				if (platform.platSubId != null && platform.platSubId.trim().length() > 0) {
					rutaPlataforma += ("-" + platform.platSubId)
				}
				File fichDestino = new File([entornoEjecucion, rutaPlataforma, 
					platform.rutatar, fichConfig.getName()].join(System.getProperty("file.separator")))
				if (!fichDestino.exists()) {
					new File(fichDestino.getParent()).mkdirs()
					fichDestino.createNewFile()
				}
				fichDestino.bytes = fichConfig.bytes
				if (ejecutable){
					File fichTemp = new File([dir, rutaPlataforma, platform.rutatar, 
						fichConfig.getName()].join(System.getProperty("file.separator")))
					if (!fichTemp.exists()) {
						new File(fichTemp.getParent()).mkdirs()
						fichTemp.createNewFile()
					}
					fichTemp.bytes = fichConfig.bytes
				}
			}
		}
		
		if (ejecutable){
			// Incluir el o los ejecutables en el entregable individual, sobre el directorio 'dir'
			listaEntregables.each { entregable ->				
				entregable.platforms.each { platform ->
					String rutaPlataforma = platform.platId
					if (platform.platSubId != null && platform.platSubId.trim().length() > 0) {
						rutaPlataforma += ("-" + platform.platSubId)
					}
					File dirEntregable = new File([workspace, entregable.ruta, rutaPlataforma, "release"].
						join(System.getProperty("file.separator")))
					println "Buscando ejecutables en $dirEntregable ..."
					File dirDestino = new File([dir, rutaPlataforma, platform.rutatar].
						join(System.getProperty("file.separator")))
					dirDestino.mkdirs()
					println "Ruta destino $dirDestino ..."
					println "Ruta entregable $dirEntregable ..."
					dirEntregable.traverse (
						type : FileType.FILES,
						maxDepth: 0) { fichero ->
						if (fichero.getName().toLowerCase() =~ /.+\.exe$/) {
							Utiles.copy(fichero, dirDestino.getCanonicalPath())
						}
					}
				}
			}
			// Subir a nexus el entregable individual del componente con los .exe, .dll, etc.	
			deployNexusEntregable(groupidEnt, env['component'], versionEnt, dir)
		}
		else {
			// Subir a nexus el entregable individual del componente con los .h, .lib, etc.
			deployNexusEntregable(groupidEnt, env['component'], versionEnt, tmpDir)
		}
	
	}		
}

// Si existe un directorio CommonHeaders, actualizar el entorno de compilación
File commonHeaders = new File(workspace, "CommonHeaders")
if (commonHeaders.exists() && commonHeaders.isDirectory()) {
	[ "Release", "Debug"].each { modo ->
		actualizarEntornoCompilacion(
			workspace + System.getProperty("file.separator") + "CommonHeaders", 
			stream, modo, null, entornoCompilacion, confEntCompilacion)
	}
	tempDir = File.createTempFile("nexus", "tmp")
	tempDir.delete()
	tempDir.mkdir()
	//acumularEntregableLib(tempDir, workspace + System.getProperty("file.separator") + "CommonHeaders")
	// En vez de pasar la ruta del directorio CommonHeaders pasamos el del padre para que suba a Nexus el xml con el entorno de compilacion
	acumularEntregableLib(tempDir, workspace)
	acumularXml(tempDir, workspace)
	deployNexusEntregable(groupId, env['component'], version, tempDir)
	tempDir.delete()
}