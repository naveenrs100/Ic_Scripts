package jenkins

import es.eci.utils.StringUtil

/**
 * Este script genera un fichero de despligue de Urban que posteriormente utilizará
 * el job de SCHEDULE_URBAN que corresponda.
 * 
 * Parámetros
 * 
 * urbanCodeApp: Aplicación UrbanCode, definida en el job de PLAN.
 * urbanCodeEnv: Entorno UrbanCode, definida en el job de PLAN.
 * productId: Identificador de producto que sirve para que el job SCHEDULE_URBAN
 * 			  encuentre el fichero correcto.
 * version: Instantánea a planificar, o control por palabras clave para el job
 * 			de SCHEDULE_URBAN. El control puede ser DESARROLLO o RELEASE.
 * stream: Nombre de corriente para diferenciar nightlys.
 */

// --- Variables entrantes ---

def urbanCodeApp = args[0] // OBLIGATORIO
def urbanCodeEnv = args[1] // OBLIGATORIO
def productId = args[2] // OGLIGATORIO
def version = args[3]
def stream = args[4]

def timeDelay = null  // OPCIONAL
if (args.size() > 5) {
	timeDelay = args[5]
}

File ficheroUrban = null

// --- Lógica ---

if ( !StringUtil.isNull(productId) && !StringUtil.isNull(urbanCodeEnv) && !StringUtil.isNull(urbanCodeApp) ) {
	ficheroUrban = new File("${productId}_${urbanCodeEnv}_plan.txt")
	
	// Compobamos que hay version
	if ( !StringUtil.isNull(version) ) {
		// Update del fichero
		ficheroUrban.setText("");
		ficheroUrban.text = "environment=${urbanCodeEnv}\naplicacionUrbanCode=${urbanCodeApp}\nversion=${version}\nstream=${stream}"
		if (timeDelay != null) {
			ficheroUrban.text += "\ntimeDelay=${timeDelay}"
		}
	} else {
		println "AVISO - Version vacia borrando fichero..."
		ficheroUrban.delete();
	}
	
} else
	throw new Exception("ERROR - El productId, el entornoUrban y la aplicacionUrban son obligatorios, revisar configuracion del job")
