import es.eci.utils.versioner.XmlUtils;
import org.w3c.dom.Document



File configDir = new File("C:/Users/dcastro.jimenez/Desktop/Jobs a Modificar/JOBS_PRE");

configDir.eachDir { File dir ->
	File configFile = new File(dir,"config.xml");
	
	Document doc = XmlUtils.parseXml(configDir);
	
	XmlUtils.xpathNode(doc, "/project/");
		
}



