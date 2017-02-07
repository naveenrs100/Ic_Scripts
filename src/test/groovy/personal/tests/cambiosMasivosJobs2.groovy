import org.w3c.dom.Document;
import org.w3c.dom.Node;
import es.eci.utils.versioner.XmlUtils;

File backupDir = new File("C:/Users/dcastro.jimenez/Desktop/Jobs a Modificar/IMPORTANTISIMO BACKUP INT");
File dir = new File("C:/Users/dcastro.jimenez/Desktop/Jobs a Modificar/JOBS_INT");
File jobsTxt = new File("C:/Users/dcastro.jimenez/Desktop/Jobs a Modificar/jobs_a_cambiar.txt")
jobsTxt.text = "";

int contador = 0;
dir.eachFile { File componentDir ->
	File configFile = new File(componentDir, "config.xml");
	Document doc = XmlUtils.parseXml(configFile);
	boolean cambiado = false;
	
	// Se cambia el jdk a WAS85
	Node[] jdks = XmlUtils.xpathNodes(doc, "//jdk");
	jdks.each { Node thisJdkNode ->
		if(thisJdkNode.getTextContent().contains("(Default)") || thisJdkNode.getTextContent().contains("(System)")) {
			contador++
			println("\n${contador}: " + componentDir.getName())
			thisJdkNode.setTextContent("WAS85");	
			cambiado = true;
		}
	}
	
	// Se eliminan los pasos que lanzan por shell getComponents.sh
	Node[] getCompoShNodes = XmlUtils.xpathNodes(doc, "//hudson.tasks.Shell/command");
	getCompoShNodes.each { Node compoShNode ->
		if(compoShNode.getTextContent().contains("rtc/getComponents.sh")) {
			Node shellNode = compoShNode.parentNode
			shellNode.parentNode.removeChild(shellNode);
			cambiado = true;
		}
	}
	
	// Se pone el classpath correcto en setJobsFromStream.groovy
	Node[] scriptFileNodes = XmlUtils.xpathNodes(doc, "//hudson.plugins.groovy.SystemGroovy/scriptSource/scriptFile");
	scriptFileNodes.each { Node scriptFileNode ->
		if(scriptFileNode.getTextContent().contains("setJobsFromStream.groovy")) {
			XmlUtils.getChildNodes(scriptFileNode.parentNode.parentNode).each { Node childNode ->
				if(childNode.getNodeName().equals("classpath") && childNode.getTextContent().trim().equals("")) {
					childNode.setTextContent("/jenkins/jobs/ScriptsCore/workspace/groovy/classes");
					cambiado = true;
				}
			}
			
			// Comprobamos si el Trigger hay que cambiarlo a Trigger_groups
			Node[] triggerNodes = XmlUtils.xpathNodes(doc,"//hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig/projects");
			triggerNodes.each { Node triggerNode ->
				if(triggerNode.getTextContent().trim().equals("Trigger")) {
					triggerNode.setTextContent("Trigger_groups");
					cambiado = true;
				}
			}
		}
	}
	
	// Se elimina el validate reactor
	Node[] targetsNodes = XmlUtils.xpathNodes(doc, "//buildStep/targets");
	targetsNodes.each { Node targetsNode ->
		if(targetsNode.getTextContent().contains("validate -l reactor.log")) {
			Node buildStepNode = targetsNode.parentNode;
			buildStepNode.parentNode.removeChild(buildStepNode);
			cambiado = true;
		}
	}
	
	// Se elimina los setOrdered
	Node[] scriptFilesNode = XmlUtils.xpathNodes(doc, "//hudson.plugins.groovy.SystemGroovy/scriptSource/scriptFile");
	scriptFilesNode.each { Node scriptFileNode ->
		if(scriptFileNode.getTextContent().contains("setOrderedJobs.groovy")) {
			Node systemGroovyNode = scriptFileNode.parentNode.parentNode;
			systemGroovyNode.parentNode.removeChild(systemGroovyNode);
			cambiado = true;
		}
	}
	
	
	
	
	
	if(cambiado) {
		println("\n${contador}: " + componentDir.getName());
		XmlUtils.transformXml(doc, configFile);
		jobsTxt << "${componentDir.getName()}\n"
	}
}











