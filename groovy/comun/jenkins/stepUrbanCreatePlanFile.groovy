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
 */

// --- Variables entrantes ---

def urbanCodeApp = args[0] // OBLIGATORIO
def urbanCodeEnv = args[1] // OBLIGATORIO
def productId = args[2] // OGLIGATORIO
def version = args[3]
def ficheroUrban = ""

// --- Módulos ---
def isNull = { String s ->
	return s == null || s.trim().length() == 0;
}

// --- Lógica ---

if ( !isNull(productId) && !isNull(urbanCodeEnv) && !isNull(urbanCodeApp) ) {
	ficheroUrban = new File("${productId}_${urbanCodeEnv}_plan.txt")
	
	// Compobamos que hay version
	if ( !isNull(version) ) {
		// Update del fichero
		ficheroUrban << "environment=${urbanCodeEnv}\naplicacionUrbanCode=${urbanCodeApp}\nversion=${version}"
	} else {
		println "AVISO - Version vacia borrando fichero..."
		ficheroUrban.delete();
	}
	
} else
	throw new Exception("ERROR - El productId, el entornoUrban y la aplicacionUrban son obligatorios, revisar configuracion del job")
