package version.versioner

@Grab(group='com.ibm.icu', module='icu4j', version='57.1')

import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.versioner.PomXmlWriteOperations
import es.eci.utils.versioner.ArtifactsJsonUtils;

SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
def params = parameterBuilder.getSystemParameters();

def index = params["increaseIndex"].toInteger();
// Directorio actual
def parentWorkspaceFile = new File(".")
//def component = params["component"];
def component = "";
def action = params["action"];
def releaseMantenimiento = params["releaseMantenimiento"];

def artifactsJson = ArtifactsJsonUtils.getArtifactsJson(params, component, parentWorkspaceFile, action);

PomXmlWriteOperations.increaseVersion(parentWorkspaceFile, artifactsJson, params["nexusUrl"], action, releaseMantenimiento);