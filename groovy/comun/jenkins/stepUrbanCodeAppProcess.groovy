import urbanCode.UrbanCodeApplicationProcess;
import urbanCode.UrbanCodeExecutor;
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
 */

// --- Variables de sistema ---
def urbanCodeCommand = args[0]
def urlUrbanCode = args[1]
def userUrbanCode = args[2]
def pwdUrbanCode = args[3]

// --- Variables entrantes ---

def urbanCodeApp = args[4] // OBLIGATORIO
def urbanCodeEnv = args[5] // OBLIGATORIO
def version = args[6] // OBLIGATORIO

// --- Módulos ---
def isNull = { String s ->
	return s == null || s.trim().length() == 0;
}

// --- Lógica ---

if ( !isNull(urbanCodeApp) && !isNull(urbanCodeEnv) && !isNull(version) ) {
	
	UrbanCodeExecutor exec = new UrbanCodeExecutor(
		urbanCodeCommand,
		urlUrbanCode,
		userUrbanCode,
		pwdUrbanCode)
	
	exec.initLogger { println it }
	
	// Si la versión es igual a DESARROLLO, búscaremos la última snapshot en urban de desarrollo
	if ( version == "DESARROLLO") {
		version = "nightly"
	}
	
	UrbanCodeApplicationProcess process = new UrbanCodeApplicationProcess(
		urbanCodeApp,
		Constants.DEPLOY_PROCESS,
		urbanCodeEnv,
		true,
		version)
	
	// Lanzar el despliegue
	try {
		exec.requestApplicationProcess(process)
	} catch (Exception e) {
		println "ERROR - al ejecutar el despliegue en Urban"
		e.printStackTrace()
	}
	
} else
	throw new Exception("ERROR - urbanCodeApp, urbanCodeEnv y version son obligatorios, revisar configuracion del job")
