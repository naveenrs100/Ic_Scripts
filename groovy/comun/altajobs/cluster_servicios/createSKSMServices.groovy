package altajobs.cluster_servicios
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import es.eci.utils.versioner.XmlUtils;
import es.eci.utils.versioner.XmlWriter;


def servicios = ["SKSM0017 - Credenciales B2C"];

File plantillasDir = new File("C:/OpenDevECI/WSECI_NEON/DIC - Scripts/groovy/comun/altajobs/cluster_servicios/Plantillas");


servicios.each { String servicio ->
	File buildTemplate = new File(plantillasDir,"STREAM - build/config.xml");
	File releaseTemplate = new File(plantillasDir,"STREAM - release/config.xml");
	File deployTemplate = new File(plantillasDir,"STREAM - deploy/config.xml");
	File addFixTemplate = new File(plantillasDir,"STREAM - addFix/config.xml");
	File addHotfixTemplate = new File(plantillasDir,"STREAM - addHotfix/config.xml");
	File compoTemplate = new File(plantillasDir,"STREAM -COMP- COMPO/config.xml");
	File compoPropTemplate = new File(plantillasDir,"STREAM -COMP- COMPO - Properties/config.xml");
	

	def STREAM_DESARROLLO = 	servicio + " - DESARROLLO";
	def STREAM_RELEASE = 		servicio + " - RELEASE";
	def STREAM_MANTENIMIENTO = 	servicio + " - MANTENIMIENTO";
	def COMPONENT_NAME = 		servicio;
	def WKS_MODIFICADA = 	STREAM_DESARROLLO.replaceAll(" - ","_").replaceAll(" -","_").replaceAll("- ", "_").replaceAll("-","_").replaceAll(" ","_").replaceAll("\\.","_");
	def COMPO_MODIFICADA = 	COMPONENT_NAME.replaceAll(" - ","_").replaceAll(" -","_").replaceAll("- ", "_").replaceAll("-","_").replaceAll(" ","_").replaceAll("\\.","_");

	// **************************  Build
	new File(plantillasDir,"Servicios/${servicio} - DESARROLLO - build").mkdirs();
	new File(plantillasDir,"Servicios/${servicio} - DESARROLLO - build/config.xml").bytes =  buildTemplate.bytes;
	File buildConfigXml = new File(plantillasDir,"Servicios/${servicio} - DESARROLLO - build/config.xml");
	modificacion(buildConfigXml,STREAM_DESARROLLO,STREAM_RELEASE,STREAM_MANTENIMIENTO,WKS_MODIFICADA,COMPO_MODIFICADA,COMPONENT_NAME);


	// **************************  Release
	new File(plantillasDir,"Servicios/${servicio} - DESARROLLO - release").mkdirs();
	new File(plantillasDir,"Servicios/${servicio} - DESARROLLO - release/config.xml").bytes =  releaseTemplate.bytes;
	File releaseConfigXml = new File(plantillasDir,"Servicios/${servicio} - DESARROLLO - release/config.xml");	
	modificacion(releaseConfigXml,STREAM_DESARROLLO,STREAM_RELEASE,STREAM_MANTENIMIENTO,WKS_MODIFICADA,COMPO_MODIFICADA,COMPONENT_NAME);

	// **************************  Deploy
	new File(plantillasDir,"Servicios/${servicio} - DESARROLLO - deploy").mkdirs();
	new File(plantillasDir,"Servicios/${servicio} - DESARROLLO - deploy/config.xml").bytes =  deployTemplate.bytes;
	File deployConfigXml = new File(plantillasDir,"Servicios/${servicio} - DESARROLLO - deploy/config.xml");	
	modificacion(deployConfigXml,STREAM_DESARROLLO,STREAM_RELEASE,STREAM_MANTENIMIENTO,WKS_MODIFICADA,COMPO_MODIFICADA,COMPONENT_NAME);
	
	// **************************  addFix
	new File(plantillasDir,"Servicios/${servicio} - DESARROLLO - addFix").mkdirs();
	new File(plantillasDir,"Servicios/${servicio} - DESARROLLO - addFix/config.xml").bytes =  addFixTemplate.bytes;
	File addFixConfigXml = new File(plantillasDir,"Servicios/${servicio} - DESARROLLO - addFix/config.xml");
	modificacion(addFixConfigXml,STREAM_DESARROLLO,STREAM_RELEASE,STREAM_MANTENIMIENTO,WKS_MODIFICADA,COMPO_MODIFICADA,COMPONENT_NAME);
	
	// **************************  addHotfix
	new File(plantillasDir,"Servicios/${servicio} - DESARROLLO - addHotfix").mkdirs();
	new File(plantillasDir,"Servicios/${servicio} - DESARROLLO - addHotfix/config.xml").bytes =  addHotfixTemplate.bytes;
	File addHotfixConfigXml = new File(plantillasDir,"Servicios/${servicio} - DESARROLLO - addHotfix/config.xml");
	modificacion(addHotfixConfigXml,STREAM_DESARROLLO,STREAM_RELEASE,STREAM_MANTENIMIENTO,WKS_MODIFICADA,COMPO_MODIFICADA,COMPONENT_NAME);
	
	// **************************  compo
	new File(plantillasDir,"Servicios/${servicio} - DESARROLLO -COMP- ${servicio}").mkdirs();
	new File(plantillasDir,"Servicios/${servicio} - DESARROLLO -COMP- ${servicio}/config.xml").bytes =  compoTemplate.bytes;
	File compoConfigXml = new File(plantillasDir,"Servicios/${servicio} - DESARROLLO -COMP- ${servicio}/config.xml");
	modificacion(compoConfigXml,STREAM_DESARROLLO,STREAM_RELEASE,STREAM_MANTENIMIENTO,WKS_MODIFICADA,COMPO_MODIFICADA,COMPONENT_NAME);
	
	// **************************  compo properties
	new File(plantillasDir,"Servicios/${servicio} - DESARROLLO -COMP- ${servicio} - Properties").mkdirs();
	new File(plantillasDir,"Servicios/${servicio} - DESARROLLO -COMP- ${servicio} - Properties/config.xml").bytes =  compoPropTemplate.bytes;
	File compoPropsConfigXml = new File(plantillasDir,"Servicios/${servicio} - DESARROLLO -COMP- ${servicio} - Properties/config.xml");
	modificacion(compoPropsConfigXml,STREAM_DESARROLLO,STREAM_RELEASE,STREAM_MANTENIMIENTO,WKS_MODIFICADA,COMPO_MODIFICADA,COMPONENT_NAME);
}



private void modificacion(thisConfigXml,STREAM_DESARROLLO,
		STREAM_RELEASE,STREAM_MANTENIMIENTO,WKS_MODIFICADA,COMPO_MODIFICADA,COMPONENT_NAME) {
	XmlUtils utils = new XmlUtils();
	Document buildDoc = utils.parseXml(thisConfigXml);
	
	Node [] descriptionNodes = utils.xpathNodes(buildDoc, "//description")
	descriptionNodes.each { Node descriptionNode ->
		descriptionNode.setTextContent("");
	}

	Node[] defaultValueNodes = utils.xpathNodes(buildDoc, "//defaultValue");

	defaultValueNodes.each { Node node ->
		if(node.getTextContent().equals("STREAM_DESARROLLO")) {
			node.setTextContent(STREAM_DESARROLLO);
		}
		if(node.getTextContent().equals("STREAM_RELEASE")) {
			node.setTextContent(STREAM_RELEASE);
		}
		if(node.getTextContent().equals("STREAM_MANTENIMIENTO")) {
			node.setTextContent(STREAM_MANTENIMIENTO);
		}
		if(node.getTextContent().contains("COMPONENT_NAME")) {
			def newStr = node.getTextContent().replaceAll("COMPONENT_NAME",COMPONENT_NAME);
			node.setTextContent(newStr);			
		}
	}

	Node wksNode = utils.xpathNode(buildDoc, "/project/customWorkspace");
	String newWks = wksNode.getTextContent().replaceAll("WKS_MODIFICADA", WKS_MODIFICADA).
		replaceAll("COMPO_MODIFICADA", COMPO_MODIFICADA);
	wksNode.setTextContent(newWks);

	XmlWriter.transformXml(buildDoc, thisConfigXml);
}

