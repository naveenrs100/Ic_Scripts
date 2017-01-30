/**
 * 
 */
package es.eci.utils.pom

import org.w3c.dom.Document
import es.eci.utils.versioner.XmlUtils;
import es.eci.utils.pom.MavenCoordinates;

/**
 * Extensión de MavenCoordinates que añade un método para leer
 * un pom.xml mediante org.w3c.dom. Se hace para no "ensuciar" la
 * clase MavenCoordinates con imports innecesarios.
 */
class MavenCoordinatesDocument extends MavenCoordinates {
	
	/**
	 * Constructor con GAV y parámetros opcionales
	 * @param theGroupId G
	 * @param theArtifactId A
	 * @param theVersion V
	 */
	public MavenCoordinatesDocument(String theGroupId, String theArtifactId, String theVersion) {
		super(theGroupId, theArtifactId, theVersion);
	}
	
	/**
	 * Lee las coordenadas de un pom.xml que viene parseado mediante org.w3c.dom
	 * @param doc Estructura xml parseada mediante el parser de org.w3c.dom
	 * @return Coordenadas maven del pom
	 */
	public static MavenCoordinatesDocument readPomDocument(org.w3c.dom.Document doc, File docFile) {
		XmlUtils xmlUtils = new XmlUtils();
		String tmpGroup = null;
		if (xmlUtils.xpathNode(doc,"/project/groupId") != null && xmlUtils.xpathNode(doc,"/project/groupId").getTextContent().trim().length() > 0) {
			tmpGroup = xmlUtils.xpathNode(doc,"/project/groupId").getTextContent();
		}
		else if(xmlUtils.xpathNode(doc,"/project/parent/groupId") != null && xmlUtils.xpathNode(doc,"/project/parent/groupId").getTextContent().trim().length() > 0) {
			// El del padre
			tmpGroup = 	xmlUtils.xpathNode(doc,"/project/parent/groupId").getTextContent()
		} else {
			throw new Exception("Al pom \"${docFile.getCanonicalPath()}\" le falta el parámetro <groupId> principal o el que apunta al parent.");
		}
		
		String tmpVersion = null;
		if (xmlUtils.xpathNode(doc,"/project/version") != null && xmlUtils.xpathNode(doc,"/project/version").getTextContent().trim().length() > 0) {
			tmpVersion = xmlUtils.xpathNode(doc,"/project/version").getTextContent();
		}
		else if(xmlUtils.xpathNode(doc,"/project/parent/version") != null && xmlUtils.xpathNode(doc,"/project/parent/version").getTextContent().trim().length() > 0) {
			// El del padre
			tmpVersion = xmlUtils.xpathNode(doc,"/project/parent/version").getTextContent();
		} else {
			throw new Exception("Al pom \"${docFile.getCanonicalPath()}\" le falta el parámetro <version> principal o el que apunta al parent.");
		}
		
		String artifactId = xmlUtils.xpathNode(doc,"/project/artifactId").getTextContent();
		String packaging = null;
		if (xmlUtils.xpathNode(doc,"/project/packaging") != null && xmlUtils.xpathNode(doc,"/project/packaging").getTextContent().length() > 0) {
			packaging = xmlUtils.xpathNode(doc,"/project/packaging").getTextContent();
		}
		String classifier = null;
		if (xmlUtils.xpathNode(doc,"/project/classifier") != null && xmlUtils.xpathNode(doc,"/project/classifier").getTextContent().trim().length() > 0) {
			classifier = xmlUtils.xpathNode(doc,"/project/classifier").getTextContent();
		}
		MavenCoordinatesDocument ret = new MavenCoordinatesDocument(tmpGroup, artifactId, tmpVersion);
		if (classifier != null) {
			ret.setClassifier(classifier);
		}
		if (packaging != null) {
			ret.setPackaging(packaging);
		}
		return ret;
	}		

}
