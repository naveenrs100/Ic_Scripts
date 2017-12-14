
import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.versioner.PomXmlWriteOperations
import es.eci.utils.versioner.ArtifactsJsonUtils;

def params = [:];
File parentWorkspaceFile = new File("C:/OpenDevECI/WSECI_NEON/6BR-LIBS");
//def component = params["component"];
def component = "";
def action = "release";

def artifactsJson = ArtifactsJsonUtils.getArtifactsJson(params, component, parentWorkspaceFile, action);

PomXmlWriteOperations.removeSnapshot(parentWorkspaceFile, artifactsJson, params["nexusUrl"]);