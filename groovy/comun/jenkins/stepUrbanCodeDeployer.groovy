import urbanCode.Constants
import urbanCode.UrbanCodeApplicationProcess
import urbanCode.UrbanCodeExecutor
import es.eci.utils.CheckSnapshots

/**
 * Este script se invoca directamente, sin relación con ningún proceso de IC, 
 * para el despliegue de una instantánea (ficha de despliegue) de Urban Code
 * previamente existente.
 */

//---------------> Variables entrantes

def udClient = args[0]
def urlUrbanCode = args[1]
def user = args[2]
def password = args[3]
def application = args[4]
def snapshot = args[5]
String entorno = args[6] 

//---------------> Lógica
try{
	UrbanCodeExecutor exe = 
		new UrbanCodeExecutor(udClient, urlUrbanCode, user, password);
	exe.initLogger { println it }
	// ¿Existe la instantánea?
	CheckSnapshots chk = new CheckSnapshots();
	boolean existsUrbanSnapshot = chk.checkUrbanCodeSnapshots(exe, application, snapshot);
	if (!existsUrbanSnapshot) {
		// Avisar de que la instantánea no existe, y que se va a proceder a probar con
		//	la instantánea snapshot + "_completa"
		println "WARNING -> La instantánea $snapshot no existe en RTC.  Probando con ${snapshot}_completa ..."
		snapshot += "_completa"
		existsUrbanSnapshot = chk.checkUrbanCodeSnapshots(exe, application, snapshot);
		if (!existsUrbanSnapshot) {
			throw new Exception("ERROR -> La instantánea ${snapshot}_completa tampoco existe")
		}
	}
	// lanzamiento de la instantánea en el entorno indicado
	UrbanCodeApplicationProcess process = 
		new UrbanCodeApplicationProcess(
			application,
			Constants.DEPLOY_PROCESS, 
			entorno, 
			false, 
			snapshot);
	exe.requestApplicationProcess(process)
}
catch(Exception e){
	println "ERROR: ${e.getMessage()}"
	e.printStackTrace()
	throw e
}