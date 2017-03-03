import es.eci.utils.GlobalVars
import es.eci.utils.JobRootFinder;
import hudson.model.*;
import es.eci.utils.clarive.ClariveConnection;
import es.eci.utils.clarive.ClariveParamsHelper;

def build = Thread.currentThread().executable;
def resolver = build.buildVariableResolver;

GlobalVars gVars = new GlobalVars(build);
def id_proceso = gVars.get("id_proceso");

ClariveConnection clConn = new ClariveConnection();
clConn.initLogger { println it };

def ECI_PROXY_URL = build.getEnvironment(null).get("ECI_PROXY_URL");
def ECI_PROXY_PORT = build.getEnvironment(null).get("ECI_PROXY_PORT");
def nakedUrl = build.getEnvironment(null).get("CLARIVE_URL");
def api_key_clarive = resolver.resolve("CLARIVE_API_KEY");
def nombre_componente = resolver.resolve("componentName");
def action = resolver.resolve("action");
def version = resolver.resolve("builtVersion");
if(version == null || version.trim().equals("")) {
	version = resolver.resolve("version");
}
def proceso = resolver.resolve("tipo_proceso").toUpperCase();
if(proceso.equals("ADDHOTFIX")) {
	proceso = "HOTFIX";
}

ClariveParamsHelper clHelper = new ClariveParamsHelper(build);
def nombre_producto = clHelper.getProjectArea();
def nombre_subproducto = clHelper.getSubproducto();
def tipo_corriente = clHelper.getTipoCorriente();
def resultado = clHelper.getResultado();
def metrica_PU = clHelper.getMetricas().get("metricaPU");
def metrica_PC = clHelper.getMetricas().get("metricaPC");

JobRootFinder jRootFinder = new JobRootFinder(build);
def rootBuild = jRootFinder.getRootBuild(build);
def rootResult = rootBuild.getResult();
println("Resultado del padre: " + rootResult);

if(rootResult != Result.NOT_BUILT && rootResult != Result.ABORTED) {
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
		
	// Si el job raíz es el propio job de componente se
	// llama de nuevo a SWC03 para notificar el fin de proceso.
	if(rootBuild.getFullDisplayName().contains("-COMP-")) {
		println("Como el job raíz es el propio de componente notificaremos también el final de proceso:");
		clConn.swc03(
			nakedUrl,
			api_key_clarive,
			ECI_PROXY_URL,
			ECI_PROXY_PORT,
			action,
			nombre_producto,
			nombre_subproducto,
			tipo_corriente,
			nombre_componente,
			version,
			id_proceso,
			resultado,
			metrica_PU,
			metrica_PC);
	}
	
} else {
	println("No se ha construido el job padre.");
}






