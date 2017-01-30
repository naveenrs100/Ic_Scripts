import es.eci.utils.GlobalVars
import hudson.model.*;
import es.eci.utils.clarive.ClariveConnection;

def build = Thread.currentThread().executable;
def resolver = build.buildVariableResolver;

GlobalVars gVars = new GlobalVars(build);
def id_proceso = gVars.get("id_proceso");
//def id_proceso = resolver.resolve("id_proceso");

ClariveConnection clConn = new ClariveConnection();
clConn.initLogger { println it };

def ECI_PROXY_URL = build.getEnvironment(null).get("ECI_PROXY_URL");
def ECI_PROXY_PORT = build.getEnvironment(null).get("ECI_PROXY_PORT");
def nakedUrl = build.getEnvironment(null).get("CLARIVE_URL");
def api_key_clarive = resolver.resolve("CLARIVE_API_KEY");

def nombre_producto = resolver.resolve("projectArea");
def nombre_subproducto = resolver.resolve("subproducto");
def tipo_corriente = resolver.resolve("tipo_corriente");
def nombre_componente = resolver.resolve("componentName");
def resultado = resolver.resolve("resultado");
def metrica_PU = resolver.resolve("metrica_PU"); 
def metrica_PC = resolver.resolve("metrica_PC");
def version = resolver.resolve("builtVersion");
if(version == null || version.trim().equals("")) {
	version = resolver.resolve("version");
}

def proceso = resolver.resolve("tipo_proceso").toUpperCase();
if(proceso.equals("ADDHOTFIX")) {
	proceso = "HOTFIX";
}

/**
 * Ejecución del servicio SWC03 de Clarive el cual 
 * informa del fin del proceso asociado al "id_proceso".
 */
clConn.swc03(
		nakedUrl,		
		api_key_clarive,
		ECI_PROXY_URL,
		ECI_PROXY_PORT,
		proceso,
		nombre_producto,
		nombre_subproducto,
		tipo_corriente,
		nombre_componente,
		version,
		id_proceso,
		resultado,
		metrica_PU,
		metrica_PC);
