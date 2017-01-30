package es.eci.utils.pom

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Clase de apoyo que define un nodo mediante su org.w3c.dom.Node,
 * su org.w3c.org.Document y el file al que pertenece.
 */
class NodeProps {
	
	Document doc;
	Node node;
	File pomFile
	
	public NodeProps(Document doc, Node node, File pomFile) {
		this.doc = doc;
		this.node = node;
		this.pomFile = pomFile;
	}
	
	public Document getDoc() {
		return doc;
	}
	public Node getNode() {
		return node;
	}
	public File getPomFile() {
		return pomFile;
	}

}
