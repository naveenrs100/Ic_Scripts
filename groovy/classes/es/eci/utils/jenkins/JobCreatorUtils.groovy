package es.eci.utils.jenkins;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import es.eci.utils.base.Loggable
import es.eci.utils.versioner.XmlUtils;
import es.eci.utils.versioner.XmlWriter;

public class JobCreatorUtils extends Loggable {

	/**
	 * Devuelve el nodo correspondiente al nombre de parámetro que se le indique.
	 * @param parameterName
	 * @param parametersNode
	 * @return
	 */
	public static modifyParameterNode(String parameterName, String finalValue, Node parametersNode, XmlUtils xmlUtils) {
		Node[] parameterNodes = xmlUtils.getChildNodes(parametersNode);
		for(parameterNode in parameterNodes) {
			Node nodeName = xmlUtils.getChildNodes(parameterNode).find { it.getNodeName() == "name"}
			if(nodeName.getTextContent().equals(parameterName)) {
				Node defaultValueNode = xmlUtils.getChildNodes(parameterNode).find { it.getNodeName() == "defaultValue" }
				System.out.println("##################################  Cambiando el valor a " + parameterName + " por " + finalValue)
				defaultValueNode.setTextContent(finalValue);
			}
		}
	}
	
	/**
	 * Devuelve un string sustituyendo espacios en blanco, signos de puntuación y guiones medios por guiones bajos.
	 * @param value
	 * @return
	 */
	public static String normalize(String value) {
		def ret = value.replaceAll(" - ", "_")
				.replaceAll(" -","_")
				.replaceAll("- ","_")
				.replaceAll("\\.","_")
				.replaceAll(" ","_")
				.replaceAll("\\(", "")
				.replaceAll("\\)", "");
		return ret;
	}
	
	/**
	 * Crea el config.xml final del job
	 * @param productDirectory
	 * @param product
	 * @param streamType
	 * @param action
	 * @param doc
	 * @return
	 */
	public static writeFinalJobFile(File productDirectory, String product, String streamType, String action, Document doc, String scm) {
		File destDir;
		if(scm.equals("rtc")) {
			destDir = new File(productDirectory, "${product} - ${streamType} - ${action}");
		}
		if(scm.equals("git")) {
			destDir = new File(productDirectory, "${product} - ${action}");
		}
		destDir.mkdirs();
		File destFile = new File(destDir,"config.xml");
		destFile.createNewFile();
		XmlWriter.transformXml(doc, destFile);
	}
	
	/**
	 * Crea el config.xml final del job de componente
	 * @param productDirectory
	 * @param product
	 * @param streamType
	 * @param component
	 * @param doc
	 */
	public static writeFinalComponentJobFile(File productDirectory, String product, String streamType, String component, Document doc, String scm, String streamCargaInicial = null) {
		File destDir;
		if(scm.equals("rtc")) {
			if(streamCargaInicial == null || streamCargaInicial.trim().equals("")) {
				destDir = new File(productDirectory, "${product} - ${streamType} -COMP- ${component}");
			} else {
				destDir = new File(productDirectory, "${streamCargaInicial} -COMP- ${component}");
			}
		}
		if(scm.equals("git")) {
			destDir = new File(productDirectory, "${product} -COMP- ${component}");
		}
		destDir.mkdirs();
		File destFile = new File(destDir,"config.xml");
		destFile.createNewFile();
		XmlWriter.transformXml(doc, destFile);
	}

}