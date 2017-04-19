@GrabResolver(name='nexuseci', root='http://nexus.elcorteingles.int/content/groups/public/')
@Grab(group='com.ibm.icu', module='icu4j', version='57.1')

import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.versioner.PomXmlWriteOperations
import es.eci.utils.versioner.ArtifactsJsonUtils;

SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
def params = parameterBuilder.getSystemParameters();

def parentWorkspaceFile = new File(params["parentWorkspace"]);
//def component = params["component"];
def action = params["action"];
def component = ""

def artifactsJson = ArtifactsJsonUtils.getArtifactsJson(params, component, parentWorkspaceFile, action);

PomXmlWriteOperations.addSnapshot(parentWorkspaceFile,artifactsJson,params["nexusUrl"]);
