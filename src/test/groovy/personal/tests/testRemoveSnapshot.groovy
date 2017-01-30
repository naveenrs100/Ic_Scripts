@GrabResolver(name='nexuseci', root='http://nexus.elcorteingles.int/content/groups/public/')
@Grab(group='com.ibm.icu', module='icu4j', version='57.1')

import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.versioner.PomXmlOperations
import es.eci.utils.versioner.ArtifactsJsonUtils;

SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
def params = ["aaa":"bbb"];

def parentWorkspaceFile = new File("C:/OpenDevECI/WSECI/QSP-Supermercado2016");
def component = "";
def action = "release";

def artifactsJson = ArtifactsJsonUtils.getArtifactsJson(params, component, parentWorkspaceFile, action);

PomXmlOperations.removeSnapshot(parentWorkspaceFile, artifactsJson);