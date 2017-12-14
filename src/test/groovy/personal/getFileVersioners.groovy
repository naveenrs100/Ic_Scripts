import es.eci.utils.versioner.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

File dir = new File("C:/Users/dcastro.jimenez/Desktop/Jobs a Modificar/JOBS_INT");
//File dir = new File("C:/Users/dcastro.jimenez/Desktop/Jobs a Modificar/TEST");

ArrayList<String> jobsVersionerList = [
	"stepFileVersioner",
	"stepFileVersionerCC",
	"stepFileVersionerSH",
	"stepFileVersionerPGP",
	"stepFileVersionerBroker",
	"stepFileVersionerGradle",
	"stepFileVersionerWeblogic",
	"stepFileVersionerGradleJDK7",
	"stepFileVersionerGradleRUJDK7"
]

ArrayList<File> filesReferencedList = new ArrayList<File>();

dir.eachFileRecurse { File file ->
	if(file.getName().equals("config.xml")) {
		Boolean fileReference = false;
		XmlUtils xu = new XmlUtils();
		Document doc = xu.parseXml(file);
		def paramNode = xu.xpathNode(doc, "/project/properties/hudson.model.ParametersDefinitionProperty/parameterDefinitions");
		if(paramNode != null) {
			def paramNodes = xu.getChildNodes(paramNode);
			paramNodes.each { Node node ->
				Node listaNode = xu.getChildNode(node, "name");
				if(listaNode != null && listaNode.getTextContent().equals("lista")) {
					String listaNodeText = xu.getChildNode(node, "defaultValue").getTextContent();
					for(String stepJob : jobsVersionerList) {
						if(listaNodeText.contains(stepJob)) {
							println(file.getCanonicalPath());
							filesReferencedList.add(file);
						}
					}
				}
			}
		}
	}
}

