//@GrabResolver(name='nexuseci', root='http://nexus.elcorteingles.int/content/groups/public/')
//@Grab(group='com.ibm.icu', module='icu4j', version='57.1')

import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.versioner.PomXmlWriteOperations
import es.eci.utils.versioner.ArtifactsJsonUtils;

//SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
//def params = parameterBuilder.getSystemParameters();

// Directorio actual
//def parentWorkspaceFile = new File(".")
File parentWorkspaceFile = new File("C:/OpenDevECI/WSECI_NEON/6BR-LIBS");
//def component = params["component"];
def component = "";
//def action = params["action"];
def action = "release";
//def releaseMantenimiento = params["releaseMantenimiento"];
def releaseMantenimiento = null;
def params = [:];

def artifactsJson = ArtifactsJsonUtils.getArtifactsJson(params, component, parentWorkspaceFile, action);

PomXmlWriteOperations.increaseVersion(parentWorkspaceFile, artifactsJson, params["nexusUrl"], action, releaseMantenimiento);