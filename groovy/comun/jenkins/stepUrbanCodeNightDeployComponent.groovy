package jenkins

import hudson.model.AbstractBuild
import urbanCode.UrbanCodeExecutor
import urbanCode.UrbanCodeSnapshotDeployer
import es.eci.utils.JobRootFinder
import es.eci.utils.ParameterValidator
import es.eci.utils.ParamsHelper

/**
 * 
 * Debe lanzarse dentro del workflow de deploy.
 * + Recupera el job ancestro superior del que se esté ejecutando (la raíz del 
 * árbol).
 * + En caso de que el ancestro sea un job de componente, concluimos que el 
 * despliegue se ha lanzado desde componente, y debemos comunicarnos con Urban.
 * En otro caso, el script termina aquí.
 * + Recupera los parámetros entornoUrbanCode, componenteUrbanCode, 
 * aplicacionUrbanCode y builtVersion del job de componente.  
 * Si faltara alguno, no puede continuar.
 * + Con ambos valores, compone una lista de versiones y componentes y la envía
 * a Urban Code como instantánea 'nightly' para su despliegue
 * 
 */

///////////////////////////////////////////////////////////////////////////////
// Contexto del script
// Nexus
String urlNexus = 			build.getEnvironment(null).get("ROOT_NEXUS_URL");
// UC
String urbanCodeCommand = 	build.getEnvironment(null).get("UDCLIENT_COMMAND");
String urlUrbanCode = 		build.getEnvironment(null).get("UDCLIENT_URL");
String userUrbanCode = 		build.getEnvironment(null).get("UDCLIENT_USER");
String pwdUrbanCode = 		build.buildVariableResolver.resolve("UDCLIENT_PASS");
// Aplicación y entorno para Urban Code
String urbanCodeApp =		build.getEnvironment(null).get("aplicacionUrbanCode");
String urbanCodeEnv =		build.getEnvironment(null).get("entornoUrbanCode");
String urbanCodeComp =		build.getEnvironment(null).get("componenteUrbanCode");
///////////////////////////////////////////////////////////////////////////////

//TODO: Agregar variables de gitGrouo y corriente para generar el nombre de la nightly

def isNotNull = { String s ->
	return s != null && s.trim().length() > 0;
}

JobRootFinder finder = new JobRootFinder();
finder.initLogger { println it }

AbstractBuild ancestor = finder.getRootBuild(build);

println "Identificado el ancestro: $ancestor"

if (ancestor.getProject().getName().contains("-COMP-")) {
	// Versión del componente
	def builtVersion = ParamsHelper.getParam(ancestor, "builtVersion");
	println "El job ha sido lanzado desde componente, preparando despliegue..."
	boolean validParameters = true;
	try {
	ParameterValidator.builder().
		add("urbanCodeApp", urbanCodeApp, isNotNull).
		add("urbanCodeEnv", urbanCodeEnv, isNotNull).
		add("urbanCodeComp", urbanCodeComp, isNotNull).
		add("builtVersion", builtVersion, isNotNull).
			build().validate();
	}
	catch (Exception e) {
		println e.getMessage();
		// No son válidos
		validParameters = false;
	}
	if (validParameters) {
		// Versiones
		List<Map<String, String>> componentsVersions = [];
		Map<String, String> tmp = [:];
		tmp.put(urbanCodeComp, builtVersion);
		componentsVersions << tmp;
		// Si los parámetros están todos definidos, lanzar el despliegue
		UrbanCodeExecutor exec = new UrbanCodeExecutor(urbanCodeCommand,
							urlUrbanCode,
							userUrbanCode,
							pwdUrbanCode);
		exec.initLogger { println it };
		UrbanCodeSnapshotDeployer deploy = new UrbanCodeSnapshotDeployer(exec, urlNexus);
		deploy.initLogger { println it };
		deploy.deploySnapshotVersions(
			componentsVersions,
			urbanCodeApp,
			urbanCodeEnv,
			"nightly_${urbanCodeComp}");
	}
	else {
		println "Falta algún parámetro necesario para desplegar en Urban, no se ejecuta el despliegue"
	}
}
else {
	println "El job no ha sido lanzado desde componente, no se lanza despliegue contra Urban Code"
}