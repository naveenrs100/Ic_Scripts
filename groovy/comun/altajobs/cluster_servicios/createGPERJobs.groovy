package altajobs.cluster_servicios
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import es.eci.utils.versioner.XmlUtils;
import es.eci.utils.versioner.XmlWriter;


def streamMap = ["DVD - GPER - CAPTURA":"2ND - GPER CAPTURA",
				"DVD - GPER - CAPTURA - PROPERTIES":"2ND - GPER CAPTURA - PROPERTIES",
				"DVD - GPER - COMUNES":"2ND - GPER COM",
				"DVD - GPER - ADMINISTRADOR":"2ND - GPER ADMINISTRADOR",
				"DVD - GPER - ENVIOS":"2ND - GPER ENVIOS",
				"DVD - GPER - ENVIOS - PROPERTIES":"2ND - GPER ENVIOS - PROPERTIES",
				"DVD - GPER - FACTURACION":"2ND - GPER FACTURACION",
				"DVD - GPER - FACTURACION - PROPERTIES":"2ND - GPER FACTURACION - PROPERTIES",
				"DVD - GPER - INTEGRACION":"2ND - GPER INTEGRACION",
				"DVD - GPER - INTEGRACION - PROPERTIES":"2ND - GPER INTEGRACION - PROPERTIES",
				"DVD - GPER - SERVICIOS":"2ND - GPER SERVICIOS",
				"DVD - GPER - SERVICIOS - PROPERTIES":"2ND - GPER SERVICIOS - PROPERTIES",
				"DVD - GPER - ADMINISTRADOR - PROPERTIES":"2ND - GPER ADMINISTRADOR - PROPERTIES",
				"DVD - GPER - BATCH":"2ND - GPER BATCH"
				];

File plantillasDir = new File("C:/OpenDevECI/WSECI_NEON/DIC - Scripts/groovy/comun/altajobs/gper/plantillas");


streamMap.keySet().each { String stream ->
	File buildTemplate = new File(plantillasDir,"STREAM - build/config.xml");
	File releaseTemplate = new File(plantillasDir,"STREAM - release/config.xml");
	File deployTemplate = new File(plantillasDir,"STREAM - deploy/config.xml");
	File addFixTemplate = new File(plantillasDir,"STREAM - addFix/config.xml");
	File addHotfixTemplate = new File(plantillasDir,"STREAM - addHotfix/config.xml");
	File compoTemplate = new File(plantillasDir,"STREAM -COMP- COMPONENTE/config.xml");	
	

	def STREAM_DESARROLLO = 	stream + " - DESARROLLO";
	def STREAM_RELEASE = 		stream + " - RELEASE";
	def STREAM_MANTENIMIENTO = 	stream + " - MANTENIMIENTO";
	def COMPONENT_NAME = 		streamMap.get(stream);
	def WKS_MODIFICADA = 	STREAM_DESARROLLO.replaceAll(" - ","_").replaceAll(" -","_").replaceAll("- ", "_").replaceAll("-","_").replaceAll(" ","_").replaceAll("\\.","_");
	def COMPO_MODIFICADA = 	COMPONENT_NAME.replaceAll(" - ","_").replaceAll(" -","_").replaceAll("- ", "_").replaceAll("-","_").replaceAll(" ","_").replaceAll("\\.","_");

	// **************************  Build
	new File(plantillasDir,"Servicios/${stream} - DESARROLLO - build").mkdirs();
	new File(plantillasDir,"Servicios/${stream} - DESARROLLO - build/config.xml").bytes =  buildTemplate.bytes;
	File buildConfigXml = new File(plantillasDir,"Servicios/${stream} - DESARROLLO - build/config.xml");
	modificacion(buildConfigXml,STREAM_DESARROLLO,STREAM_RELEASE,STREAM_MANTENIMIENTO,WKS_MODIFICADA,COMPO_MODIFICADA,COMPONENT_NAME);


	// **************************  Release
	new File(plantillasDir,"Servicios/${stream} - DESARROLLO - release").mkdirs();
	new File(plantillasDir,"Servicios/${stream} - DESARROLLO - release/config.xml").bytes =  releaseTemplate.bytes;
	File releaseConfigXml = new File(plantillasDir,"Servicios/${stream} - DESARROLLO - release/config.xml");	
	modificacion(releaseConfigXml,STREAM_DESARROLLO,STREAM_RELEASE,STREAM_MANTENIMIENTO,WKS_MODIFICADA,COMPO_MODIFICADA,COMPONENT_NAME);

	// **************************  Deploy
	new File(plantillasDir,"Servicios/${stream} - DESARROLLO - deploy").mkdirs();
	new File(plantillasDir,"Servicios/${stream} - DESARROLLO - deploy/config.xml").bytes =  deployTemplate.bytes;
	File deployConfigXml = new File(plantillasDir,"Servicios/${stream} - DESARROLLO - deploy/config.xml");	
	modificacion(deployConfigXml,STREAM_DESARROLLO,STREAM_RELEASE,STREAM_MANTENIMIENTO,WKS_MODIFICADA,COMPO_MODIFICADA,COMPONENT_NAME);
	
	// **************************  addFix
	new File(plantillasDir,"Servicios/${stream} - DESARROLLO - addFix").mkdirs();
	new File(plantillasDir,"Servicios/${stream} - DESARROLLO - addFix/config.xml").bytes =  addFixTemplate.bytes;
	File addFixConfigXml = new File(plantillasDir,"Servicios/${stream} - DESARROLLO - addFix/config.xml");
	modificacion(addFixConfigXml,STREAM_DESARROLLO,STREAM_RELEASE,STREAM_MANTENIMIENTO,WKS_MODIFICADA,COMPO_MODIFICADA,COMPONENT_NAME);
	
	// **************************  addHotfix
	new File(plantillasDir,"Servicios/${stream} - DESARROLLO - addHotfix").mkdirs();
	new File(plantillasDir,"Servicios/${stream} - DESARROLLO - addHotfix/config.xml").bytes =  addHotfixTemplate.bytes;
	File addHotfixConfigXml = new File(plantillasDir,"Servicios/${stream} - DESARROLLO - addHotfix/config.xml");
	modificacion(addHotfixConfigXml,STREAM_DESARROLLO,STREAM_RELEASE,STREAM_MANTENIMIENTO,WKS_MODIFICADA,COMPO_MODIFICADA,COMPONENT_NAME);
	
	// **************************  compo
	new File(plantillasDir,"Servicios/${stream} - DESARROLLO -COMP- ${COMPONENT_NAME}").mkdirs();
	new File(plantillasDir,"Servicios/${stream} - DESARROLLO -COMP- ${COMPONENT_NAME}/config.xml").bytes =  compoTemplate.bytes;
	File compoConfigXml = new File(plantillasDir,"Servicios/${stream} - DESARROLLO -COMP- ${COMPONENT_NAME}/config.xml");
	modificacion(compoConfigXml,STREAM_DESARROLLO,STREAM_RELEASE,STREAM_MANTENIMIENTO,WKS_MODIFICADA,COMPO_MODIFICADA,COMPONENT_NAME);
		
}



private void modificacion(thisConfigXml,STREAM_DESARROLLO,STREAM_RELEASE,STREAM_MANTENIMIENTO,WKS_MODIFICADA,COMPO_MODIFICADA,COMPONENT_NAME) {
	XmlUtils utils = new XmlUtils();
	Document buildDoc = utils.parseXml(thisConfigXml);
	
	Node [] descriptionNodes = utils.xpathNodes(buildDoc, "//description")
	descriptionNodes.each { Node descriptionNode ->
		descriptionNode.setTextContent("");
	}
	
	Node [] cargaInicialNodes = utils.xpathNodes(buildDoc, "//streamCargaInicial")
	cargaInicialNodes.each { Node cargaInicialNode ->
		cargaInicialNode.setTextContent(STREAM_DESARROLLO);
	}
	
	Node[] defaultValueNodes = utils.xpathNodes(buildDoc, "//defaultValue");
	
	defaultValueNodes.each { Node node ->
		if(node.getTextContent().equals("STREAM - DESARROLLO")) {
			node.setTextContent(STREAM_DESARROLLO);
		}
		if(node.getTextContent().equals("STREAM - RELEASE")) {
			node.setTextContent(STREAM_RELEASE);
		}
		if(node.getTextContent().equals("STREAM - MANTENIMIENTO")) {
			node.setTextContent(STREAM_MANTENIMIENTO);
		}
		if(node.getTextContent().contains("COMPONENT_NAME")) {
			def newStr = node.getTextContent().replaceAll("COMPONENT_NAME",COMPONENT_NAME);
			node.setTextContent(newStr);			
		}
		if(node.getTextContent().contains("CARGA_INICIAL")) {
			def newStr = node.getTextContent().replaceAll("CARGA_INICIAL",STREAM_DESARROLLO);
			node.setTextContent(newStr);
		}
		// CARGA_INICIAL
	}

	Node wksNode = utils.xpathNode(buildDoc, "/project/customWorkspace");
	String newWks = wksNode.getTextContent().replaceAll("WKS_MODIFICADA", WKS_MODIFICADA).replaceAll("COMPO_MODIFICADA", COMPO_MODIFICADA);
	wksNode.setTextContent(newWks);

	XmlWriter.transformXml(buildDoc, thisConfigXml);
}

