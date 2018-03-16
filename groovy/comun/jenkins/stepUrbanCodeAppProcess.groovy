package jenkins

import urbanCode.UrbanCodeApplicationProcess;
import urbanCode.UrbanCodeExecutor;
import es.eci.utils.StringUtil
import es.eci.utils.SystemPropertyBuilder
import urbanCode.Constants;

/**
 * Este script ejecuta un despligue de Urban invocado por el job de
 * SCHEDULE_URBAN que corresponda.
 * 
 * Parámetros
 * 
 * urbanCodeApp: Aplicación UrbanCode, definida en el job de PLAN.
 * urbanCodeEnv: Entorno UrbanCode, definida en el job de PLAN.
 * version: Instantánea a planificar, o control por palabras clave para el job
 * 			de SCHEDULE_URBAN. El control puede ser DESARROLLO o RELEASE.
 * stream: Nombre de corriente para diferenciar nightlys.
 */

//def urbanCodeCommand = args[0]
//def urlUrbanCode = args[1]
//def userUrbanCode = args[2]
//def pwdUrbanCode = args[3]
//def urbanCodeApp = args[4]
//def urbanCodeEnv = args[5]
//def version = args[6]
//def stream = args[7]

// Variables de entrada
SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
def params = parameterBuilder.getSystemParameters();

def urbanCodeCommand = params["urbanCodeCommand"]
def urlUrbanCode = params["urlUrbanCode"]
def userUrbanCode = params["userUrbanCode"]
def pwdUrbanCode = params["pwdUrbanCode"]
def urbanCodeApp = params["urbanCodeApp"]
def urbanCodeEnv = params["urbanCodeEnv"]
def version = params["version"]
def stream = params["stream"]

def isNull = { String s ->
	return s == null || s.trim().length() == 0;
}

// Obtención de la última release en UrbanCode.
private Object getLastSnapshot(UrbanCodeExecutor executor, String urbanCodeApp, String version) {
	def lista = []

	def jsonFile = executor.getSnapshotsInApplication(urbanCodeApp)

	jsonFile.each {
		if(version.equals("RELEASE")) {
			if (it.description.equals("release") || it.description.equals("addFix")) {
				lista.add(it)
			}
		}
		if(version.equals("MANTENIMIENTO")) {
			if (it.description.equals("addHotfix")) {
				lista.add(it)
			}
		}
	}

	Collections.sort(lista, new Comparator() {
				public int compare(a, b) {
					return new Date(b.created).compareTo(new Date(a.created))
				}
			})

	if (lista.size > 0) {
		return lista[0]
	} else {
		return null
	}
}

// --- Lógica ---

// Para comprobar si es una snapshot o no.
boolean isThereOpenVersion = false;

if ( !isNull(urbanCodeApp) && !isNull(urbanCodeEnv) && !isNull(version) ) {

	UrbanCodeExecutor exec = new UrbanCodeExecutor(
			urbanCodeCommand,
			urlUrbanCode,
			userUrbanCode,
			pwdUrbanCode)

	exec.initLogger { println it }

	/*
	 *  Si la versión es igual a DESARROLLO, buscaremos la última snapshot en urban de desarrollo.
	 *  Si la versión es igual a RELEASE, buscaremos la última versión cerrada.
	 */
	if ( version == "DESARROLLO") {
		if (StringUtil.isNull(stream)) {
			version = "nightly"
		} else {
			version = "nightly_${StringUtil.normalize(stream)}"
		}

		isThereOpenVersion = true;
	} else if (version == "RELEASE" || version == "MANTENIMIENTO") {
		UrbanCodeExecutor executor = new UrbanCodeExecutor(
				urbanCodeCommand,
				urlUrbanCode,
				userUrbanCode,
				pwdUrbanCode)
		executor.initLogger{println it}
		def snapshot = getLastSnapshot(executor, urbanCodeApp, version);
		version = snapshot.get('name');
	}

	// Valores antes de llamar al proceso de UrbanCode.
	println "Aplicación: ${urbanCodeApp}"
	println "Entorno: ${urbanCodeEnv}"
	println "Versión: ${version}"
	println "Es versión abierta: ${isThereOpenVersion}"

	UrbanCodeApplicationProcess process = new UrbanCodeApplicationProcess(
			urbanCodeApp,
			Constants.DEPLOY_PROCESS,
			urbanCodeEnv,
			false,
			version,
			isThereOpenVersion?["ETIQUETA":"-SNAPSHOT"]:[:]);

	// Lanzar el despliegue
	try {
		exec.requestApplicationProcess(process)
	} catch (Exception e) {
		println 'ERROR - al ejecutar el despliegue en Urban'
		e.printStackTrace()
		throw new Exception('ERROR - al ejecutar el despliegue en Urban')
	}

} else {
	println "Aplicación: ${urbanCodeApp}"
	println "Entorno: ${urbanCodeEnv}"
	println "Versión: ${version}"
	throw new Exception("ERROR - urbanCodeApp, urbanCodeEnv y version son obligatorios, revisar configuracion del job")
}