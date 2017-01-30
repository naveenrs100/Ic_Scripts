import hudson.model.Hudson

import java.beans.XMLEncoder

import jenkins.model.Jenkins
import es.eci.utils.ParamsHelper

/**
 * Este script se ejecuta como System Groovy Script.
 * 
 * El propósito de este script es recorrer los jobs de release (todos aquellos
 * que tengan la forma XXXXX - release en Jenkins) y que además tengan los parámetros
 * gitGroup y projectAreaUUID.  En estos jobs, tomará el grupo git y le asociará
 * el valor por defecto que tenga en el parámetro, en caso de que estén informados
 * ambos.  Si ambos lo están, los guardará en una estructura de datos tipo map.
 * 
 * La estructura de datos se persiste en un fichero git.xml en el workspace
 * del job, para que la aproveche después el script updateSonarUsers.groovy.
 * 
 */

def build = Thread.currentThread().executable;
def resolver = build.buildVariableResolver;

File workspace = new File(build.workspace.toString())

// Expresión regular para identificar los jobs de release
def pattern = /.+ - release/

Map<String, String> gitGroups = [:]

def jobNames = Jenkins.getInstance().getJobNames();
jobNames.each { String jobName ->
	if (jobName ==~ pattern) {
		def job = Hudson.getInstance().getJob(jobName);
		String gitGroup = ParamsHelper.getDefaultParameterValue(job, "gitGroup");
		String projectAreaUUID = ParamsHelper.getDefaultParameterValue(job, "projectAreaUUID");
		if (gitGroup != null && projectAreaUUID != null) {
			// Se trata de un job de release con grupo git y tiene una referencia a la
			//	project area en RTC
			gitGroups[gitGroup] = projectAreaUUID;
		}		
	}
}

// Persistir el map en un fichero
File gitFile = new File(workspace, "git.xml");
XMLEncoder e = new XMLEncoder(new FileOutputStream(gitFile))
e.writeObject(gitGroups);
e.close();
