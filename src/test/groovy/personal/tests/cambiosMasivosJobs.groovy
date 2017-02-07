import org.w3c.dom.Document;
import org.w3c.dom.Node;
import es.eci.utils.versioner.XmlUtils;

File dir = new File("C:/Users/dcastro.jimenez/Desktop/Jobs a Modificar/JOBS_INT");
File jobsTxt = new File("C:/Users/dcastro.jimenez/Desktop/Jobs a Modificar/jobs.txt")
jobsTxt.text = "";

dir.eachFile { File componentDir ->
	File configFile = new File(componentDir, "config.xml");

	Document doc = XmlUtils.parseXml(configFile);
	Node[] nodesScripts = XmlUtils.xpathNodes(doc, "//hudson.plugins.groovy.SystemGroovy/scriptSource/scriptFile")
	nodesScripts.each { Node thisScriptNode ->
		if(thisScriptNode != null && thisScriptNode.getTextContent().contains("setJobsFromStreamLight.groovy")) {
			Node[] nodesTrigger = XmlUtils.xpathNodes(doc, "//hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig/projects")
			nodesTrigger.each { Node thisTriggerNode ->
				if(thisTriggerNode != null && (thisTriggerNode.getTextContent().equals("Trigger"))) {
					Node[] nodesGetCompo = XmlUtils.xpathNodes(doc, "//hudson.plugins.groovy.Groovy/scriptSource/scriptFile")
					nodesGetCompo.each { Node thisGetCompoNode ->
						if(thisGetCompoNode != null && thisGetCompoNode.getTextContent().contains("getComponents.groovy")) {
							println("Transformando: ${componentDir.getName()}")
//							jobsTxt << "${componentDir.getName()}\n"
//							String newScriptValue = thisScriptNode.getTextContent().replace("setJobsFromStreamLight.groovy", "setJobsFromStream.groovy");
//							thisScriptNode.setTextContent(newScriptValue);
//							thisTriggerNode.setTextContent("Trigger_groups");
//							Node groovyScriptNode = thisGetCompoNode.parentNode.parentNode;
//							groovyScriptNode.parentNode.removeChild(groovyScriptNode);
//							XmlUtils.transformXml(doc, configFile);
						}
					}
				}
			}
		}
	}		
}