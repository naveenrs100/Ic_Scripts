package git

@GrabResolver (name='nexus', root='http://nexus.elcorteingles.int/content/groups/public/')
@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.3.6')

import git.GitlabClient
import es.eci.utils.SystemPropertyBuilder

String urlGitlab = null;
String privateToken = null;

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
Map params = propertyBuilder.getSystemParameters();
urlGitlab = params.get("urlGitlab");
privateToken = params.get("privateToken");

GitlabClient client = new GitlabClient(urlGitlab, privateToken)
client.initLogger { println it }
println client.get("projects", null)