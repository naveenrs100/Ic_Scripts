package jenkins

import hudson.model.Hudson
import urbanCode.Constants;
import urbanCode.UrbanCodeApplicationProcess
import urbanCode.UrbanCodeComponentInfoService
import urbanCode.UrbanCodeComponentVersion;
import urbanCode.UrbanCodeExecutor
import urbanCode.UrbanCodeSnapshot
import urbanCode.UrbanCodeSnapshotDeployer;
import buildtree.BuildBean
import buildtree.BuildTreeHelper
import es.eci.utils.NexusHelper
import es.eci.utils.ParamsHelper
import es.eci.utils.StringUtil
import es.eci.utils.pom.MavenCoordinates

/**
 * 
 * + Recupera la lista de jobs ejecutados en el deploy nocturno (solo los que
 * hayan registrado cambios).  Esto se encuentra en la variable jobs.
 * + Para cada job, recoge el nombre de componente Urban Code asociado, si lo 
 * hubiera.
 * + Resuelve contra Urban Code la información de cada componente (grupo y 
 * artefacto)
 * + Resuelve contra Nexus el timestamp exacto d.e la versión.  Para ello es 
 * necesario saber el último -SNAPSHOT generado.
 * + Compone una instantánea 'nightly' con las versiones a desplegar
 * + Elimina de Urban Code la instantánea 'nightly' de la aplicación si ya
 * existiese
 * + Crea la instantánea 'nightly' con la información recogida.
 * 
 * Variables de entrada:
 * 
 * jobs - Lista separada por comas de nombres de jobs de Jenkins
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
// Credenciales Nexus
String nexusUser =			build.buildVariableResolver.resolve("NEXUS_ADMIN_USER");
String nexusPass =			build.buildVariableResolver.resolve("NEXUS_ADMIN_PASS");
String stream	=			build.buildVariableResolver.resolve("stream");
String group	=			build.buildVariableResolver.resolve("gitGroup");

///////////////////////////////////////////////////////////////////////////////

//TODO: Agregar variables de gitGrouo y corriente para generar el nombre de la nightly

def isNull = { String s ->
	return s == null || s.trim().length() == 0;
}

if (!isNull(urbanCodeApp)) {
	// Árbol de construcción
	BuildTreeHelper helper = new BuildTreeHelper(200);
	helper.initLogger { println it };
	// Recorrido en profundidad
	List<BuildBean> buildTree = helper.executionTree(build);
	
	// Pares componentes/versión
	List<Map<String, String>> componentsVersions = []
	
	try {
		// Obtener la instancia de job en jenkins para cada ejecución de componente
		buildTree.each { BuildBean bean -> 
			if(bean.getName().contains("-COMP-")) {
				println "---> Recuperando la versión del job ${bean} ..."
				def componentJob = 
					Hudson.getInstance().getJob(bean.getName());  // Definición del job
				println "Job: $componentJob"
				def componentBuild = componentJob.
						getBuildByNumber(bean.getBuildNumber());  // Ejecución concreta
				println "Build: $componentBuild"
				String componentUrbanCode = 
					ParamsHelper.getDefaultParameterValue(
						componentJob, 
						"componenteUrbanCode");
				String componentDocumentation =
					ParamsHelper.getDefaultParameterValue(
						componentJob,
						"documentacion");
				if (componentUrbanCode != null && componentUrbanCode.trim().length() > 0) {	
					println "Recuperando desde $urlUrbanCode la información de $componentUrbanCode"
					// Obtener el parámetro de versión guardado en la ejecución del job	
					String builtVersion = ParamsHelper.getParam(componentBuild, 
						"builtVersion");
					println "---> Recuperada versión: $componentUrbanCode <-- $builtVersion";
					Map<String, String> tmp = [:];
					tmp.put(componentUrbanCode, builtVersion);					
					if(componentDocumentation != null && componentDocumentation.trim().equals("true")) {
						Map<String, String> tmpDoc = [:];
						tmpDoc.put(componentUrbanCode + ".doc", builtVersion);
						componentsVersions.add(tmpDoc);
					}
					componentsVersions.add(tmp);
				}
				else {
					println "Descartando ${bean} al no tener despliegue en Urban"
				}
			}
		} 
		if (componentsVersions != null && componentsVersions.size() > 0) {
			UrbanCodeExecutor exec = new UrbanCodeExecutor(urbanCodeCommand,
								urlUrbanCode,
								userUrbanCode,
								pwdUrbanCode);
			exec.initLogger { println it };
			// Lanzar el despliegue
			UrbanCodeSnapshotDeployer deploy = new UrbanCodeSnapshotDeployer(exec, urlNexus);
			
			// Informamos las credenciales de Nexus por si fueran necesarias para acceder a un repo privado.
			deploy.setNexus_user(nexusUser);
			deploy.setNexus_pass(nexusPass);
			
			deploy.initLogger { println it };
			// Nombre de la agrupación
			def groupName = null;
			if (!StringUtil.isNull(group)) {
				groupName = group;
			}
			else if (!StringUtil.isNull(stream)) {
				groupName = stream;
			}
			deploy.deploySnapshotVersions(
				componentsVersions, 
				urbanCodeApp, 
				urbanCodeEnv,
				"nightly_${StringUtil.normalize(groupName)}");
		}
		else {
			println "En la construcción no han entrado componentes con despliegue en Urban"
			println "Cancelando la creación de instantánea..."
		}
	}
	catch (Exception e) {
		println "---> ERROR INTENTANDO COMPONER LA INSTANTÁNEA NOCTURNA"
		println e.getMessage();
		e.printStackTrace()
		throw e
	}
}
else {
	println "No se intenta desplegar la nightly al faltar datos:"
	println " urbanCodeApp <- $urbanCodeApp"
}
