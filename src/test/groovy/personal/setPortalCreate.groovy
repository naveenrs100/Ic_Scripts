import org.w3c.dom.Document;
import org.w3c.dom.Node;
import es.eci.utils.versioner.XmlUtils;

File rootDir = new File("C:/Users/dcastro.jimenez/Desktop/Jobs a Modificar/JOBS_INT");

rootDir.eachDir { File dir ->
	boolean cambiado = false;
	File configFile = new File(dir,"config.xml");	
	Document doc = XmlUtils.parseXml(configFile);	
	Node node = XmlUtils.xpathNode(doc, 
		"/project" +
		"/builders" +
		"/org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder" + 
		"/buildStep" +
		"/configs" +
		"/hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig" +
		"/projects");
		
	if(node != null && node.getTextContent().equals("stepNotifierPortal")) {
		node.setTextContent("stepNotifierPortalCreate");
		println("Intentando cambiar \"${dir.getName()}\"");
		cambiado = true;
	}
	
	if(cambiado) {
		XmlUtils.transformXml(doc, configFile);
	}	
}

