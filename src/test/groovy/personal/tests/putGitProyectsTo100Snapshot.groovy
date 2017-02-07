import es.eci.utils.versioner.PomXmlOperations;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import es.eci.utils.versioner.XmlUtils;

def version = "1.0.0.10-SNAPSHOT";

File prL1 = new File("C:/jenkins/workspace/test2/PruebaRelease-Biblioteca-1");
File prL2 = new File("C:/jenkins/workspace/test2/PruebaRelease-Biblioteca-2");
File prA1 = new File("C:/jenkins/workspace/test2/PruebaRelease-App-1");
File prA2 = new File("C:/jenkins/workspace/test2/PruebaRelease-App-2");

def dirs = [prL1,prL2,prA1,prA2]

dirs.each { File dir ->
	dir.eachFileRecurse { File file ->
		if(file.getName().equals("pom.xml")) {
			Document doc = XmlUtils.parseXml(file);
			Node[] versionNodes = XmlUtils.xpathNodes(doc, "//version");
			Node[] mainVersionNodes = XmlUtils.xpathNodes(doc, "//main-version");
			Node[] lib1VersionNodes = XmlUtils.xpathNodes(doc, "//lib1-version");
			Node[] lib2VersionNodes = XmlUtils.xpathNodes(doc, "//lib2-version");
			
			def nodes = [versionNodes,mainVersionNodes,lib1VersionNodes,lib2VersionNodes];
			
			nodes.each { Node[] thisNodes ->
				thisNodes.each { Node thisNode ->
					String txt = thisNode.getTextContent()
					if(!txt.equals("3.1") && !txt.equals("2.3") && !txt.equals("2.8") && !txt.equals("5") && !txt.equals("1.0.0") && !txt.equals("1.0") && !txt.equals("4.9")) {
						thisNode.setTextContent(version);
					}
				}
			}
			XmlUtils.transformXml(doc, file);
		}
	}
}
