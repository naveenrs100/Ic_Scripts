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

def build = Thread.currentThread().executable
def resolver = build.buildVariableResolver

def version = resolver.resolve("version");

if (version == null || version.trim().length() == 0) {

	println "Calculando la versión..."
	
	// Parámetros de entrada
	def gitGroup = resolver.resolve("gitGroup");
	def branch = resolver.resolve("branch")
	def gitHost = build.getEnvironment(null).get("GIT_HOST"); 
	def gitCommand = build.getEnvironment(null).get("GIT_SH_COMMAND");
	def gitRepository = resolver.resolve("component")
	
	GitUtils utils = new GitUtils("git", gitHost, gitCommand)
	utils.initLogger { println it }
	
	version = utils.getRepositoryLastTag(gitGroup, gitRepository, branch);
	
	ParamsHelper.addParams(build, ["version": version]);

}
else {
	println "La versión viene informada -> ${version}"
}