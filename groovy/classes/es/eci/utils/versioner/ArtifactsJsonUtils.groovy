package es.eci.utils.versioner

import components.MavenComponent
import es.eci.utils.VersionUtils
import es.eci.utils.base.Loggable
import es.eci.utils.pom.PomNode
import es.eci.utils.pom.PomTree
import org.w3c.dom.Document

class ArtifactsJsonUtils extends Loggable {
	
	private static VersionUtils vUtils = new VersionUtils();

	public static processSnapshotMaven(Map<String, List<File>> poms, File home){
		Map<String,List<ArtifactBeanLight>> artifactsComp = getArtifactsMaven(poms,home)
				
		def artifactsJson = writeJsonArtifactsMaven(artifactsComp)
		
		return artifactsJson;
	}

	public static buildArtifactsFile(List components, File baseDirectory, String action) {
		List<MavenComponent> ret = null;
		// Creación del artifacts.json
		Map<String, List<File>> poms = new HashMap<String, List<File>>();
		components.each { String component->
			//poms.put(component, getPoms(new File(baseDirectory, component)))
			poms.put(component, getPoms(baseDirectory))
		}
		
		def artifactsJson = processSnapshotMaven(poms, baseDirectory)		

		return artifactsJson;

	}

	// Devuelve el listado de pom.xml bajo un directorio
	public static getPoms(File fromDir, String fileMatch = "pom\\.xml"){
		List<File> files = []
		PomTree tree = new PomTree(fromDir);
		for (Iterator<PomNode> iterator = tree.widthIterator(); iterator.hasNext();) {
			PomNode node = iterator.next();
			files << node.getFile()
		}
		return files
	}

	// Devuelve la lista de artefactos maven en una serie de ficheros pom.xml
	public static Map<String,List<ArtifactBeanLight>> getArtifactsMaven(Map<String, List<File>> poms, File baseDirectory) {
		Map<String,List<ArtifactBeanLight>> artifacts = [:];
		poms.each { fComp ->
			def comp = fComp.key
			def pomFileList = fComp.value
			def list = []
			pomFileList.each { File pomFile ->
				println("Intentando leer ${pomFile} ...")
				try {					
					Document doc = XmlUtils.parseXml(pomFile);
					ArtifactBeanLight artifactBean = getArtifactMaven(doc, baseDirectory, pomFile);
					list.add(artifactBean);
				}
				catch(Exception e) {
					println fichero.getCanonicalPath()
					println fichero.size() + " bytes"
					println fichero.text
					throw e
				}
			}
			artifacts.put(comp,list)
		}
		return artifacts
	}
	
	/**
	 * Construye un objeto ArtifactBeanLight con la información de artifactId,
	 * groupId y versión (ya resuelta) de un pom.xml
	 * @param doc
	 * @param baseDirectory
	 * @param file
	 * @return ArtifactBeanLight artifact
	 */
	public static ArtifactBeanLight getArtifactMaven(Document doc, File baseDirectory, File file){
		ArtifactBeanLight artifact = new ArtifactBeanLight()
		def treeMapNode = XmlUtils.getTreeNodesMap(baseDirectory);
		//artifact.version = pom.version.text()			
		def docVersionNode = XmlUtils.xpathNode(doc, "/project/version")
		if(docVersionNode != null && docVersionNode.getTextContent().length() > 0) {			
			if(!docVersionNode.getTextContent().contains("\${")) {
				artifact.version = docVersionNode.getTextContent();
			} else if(!docVersionNode.getTextContent().contains("\${project.") && !docVersionNode.getTextContent().contains("\${parent.")) {
				artifact.version = XmlUtils.getFinalPropNode(file, treeMapNode, docVersionNode.getTextContent()).getNode().getTextContent();				
			}
						
		} else {
			def docParentVersionNode = XmlUtils.xpathNode(doc, "/project/parent/version")
			if(!docParentVersionNode.getTextContent().contains("\${")) {
				artifact.version = docParentVersionNode.getTextContent();
			} else if(!docParentVersionNode.getTextContent().contains("\${project.") && !docParentVersionNode.getTextContent().contains("\${parent.")) {
				artifact.version = XmlUtils.getFinalPropNode(file, treeMapNode, docParentVersionNode.getTextContent()).getNode().getTextContent();				
			}
		}
	
		println("Calculada artifact.version: ${artifact.version} para el pom.xml \"${file.getCanonicalPath()}\"")
		// Resolver la versión contra una propiedad si fuera necesario
		artifact.artifactId = XmlUtils.xpathNode(doc, "/project/artifactId").getTextContent()
		def docGroupIdNode = XmlUtils.xpathNode(doc, "/project/groupId");
		if(docGroupIdNode != null) {
			artifact.groupId = 	docGroupIdNode.getTextContent();
		} else {
			artifact.groupId = XmlUtils.xpathNode(doc, "/project/parent/groupId").getTextContent();			
		}
		
		return artifact
	}



	// Escribe el fichero de artefactos maven en el directorio de la construcción
	public static writeJsonArtifactsMaven(Map<String,List<ArtifactBeanLight>> artifactsComp) {		
		def cont = 0
		String artifactsJson = "";
		artifactsJson = artifactsJson + "["; 
		artifactsComp.each { artsComp ->
			def cont2 = 0
			cont = cont + 1
			def comp = artsComp.key
			List<ArtifactBeanLight> artifacts = artsComp.value
			artifacts.each{ ArtifactBeanLight artifact ->
				cont2 = cont2 +1
				artifactsJson = artifactsJson + "{\"version\":\"${artifact.version}\",\"component\":\"${comp}\",\"groupId\":\"${artifact.groupId}\",\"artifactId\":\"${artifact.artifactId}\"}";
				if (cont < artifactsComp.size() || cont2 != artifacts.size() )					
					artifactsJson = artifactsJson + ","; 
			}
		}
		artifactsJson = artifactsJson + "]";

		return artifactsJson;
	}

	/**
	 * Devuelve el parámetro artifactsJson que se envíe desde el job de corriente o,
	 * en caso de venir ninguno se crea un artifactsJson propio del componente al vuelo.
	 * @param params Parámetros de entrada al step.
	 * @param parentWorkspaceFile
	 * @return String artifactsJson
	 */
	public static getArtifactsJson(Map<String,String> params, String component, File parentWorkspaceFile, String action) {
		def artifactsJson = params["artifactsJson"];
		if(artifactsJson == null || artifactsJson.trim().equals("") || artifactsJson.trim().equals("\${artifactsJson}")) {
			println("No hay un artifactsJson desde arriba. Comprobamos si existe un fichero \"artifacts.json\" en el workspace del componente.");
			def artifactsFile = new File(parentWorkspaceFile.getCanonicalPath() + "/artifacts.json");
			if(artifactsFile.exists()) {
				println("Sacamos el artifacts.json del archivo.");
				artifactsJson = artifactsFile.getText();
			} else {
				def components = ["${component}"]								
				println("Creamos nuestro propio artifactsJson al vuelo.");
				artifactsJson = buildArtifactsFile(components, parentWorkspaceFile, action);
			}
		}
		println("El artifactsJson utilizado para este el componente ${component} es \n ${artifactsJson}");

		return artifactsJson;
	}

	/**
	 * Clase privada con la información de un artefacto.	 *
	 */
	private static class ArtifactBeanLight {
		public String version
		public String groupId
		public String artifactId
		public boolean equals (object){
			if (object!=null){
				if (object.groupId==groupId && object.artifactId == this.artifactId)
					return true
			}
			return false
		}
	}
}
