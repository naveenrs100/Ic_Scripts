import org.w3c.dom.Document;
import org.w3c.dom.Node;
import es.eci.utils.versioner.XmlUtils;
import es.eci.utils.versioner.XmlWriter;

File baseDir = new File("C:/Users/dcastro.jimenez/Desktop/Jobs a Modificar/JOBS_INT");

def mailContent = "managersMail=jmp_ams@iecisa.com,madorado@viewnext.com\n" +
		"MAIL_SUBJECT=\$stream - \$action"

baseDir.eachDir { File dir ->
	println("Comprobando ${dir.getName()}...");
	File configFile = new File(dir,"config.xml");
	Document doc = XmlUtils.parseXml(configFile);

	def cambiado = false;
	
	Node nodo = XmlUtils.xpathNode(doc, "/project/publishers/hudson.plugins.parameterizedtrigger.BuildTrigger/configs/hudson.plugins.parameterizedtrigger.BuildTriggerConfig/configs/hudson.plugins.parameterizedtrigger.PredefinedBuildParameters/properties");
	if(nodo != null) {
		def contenidoPrevio = nodo.getTextContent();
		if(!contenidoPrevio.contains("MAIL_SUBJECT") && !contenidoPrevio.contains("managersMail")) {
			nodo.setTextContent(contenidoPrevio + "\n" + mailContent);
			cambiado = true;
		}
	} else {
		println("\t...no tiene nodo properties");
	}


	Node nodoTriggers = XmlUtils.xpathNode(doc, "/project/publishers/hudson.plugins.parameterizedtrigger.BuildTrigger/configs/hudson.plugins.parameterizedtrigger.BuildTriggerConfig/projects");

	if(nodoTriggers != null) {
		def projectsPrevios = nodoTriggers.getTextContent();
		if(!projectsPrevios.contains("stepNotifierMail")) {
			nodoTriggers.setTextContent(projectsPrevios + ",stepNotifierMail");
			cambiado = true;
		}
	} else {
		println("\t...no tiene nodo projects");
	}

	if(cambiado) {
		XmlWriter.transformXml(doc, configFile);
	}
}