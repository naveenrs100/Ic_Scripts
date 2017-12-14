package sonar

import java.beans.XMLEncoder

import es.eci.utils.ParamsHelper
import es.eci.utils.StringUtil
import git.GitlabGroup
import groovy.json.JsonSlurper
import hudson.model.Hudson
import jenkins.model.Jenkins

/**
 * Este script se ejecuta como System Groovy Script.
 * 
 * El propósito de este script es recorrer los jobs de release (todos aquellos
 * que tengan la forma XXXXX - release en Jenkins) y que además tengan los parámetros
 * gitGroup y productId.  En estos jobs, tomará el grupo git y le asociará
 * el valor por defecto que tenga en el parámetro, en caso de que estén informados
 * ambos.  Si ambos lo están, los guardará en una estructura de datos tipo map.
 * 
 * La estructura de datos se persiste en un fichero git.xml en el workspace
 * del job, para que la aproveche después el script updateSonarUsers.groovy.
 * 
 */
println "================================================="
println "Inicio de gitProductIdReader..."

File workspace = new File(build.workspace.toString())

File groupsFile = new File(workspace, "gitlab_groups.json")
assert groupsFile.exists()

// Convierte la lista de grupos en un map indexado por nombre de grupo
Map processGitGroups(List<GitlabGroup> groups) {
	Map ret = [:]
	groups.each { def group ->
		ret[group.name] = group;
	}
	return ret;
}

Map actualGroups = [:]
def list = new JsonSlurper().parseText(groupsFile.text);
actualGroups = processGitGroups(list);

// Expresión regular para identificar los jobs de release
def pattern = /.+ - release/

Map<String, String> gitGroups = [:]


def jobNames = Jenkins.getInstance().getJobNames();
jobNames.each { String jobName ->
	if (jobName ==~ pattern) {
		def job = Hudson.getInstance().getJob(jobName);
		String gitGroup = ParamsHelper.getDefaultParameterValue(job, "gitGroup");
		String productId = ParamsHelper.getDefaultParameterValue(job, "productId");
		if (gitGroup != null && productId != null) {
			// Se trata de un job de release con grupo git y tiene una referencia a la
			//	project area en RTC
			Integer groupId = actualGroups[gitGroup]?.id;
			if (groupId != null) {
				gitGroups["${groupId} - gitlab"] = productId + "_" + groupId.toString();
			}
			else {
				println "WARNING: No se ha encontrado en gitlab el grupo $gitGroup"
			}
		}		
	}
}

// Persistir el map en un fichero
File gitFile = new File(workspace, "git.xml");
XMLEncoder e = new XMLEncoder(new FileOutputStream(gitFile))
e.writeObject(gitGroups);
e.close();

println "Fin de gitProductIdReader"
println "================================================="
