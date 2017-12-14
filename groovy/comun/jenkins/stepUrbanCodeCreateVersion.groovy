package jenkins

import es.eci.utils.Utiles;
import hudson.model.*
import urbanCode.*
import groovy.json.*

/**
 * Este script se invoca POR COMPONENTE para forzar a Urban Code a crear la 
 * versión recién compilada en este workflow.  Da por hecho que contará con el 
 * fichero version.txt para leer la versión.
 * Parámetros
 * udclient - Ruta del cliente udclient
 * urlUrbanCode - URL de udeploy
 * user - Usuario Urban Code
 * password - Password del usuario Urban Code
 * componenteUrbanCode - Nombre del componente Urban Code al que se va a
 * ordenar actualizarse
 * Parámetros opcionales
 * version - Versión a crear
 */

// Si 'version' viene informado, recupera ahí la versión; en caso contrario,
//	la intenta leer del fichero version.txt
def getVersionValue(String version) {
	String ret = null;
	if (version != null && version.trim().length() > 0) {
		ret = version.trim();
	}
	else {
		// Intenta recuperar el version.txt
		File versionTxt = new File("version.txt")
		def jsonObject = new ConfigSlurper().parse(versionTxt.text)	
		ret = jsonObject.version
	}
	return ret;
}

//---------------> Variables entrantes
def udClient = args[0]
def urlUrbanCode = args[1]
def user = args[2]
def password = args[3]
def componenteUrbanCode = args[4]
def version = Utiles.readOptionalParameter(args, 5)


//---------------> Lógica
try{
	UrbanCodeExecutor exe = new UrbanCodeExecutor(udClient, urlUrbanCode, user, password);
	exe.initLogger({ println it });	
	def versionValue = getVersionValue(version);
	// Snapshot acumulada hasta el momento
	UrbanCodeComponentVersion componentVersion = 
		new UrbanCodeComponentVersion(componenteUrbanCode, versionValue, null, null)
	// Crearla sobre Urban Code
	def json = exe.createVersion(componentVersion)
	println json
}catch(Exception e){
	// Los errores en este comando son frecuentes, y se les hace caso omiso 
	println "WARNING: ${e.getMessage()}"
	println "Probablemente la versión existe ya en Urban Code"
}

