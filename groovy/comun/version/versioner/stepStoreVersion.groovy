package version.versioner

import hudson.model.AbstractBuild
import es.eci.utils.JobRootFinder
import es.eci.utils.ParamsHelper

/**
 * Se ejecuta siempre como System Groovy Script.
 * 
 * Este script busca en el parentWorkspace un fichero version.txt.  Caso de 
 * encontrarlo, recupera del mismo el valor de la versión y lo añade a una 
 * variable builtVersion en el job superior de componente si existe, o en el propio
 * job si no es posible.
 */
 
//---------------> Variables entrantes
def parentWorkspace = build.buildVariableResolver.resolve("parentWorkspace");
if (parentWorkspace == null || parentWorkspace.toString().trim().length() == 0) {
	// Tomar el workspace del job
	parentWorkspace = build.workspace.toString();
}

//-------------------> Lógica
File fileTxt = new File(new File(parentWorkspace.toString()), "version.txt");
fileTxt = fileTxt.getAbsoluteFile();
if (fileTxt.exists()) {
	def config = new ConfigSlurper().parse(fileTxt.text);
	def version = config.getProperty("version").toString();
	println("builtVersion <-- $version");
	// Buscar si es posible el componente
	List<AbstractBuild> fullTree = new JobRootFinder().getFullExecutionTree(build);
	AbstractBuild target = build;
	// Buscar el job de componente
	for (AbstractBuild ancestor: fullTree) {
		if (ancestor.getProject().getName().contains("-COMP")) {
			target = ancestor;
		}
	}
		
	// Si no es posible, añadir la versión al propio job
	// Añadir la versión al job de componente
	ParamsHelper.addParams(target, ["builtVersion": version]);
	
	// Se añade también al Controller para que esté disponible para todos los 
	// steps del Workflow.
	for (AbstractBuild ancestor: fullTree) {
		if (ancestor.getProject().getName().startsWith("Controller")) {
			target = ancestor;
		}
	}
	ParamsHelper.addParams(target, ["builtVersion": version]);
	// Añadido ante la sospecha de que cree un classloader leak
	target = null;
}
else {
	println("No existe en el master de Jenkins el fichero ${fileTxt}");
}
