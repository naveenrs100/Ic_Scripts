import org.w3c.dom.Document;
import org.w3c.dom.Node;
import es.eci.utils.versioner.XmlUtils;

File deleteFile = new File("C:/OpenDevECI/WSECI/DIC - Scripts/src/test/groovy/personal/delete.xml");

File log = new File("C:/Users/dcastro.jimenez/Desktop/Jobs a Modificar/deletes.txt");

Document deleteDoc = XmlUtils.parseXml(deleteFile);
Node deleteNode = XmlUtils.xpathNode(deleteDoc, "/hudson.plugins.ws__cleanup.PreBuildCleanup");

File dir = new File("C:/Users/dcastro.jimenez/Desktop/Jobs a Modificar/JOBS_INT");
//File dir = new File("C:/Users/dcastro.jimenez/Desktop/Jobs a Modificar/JOBS_PRE");

dir.eachDir { File componentDir ->
	boolean cambiado = false;
	File configFile = new File(componentDir,"config.xml");
	Document doc = XmlUtils.parseXml(configFile);
	
	Node buildWrapperNode = XmlUtils.xpathNode(doc, "/project/buildWrappers");
	
	if(buildWrapperNode != null) {
		Node thisDeleteNode = XmlUtils.getChildNode(buildWrapperNode, "hudson.plugins.ws__cleanup.PreBuildCleanup");
		if(thisDeleteNode == null) {
			Node newDeleteNode = doc.importNode(deleteNode, true);
			buildWrapperNode.appendChild(newDeleteNode);
			cambiado = true;
		} else {			
			log << "El componente \"${componentDir.getName()}\" ya tenía delete definido.\n"
		}	
	} else {	
		log << "El componente \"${componentDir.getName()}\" no tiene buildWrapper (y eso es raro que te cagas).\n"
	}
	
	if(cambiado) {
		XmlUtils.transformXml(doc, configFile);
	}
	
}