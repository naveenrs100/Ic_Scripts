package es.eci.utils.versioner

import java.io.File;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import es.eci.utils.pom.ArtifactObject;
import es.eci.utils.pom.NodeProps

class VersionerComun {

	/**
	 * Acción que calcula qué nodos de version son necesarios modificar
	 * en base al contenido del artifacts.json.
	 * @param File dir
	 * @param String artifactsJson
	 * @param Closure c1 A ejecutar en el caso de que la versión venga indicada directamente.
	 * @param Closure c2 A ejecutar en el caso de que la versión venga indicada como variable.
	 */
	public static void action(File dir, String artifactsJson, Closure c1, Closure c2) {
		def pomRaiz = new File(dir.getCanonicalPath() + "/pom.xml");
		def treeNodesMap = XmlUtils.getTreeNodesMap(dir);
		ArrayList<ArtifactObject> artifacts = XmlUtils.getArtifactsMap(artifactsJson);

		def touchedProperties = [];

		dir.eachFileRecurse { File file ->
			def docRaiz = XmlUtils.parseXml(pomRaiz);
			if(pathAllowed(file)) { // Se excluyen los archivos de target.
				boolean isRaiz = (file.getCanonicalPath() == pomRaiz.getCanonicalPath())
				if(file.getName() == "pom.xml") {
					def doc = XmlUtils.parseXml(file);
					def nodeVersion = XmlUtils.xpathNode(doc, "/project/version");
					def mainArtifact = XmlUtils.xpathNode(doc, "/project/artifactId");
					def nodeParentVersion = XmlUtils.xpathNode(doc, "/project/parent/version");
					def nodeParentArtifact = XmlUtils.xpathNode(doc, "/project/parent/artifactId");
					def parentArtifactId = nodeParentArtifact != null ? nodeParentArtifact.getTextContent() : "";

					touchedProperties = actionNode(doc,nodeVersion,file,pomRaiz,docRaiz,isRaiz,touchedProperties,false,mainArtifact.getTextContent(),artifacts,c1,c2)

					if(!file.getCanonicalPath().equals(pomRaiz.getCanonicalPath())
					&& (parentArtifactId != "eci-pom") && (parentArtifactId != "atg-super-pom")) {
						touchedProperties = actionNode(doc,nodeParentVersion,file,pomRaiz,docRaiz,isRaiz,touchedProperties,false,parentArtifactId,artifacts,c1,c2)
					}

					// Comprobamos las dependencias.
					Node[] docDeps = XmlUtils.xpathNodes(doc, "/project/dependencies/dependency");
					docDeps.each { Node docDepNode ->
						String depArtifactId = (XmlUtils.getChildNodes(docDepNode).find { it.getNodeName() == "artifactId" }).getTextContent();
						Node depVersionNode = XmlUtils.getChildNodes(docDepNode).find { it.getNodeName() == "version" }
						String depGroup = XmlUtils.getChildNodes(docDepNode).find { it.getNodeName() == "groupId" }.getTextContent();

						if(depVersionNode != null) {
							Node finalDepNode;
							String depVersionText = depVersionNode.getTextContent();
							def finalDepVersion;
							if(depVersionText.contains("\${") && !depVersionText.contains("\${project.") && !depVersionText.contains("\${parent.")) {
								finalDepNode = XmlUtils.getFinalPropNode(file,treeNodesMap,depVersionText).getNode();
								finalDepVersion = finalDepNode.getTextContent();
							}
							else {
								finalDepNode = depVersionNode;
								finalDepVersion = finalDepNode.getTextContent();
							}
							
							boolean depInArtifactsJson = isDepInArtifacts(artifacts,depArtifactId,depGroup,finalDepVersion);
							
							if(depInArtifactsJson) {
								// Si la dependencia está incluida en el artifacts.json
								touchedProperties = actionNode(doc,depVersionNode,file,pomRaiz,docRaiz,isRaiz,touchedProperties,true,depArtifactId,artifacts,c1,c2);
							}
							else if(!depInArtifactsJson && !touchedProperties.contains(finalDepNode.getNodeName()) && !finalDepVersion.contains("\${project.") && !finalDepVersion.contains("\${parent.")) {
								// Si la dependencia no está incuida en el artifacts.json
								ArtifactObject targetObject = artifacts.find { it.getArtifactId().equals(depArtifactId) && it.getGroupId().equals(depGroup) }
								if(targetObject != null) {
									println("touchedProperties -> ${touchedProperties}");
									println("[WARNING] La dependencia \"${depGroup}:${depArtifactId}:${finalDepVersion}\" en " +
											"el pom.xml \"${file.getCanonicalPath()}\" tiene la versión descuadrada");
								}
							}
						}
						else {
							println("[WARNING] La dependencia \"${depArtifactId}\" del pom \"${file.getCanonicalPath()}\" viene " +
									"indicada sin la tag <version>!");
						}
					}
				}
			}
		}
	}

	/**
	 * Método que actúa sobre los nodos versión que se le pasa y los modifica
	 * en base a las closures que reciba en c1 y c2.
	 * @param doc
	 * @param nodeVersion
	 * @param file
	 * @param pomRaiz
	 * @param docRaiz
	 * @param isRaiz
	 * @param touchedProperties
	 * @param isDependency
	 * @param c1
	 * @param c2
	 * @return
	 */
	private static actionNode(Document doc, Node nodeVersion, File file, File pomRaiz,
			Document docRaiz, boolean isRaiz, ArrayList<String> touchedProperties, boolean isDependency,
			String artifactId, ArrayList<ArtifactObject> artifacts, Closure c1, Closure c2) {

		if(nodeVersion != null) {
			def nodeVersionText = nodeVersion.getTextContent();
			if(!nodeVersionText.contains("\${")) {
				//CLOSURE:
				c1(nodeVersion, doc, file, artifacts, artifactId);

			} else if(nodeVersionText.contains("\${") && (isRaiz || isDependency) && !nodeVersionText.trim().contains("\${project.") && !nodeVersionText.trim().contains("\${parent.")) {
				def thisDoc;
				if(isRaiz) {
					println("Estamos en las dependencias del pom raiz.");
					thisDoc = doc;
				} else {
					println("No estamos en las dependencias del pom raiz.");
					thisDoc = docRaiz;
				}
				// CLOSURE:
				touchedProperties = c2(nodeVersionText, touchedProperties, thisDoc, pomRaiz, isDependency, doc, file, artifactId, artifacts);
				return touchedProperties;
			}
		}
		return touchedProperties;

	}

	/**
	 * Determina si la ruta del archivo está permitida (no está dentro
	 * de "target", ".jazz5", ".jazzShed")
	 * @param file
	 * @return boolean allowed
	 */
	public static boolean pathAllowed(File file) {
		def sep = System.getProperty("file.separator");
		def allowed = true;
		def forbiddenDirs = ["target",".jazz5",".jazzShed","src${sep}main","META-INF"];

		forbiddenDirs.each { String forbiddenDir ->
			if(file.getCanonicalPath().contains("${sep}${forbiddenDir}${sep}")) {
				allowed = false;
			}
		}
		return allowed;
	}
	
	/**
	 * 
	 * @param ArrayList<ArtifactObject> artifacts
	 * @param String depArtifactId
	 * @param String depGroup
	 * @param String finalDepVersion
	 * @return boolean True si la dependencia está dentro del artifacts.
	 */
	public static boolean isDepInArtifacts(ArrayList<ArtifactObject> artifacts, 
		String depArtifactId, String depGroup, String finalDepVersion) {
		boolean isDepInArtifacts = false;
		artifacts.each { ArtifactObject ao ->
			if( ao.getArtifactId().equals(depArtifactId) &&
				ao.getGroupId().equals(depGroup) &&
				ao.getVersion().equals(finalDepVersion)) {				
				isDepInArtifacts = isDepInArtifacts || true;				
			}
		}
		return isDepInArtifacts;
	}

}
