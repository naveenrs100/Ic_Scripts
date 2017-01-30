@GrabResolver(name='nexuseci', root='http://nexus.elcorteingles.int/content/groups/public/')
@Grab(group='com.ibm.icu', module='icu4j', version='57.1')

import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.versioner.PomXmlOperations;
import es.eci.utils.versioner.ArtifactsJsonUtils;
import es.eci.utils.pom.NodeProps;

SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
def params = parameterBuilder.getSystemParameters();

def parentWorkspaceFile = new File(params["parentWorkspace"]);

def notResolvedVariables = PomXmlOperations.checkVariables(parentWorkspaceFile);

if(notResolvedVariables.size() > 0) {
	def listaVariablesString = "";
	notResolvedVariables.each { NodeProps notResolvedVariable ->
		def variableName = notResolvedVariable.getNode().getTextContent();		
		def pomPath = notResolvedVariable.getPomFile().getCanonicalPath();
		def mensajeError = "No puede ser resuelta la variable \"${variableName}\" que está en \"${pomPath}\".";
		println(mensajeError);
		listaVariablesString = listaVariablesString + "\n\t\t${mensajeError}";
	}
	throw new NumberFormatException("Hay variables que no se han podido resolver. Corregidlas antes de volver a intentar otra construcción.${listaVariablesString}");
}