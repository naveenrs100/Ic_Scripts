import hudson.model.*
import urbanCode.*
import groovy.json.*

/**
 * Este script se invoca POR COMPONENTE para forzar a Urban Code a importar la 
 * versión recién creada en este workflow
 * Parámetros
 * udclient - Ruta del cliente udclient
 * urlUrbanCode - URL de udeploy
 * user - Usuario Urban Code
 * password - Password del usuario Urban Code
 * componenteUrbanCode - Nombre del componente Urban Code al que se va a
 * ordenar actualizarse
 */

//---------------> Variables entrantes
def udClient = args[0]
def urlUrbanCode = args[1]
def user = args[2]
def password = args[3]
def componenteUrbanCode = args[4]

//---------------> Lógica
try{
	UrbanCodeExecutor exe = new UrbanCodeExecutor(udClient, urlUrbanCode, user, password);
	exe.initLogger({ println it });
	// Snapshot acumulada hasta el momento
	UrbanCodeComponent component = new UrbanCodeComponent(componenteUrbanCode)
	// Crearla sobre Urban Code
	def json = exe.importVersions(component)
	println json
}catch(Exception e){
	println "ERROR: ${e.getMessage()}"
	e.printStackTrace()
	throw e
}
