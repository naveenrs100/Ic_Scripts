import jenkins.model.*
import vs.*

import components.*

import es.eci.utils.ComponentVersionHelper
import es.eci.utils.ParamsHelper
import es.eci.utils.Utiles
import groovy.xml.*
import hudson.model.*


/**
 * Se ejecuta desde el job de la corriente (el principal).
 * Este script parsea el environment.xml para determinar la naturaleza de cada
 * componente y el workflow que le corresponde.  
 * 
 * SALIDA DEL SCRIPT: variable de entorno ${listaEntornoCompilacion} que alimenta
 * un job Controller para lanzar la actualización del entorno de compilación con 
 * los componentes de biblioteca con versión cerrada
 * 
 * SALIDA DEL SCRIPT: variable de entorno ${lista} que alimenta a un job Controller que,
 * a su vez, dispara la compilación de los componentes (bibliotecas y aplicaciones)
 * con versión abierta 
 * 
 * SALIDA DEL SCRIPT: variable de entorno ${version} que marca la versión del entregable
 * SALIDA DEL SCRIPT: variable de entorno ${groupId} que marca el groupId del entregable
 * SALIDA DEL SCRIPT: variable de entorno ${artifactId} que marca el artifactId del entregable
 */
def build = Thread.currentThread().executable

def action = build.getEnvironment().get("action")
println "action <- ${action}"
def workspaceRTC = build.getEnvironment().get("workspaceRTC")
def stream = build.getEnvironment().get("stream")
def workspace = build.getEnvironment().get("WORKSPACE")
def onlyChanges = build.getEnvironment().get("onlyChanges")
// En un paso anterior se ha recuperado este directorio
def environmentComponent = build.getEnvironment().get("environmentComponent")
def environmentComponentDir = new File(workspace + "/${environmentComponent}");
def environment = searchEnvironmentCatalog(environmentComponentDir);

def resolver = build.getBuildVariableResolver()
def urlRTC = build.getEnvironment().get("urlRTC")
def userRTC= build.getEnvironment().get("userRTC") 
def pwdRTC = resolver.resolve("pwdRTC") 

/**
 * Busca el elemento de biblioteca correspondiente al solicitado y lo cierra sobre el fichero
 * de environment.
 * @param xmlEnv Objeto XML con la información del entorno
 * @param groupId Coordenada groupId de la biblioteca a cerrar
 * @param artifactId Coordenada artifactId de la biblioteca a cerrar
 * @param version Versión de la biblioteca recuperada de RTC
 */
def cerrarVersionBiblioteca(xmlEnv, groupId, artifactId, version) {
	xmlEnv.libs.lib.each  { lib ->
		if (lib.groupId.text() == groupId && lib.artifactId.text() == artifactId) {
			lib.version[0].setValue(Utiles.versionCerrada(version))
		}
	}
}

/**
 * Vuelca el contenido del xml al fichero de environment
 * @param xmlEnv Objeto XML con la información del entorno
 * @param environment Fichero de entorno
 */
def updateEnvironment(xmlEnv, environment) {
	XmlUtil.serialize(xmlEnv, new FileWriter(environment))
}

/**
 * Busca en el directorio del componente de entorno el fichero que contiene el 
 * catálogo.  Crea las llamadas a los frontales para todos los componentes que encuentre
 * en la corriente.
 * @param dir Directorio base del componente de catálogo
 */
def File searchEnvironmentCatalog(File dir) {
	File ret = null;
	dir.traverse(
	        type         : groovy.io.FileType.FILES,
	        nameFilter   : ~/.*\.xml/,
	        postRoot     : true
	) {it -> if (ret==null && it.getName().endsWith("environment.xml")) { ret = it } }
	return ret;
}

def ficheroComponentes = new File(workspace + "/componentsCompare.txt");
ComponentsParser parser = new ComponentsParser()
parser.initLogger { println it }
// Parseo del fichero de componentes
List<RTCComponent> listaComps = parser.parse(ficheroComponentes)

Map<String, RTCComponent> tablaComps = new HashMap<String, RTCComponent>()

println 'Componentes en la corriente:'
listaComps.each { comp ->
	tablaComps.put(comp.nombre, comp)
	println '\t' + comp.nombre
}

def xmlEnv = new XmlParser().parse(environment)

List<C_VS_biblioteca> bibliotecas = new LinkedList<C_VS_biblioteca>()
List<C_VS_aplicacion> aplicaciones = new LinkedList<C_VS_aplicacion>()
List<String> lista = []
List<String> listaEntornoCompilacion = []		

	
// Para todos los componentes:

// Clasificarlo como aplicación o biblioteca y lanzar el componente
//	con la parametrización adecuada

// Recuperar las bibliotecas del *environment.xml
xmlEnv.libs.lib.each  { lib ->
	C_VS_biblioteca biblioteca = new C_VS_biblioteca();				 
	biblioteca.setGroupId(lib.groupId.text()) 
	biblioteca.setArtifactId(lib.artifactId.text()) 
	biblioteca.setVersion(lib.version.text()) 
	biblioteca.setIde(lib.ide.text())
	biblioteca.setDebug(Boolean.valueOf(lib.debug.text()))
	if (lib.platforms != null) {
		lib.platforms.platform.each { platform ->
			biblioteca.addPlatform(platform.text())
		}
	}
	bibliotecas << biblioteca
}

//Devolver el resultado del script
def params = [:]
		
//Recuperar valores del entorno de ejecución
def entorno = xmlEnv.entornoEjecucion
def groupId = ""
if (entorno != null) {
	println "Valorando el entorno de ejecución..."
	params.add(new StringParameterValue("groupId", entorno.groupId.text()))
	// Valor por defecto para el groupId
	groupId = entorno.groupId.text()
	println "groupId <- ${groupId}"
	//def version = entorno.version.text()
	// La versión nos la da la última línea base sobre el componente de catálogo
	def version = new ComponentVersionHelper().getVersion(environmentComponent, stream, userRTC, pwdRTC, urlRTC)
	if (action == 'release') {
		if (!version.endsWith("-SNAPSHOT")) {
			throw new Exception("La versión ${version} es incorrecta; debe terminar en -SNAPSHOT")
		}
		version = Utiles.versionCerrada(version)
		println "RELEASE: cambio de versión -> " + version
		// Genera el fichero de versión
		Utiles.creaVersionTxt(version, groupId, workspace); 
	}
	println "version <- ${version}"
	params.add(new StringParameterValue("version", version))
	def artifactId = entorno.artifactId.text()
	params.add(new StringParameterValue("artifactId", artifactId))
	println "artifactId <- ${artifactId}"
}

// Generar un workflow de cada tipo para cada uno según sea su tipo y versión
// Si está marcado onlyChanges y el componente no tiene cambios, no se programa su ejecución
def componente = '$stream -COMP-';
def pasoEntornoComilacion = 'stepUpdateVSlib'

listaComps.each { comp ->
	// Para cada componente
	if (bibliotecas.find { it.artifactId == comp.nombre } == null && !environmentComponent.equals(comp.nombre)) {
		// Aplicación
		C_VS_aplicacion app = new C_VS_aplicacion();
		app.setArtifactId(comp.nombre)
		aplicaciones << app
	}
	
}


def ComponentVersionHelper helper = new ComponentVersionHelper()
helper.initLogger {  }

println "Consultando tabla de componentes..."

bibliotecas.each { lib ->
	// Consultar la tabla de componentes que hemos parseado antes
	RTCComponent comp = tablaComps.get(lib.artifactId)
	if (lib.version == null || lib.version.trim().size() == 0) {
		println "Recuperando versión: ${lib.artifactId}, $stream, $userRTC, $pwdRTC, $urlRTC..."
		lib.version = helper.getVersion(lib.artifactId, stream, userRTC, pwdRTC, urlRTC)
	}
	if (helper.esAbierta(lib.version)) {
		if (comp != null) {
			// Construir con cambios
			if (onlyChanges == "true") {
				if (comp.cambios != null && comp.cambios.size() > 0) {
					def plataformas = lib.getPlatforms().join('::')
					lista.add("${componente}?groupId=${lib.groupId}&artifactId=${lib.artifactId}&version=${lib.version}&type=lib&workspaceRTC=${workspaceRTC}&stream=${stream}&action=${action}")
					comp.cambios.each { cambio ->
						println "--> CAMBIO: " + cambio.comentario
					}
					// Si es release, actualizar el fichero de environment
					if (action == 'release') {
						println "**** ENTRO, llamo a cerrarSesionBiblioteca"
						cerrarVersionBiblioteca(xmlEnv, lib.groupId, lib.artifactId, lib.version)
						updateEnvironment(xmlEnv, environment)
					}
				} else {
					println "--> EXCLUIDO componente ${lib.artifactId} -> no tiene cambios"
				}
			// Construir sin cambios
			} else {
			
				println "--> Construcción sin cambios, se incluye ${lib.artifactId}"
			
				def plataformas = lib.getPlatforms().join('::')
				lista.add("${componente}?groupId=${lib.groupId}&artifactId=${lib.artifactId}&version=${lib.version}&type=lib&workspaceRTC=${workspaceRTC}&stream=${stream}&action=${action}")
				
				// Si es release, actualizar el fichero de environment
				if (action == 'release') {
					println "**** ENTRO, llamo a cerrarSesionBiblioteca"
					cerrarVersionBiblioteca(xmlEnv, lib.groupId, lib.artifactId, lib.version)
					updateEnvironment(xmlEnv, environment)
				}
			}
		}
	} else {
		println "--> EXCLUIDO componente ${lib.artifactId}:${lib.version} -> no se encuentra en versión abierta"
		listaEntornoCompilacion << "${pasoEntornoComilacion}?parentWorkspace=${workspace}&groupId=${lib.groupId}&artifactId=${lib.artifactId}&version=${lib.version}"
	}
}

aplicaciones.each { app ->
	// Consultar la tabla de componentes que hemos parseado antes
	RTCComponent comp = tablaComps.get(app.artifactId)
	println "Recuperando versión: ${app.artifactId}, $stream, $userRTC, $pwdRTC, $urlRTC..."
	def version = helper.getVersion(app.artifactId, stream, userRTC, pwdRTC, urlRTC)
	if (helper.esAbierta(version)) {
		if (comp != null) {
			if (onlyChanges == "true") {
				if (comp.cambios != null && comp.cambios.size() > 0) {
					lista.add("${componente}?artifactId=${app.artifactId}&version=${version}&groupId=${groupId}&type=app&workspaceRTC=${workspaceRTC}&stream=${stream}&action=${action}")
					comp.cambios.each { cambio ->
						println "--> CAMBIO: " + cambio.comentario
					}
				} else {
					println "--> EXCLUIDO componente ${app.artifactId} -> no tiene cambios"
				}
			} else {
			
				println "--> Construcción sin cambios, se incluye ${app.artifactId}"
			
				lista.add("${componente}?artifactId=${app.artifactId}&version=${version}&groupId=${groupId}&type=app&workspaceRTC=${workspaceRTC}&stream=${stream}&action=${action}")
			}
		}
	} else {
		println "--> EXCLUIDO componente ${app.artifactId}:${version} -> no se encuentra en versión abierta"
	}
}

if (onlyChanges == "true" && (lista == null || lista.size() == 0)) {
	// Se debía construir solo si hay cambios, y no los hay
	// El build se marca NOT_BUILT
	println "CONSTRUCCIÓN CANCELADA debido a que no hay componentes que tengan ningún cambio"
	build.setResult(Result.NOT_BUILT)
}

def resultado = lista.join('\n')
def resultadoEntornoCompilacion = listaEntornoCompilacion.join('\n')


params.add (new StringParameterValue("lista", resultado))
params.add (new StringParameterValue("listaEntornoCompilacion", resultadoEntornoCompilacion))

ParamsHelper.addParams(build, params)
