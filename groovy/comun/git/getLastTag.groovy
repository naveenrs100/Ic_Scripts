package git

import es.eci.utils.ParamsHelper;
import git.GitUtils;

/**
 * Script para ejecutarse como System Groovy Script en un job frontal (etapa 2).
 * Debe ser un job disparado por un push tag event desde git, que debe recuperar
 * el nombre de la última etiqueta sobre el repositorio indicado.
 * 
 * Necesita los parámetros
 * 
 * gitGroup
 * branch
 * gitRepository (normalmente 'component' en los jobs)
 * version -> Si viene inicializado, respetar su valor; en caso contrario,
 * 	calcularlo como la última tag del repositorio git
 * 
 * Escribe sobre el parámetro 
 * 
 * version
 */

def version = build.buildVariableResolver.resolve("version");
def builtVersion = build.buildVariableResolver.resolve("builtVersion");

if (version == null || version.trim().length() == 0) {

	println "Calculando la versión..."
	
	// Parámetros de entrada
	def gitGroup = build.buildVariableResolver.resolve("gitGroup");
	def branch = build.buildVariableResolver.resolve("branch")
	def gitHost = build.getEnvironment(null).get("GIT_HOST"); 
	def gitCommand = build.getEnvironment(null).get("GIT_SH_COMMAND");
	def gitRepository = build.buildVariableResolver.resolve("component")
	
	GitUtils utils = new GitUtils("git", gitHost, gitCommand)
	utils.initLogger { println it }
	
	version = utils.getRepositoryLastTag(gitGroup, gitRepository, branch);
	
	
	String[] myArray = new String[1];
	ParamsHelper.deleteParams(build, ["version"].toArray(myArray));
	
	Map<String,String> params = [:];
	params.put("version", version);
	if(builtVersion == null || builtVersion.trim().equals("")) {
		params.put("builtVersion", version);
	}
	
	ParamsHelper.addParams(build, params);

}
else {
	println "La versión viene informada -> ${version}"
}