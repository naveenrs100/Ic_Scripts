package sonar

@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.4')

import es.eci.utils.SystemPropertyBuilder
import git.GitlabClient
import git.GitlabGroup
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

/**
 * Este script se invoca como Groovy Script y su propósito es crear una
 * estructura de datos con los grupos existentes en gitlab, en el fichero
 * gitlab_groups.xml, y que posteriormente lo utilice el script 
 * gitProductIdReader.groovy. 
 * 
 * Parámetros:
 * 
 * URL de gitlab (parámetro global de Jenkins)
 * Token privado de administración de Gitlab (parámetro global de Jenkins)
 * URL de Nexus, para recuperar los keystores
 * Versión del keystore de Gitlab
 */
println "================================================="
println "Inicio de gitGroupIdentifiers..."

File f = new File("gitlab_groups.json");
f.createNewFile();

SystemPropertyBuilder builder = new SystemPropertyBuilder();
Map<String, String> params = builder.getSystemParameters();

String gitlabURL		= params['gitlabURL']
String gitlabSecret 	= params['gitlabSecret']
String gitlabKeystore 	= params['gitlabKeystore']
String nexusURL			= params['nexusURL']

GitlabClient client = 
	new GitlabClient(gitlabURL, gitlabSecret, gitlabKeystore, nexusURL);
client.initLogger { println it }
	
List<GitlabGroup> ret = []
def json = client.get("groups", [:])
def groups = new JsonSlurper().parseText(json);
groups.each { 
	GitlabGroup group = new GitlabGroup();
	group.setId(it.id);
	group.setName(it.name);
	ret << group;
}

f.text = JsonOutput.toJson(ret)

println "Fin de gitGroupIdentifiers"
println "================================================="
