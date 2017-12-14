package es.eci.utils.versioner;

import java.io.File;

import es.eci.utils.pom.ArtifactObject;
import es.eci.utils.pom.NodeProps;
import es.eci.utils.transfer.FileDeployer;
import es.eci.utils.versioner.VersionerComun;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import es.eci.utils.versioner.XmlUtils;

class PomXmlOperations {

	/**
	 * Comprueba que todas las variables de un árbol de poms están resueltas
	 * en ese mismo árbol. Si no lo está, devuelve una lista con las variables
	 * que no lo están y dónde están
	 * @return (ArrayList) Variables no resueltas 
	 */
	public static checkVariables(File dir, String nexusUrl) {
		XmlUtils utils = new XmlUtils();
		def treeNodesMap = utils.getTreeNodesMap(dir);
		ArrayList<NodeProps> notResolvedVariables = [];
		dir.eachFileRecurse { File file ->
			if(VersionerComun.pathAllowed(file) && file.getName() == "pom.xml") {
				Document doc = utils.parseXml(file);
				def mainVersionNode = utils.xpathNode(doc, "/project/version");
				if(mainVersionNode != null) {
					def mainVersion = mainVersionNode.getTextContent();
					if(mainVersion.contains("\$") && !mainVersion.contains("\${project.") && !mainVersion.contains("\${parent.")) {
						try {
							// Se intenta resolver la variable. Si da NumberFormatException se almacena para devolverla como error.
							utils.getFinalPropNode(file, treeNodesMap, mainVersion, nexusUrl).getNode();
						} catch(NumberFormatException e) {
							NodeProps mainVersionNodeProps = new NodeProps(doc, mainVersionNode, file);
							notResolvedVariables.add(mainVersionNodeProps);

						}
					}
				}

				// Comprobamos ahora las posibles variables en dependencias.
				def depNodes = utils.xpathNodes(doc, "/project/dependencies/dependency");
				depNodes.each { Node docDepNode ->
					Node depVersionNode = utils.getChildNodes(docDepNode).find { it.getNodeName() == "version" }
					if(depVersionNode != null) {
						def depVersion = depVersionNode.getTextContent();
						if(depVersion.contains("\${") && !depVersion.contains("\${project.") && !depVersion.contains("\${parent.")) {
							try {
								utils.getFinalPropNode(file, treeNodesMap, depVersion, nexusUrl).getNode().getTextContent();
							} catch (NumberFormatException e) {
								NodeProps depVersionNodeProps = new NodeProps(doc, depVersionNode, file);
								notResolvedVariables.add(depVersionNodeProps);
							}
						}
					}
				}
			}
		}

		return notResolvedVariables;
	}

	/**
	 * Comprueba no sólo que la versión de los pom.xml esté abierta sino
	 * que sólo las versiones de las dependencias que estén en el artifacts.json
	 * están abiertas. El resto no puede estarlo. 
	 * @param dir Directorio base de donde cuelgan los pom.xml.
	 * @param artifactsJson Json de artefactos que componen la release.
	 */
	public static checkOpenVersionAndDeps(File dir, String artifactsJson, String nexusUrl) {
		XmlUtils utils = new XmlUtils();
		def treeNodesMap = utils.getTreeNodesMap(dir);
		ArrayList<ArtifactObject> artifacts = utils.getArtifactsMap(artifactsJson);
		dir.eachFileRecurse { File file ->
			if(VersionerComun.pathAllowed(file) && file.getName() == "pom.xml") {
				Document doc = utils.parseXml(file);
				// Comprobamos que la versión principal del pom.xml está abierta.
				def mainVersionNode = utils.xpathNode(doc, "/project/version");
				def artifactId = utils.xpathNode(doc, "/project/artifactId").getTextContent();
				if(mainVersionNode != null) {
					def mainVersion = mainVersionNode.getTextContent();
					if(!mainVersion.contains("\${")) {
						if(!mainVersion.trim().endsWith("-SNAPSHOT")) {
							throw new NumberFormatException("La versión del pom.xml \"${file.getCanonicalPath()}\" debe estar en formato \"-SNAPSHOT\". Ahora mismo vale ${mainVersion}.");
						}
					} else if(!mainVersion.contains("\${project.") && !mainVersion.contains("\${parent.")) {
						def finalNode = utils.getFinalPropNode(file, treeNodesMap, mainVersion, nexusUrl).getNode();
						if(!finalNode.getTextContent().endsWith("-SNAPSHOT")) {
							throw new NumberFormatException("La versión del pom.xml \"${file.getCanonicalPath()}\" debe estar en formato \"-SNAPSHOT\". Ahora mismo vale ${finalNode.getTextContent()}.");
						}
					}
				}

				// Comprobamos las dependencias. Si las que no están en el artifactsJson están abiertas damos error.
				def depNodes = utils.xpathNodes(doc, "/project/dependencies/dependency");
				depNodes.each { Node docDepNode ->
					String depArtifactId = (utils.getChildNodes(docDepNode).find { it.getNodeName() == "artifactId" }).getTextContent();
					Node depVersionNode = utils.getChildNodes(docDepNode).find { it.getNodeName() == "version" }
					String depGroup = utils.getChildNodes(docDepNode).find { it.getNodeName() == "groupId" }.getTextContent();

					println("Mirando dependencia ${depArtifactId} del archivo ${file.getCanonicalPath()}")

					if(depVersionNode != null) {

						String depVersionText = depVersionNode.getTextContent();
						def finalDepVersion;
						if(depVersionText.contains("\${") && !depVersionText.contains("\${project.") && !depVersionText.contains("\${parent.")) {
							finalDepVersion = utils.getFinalPropNode(file,treeNodesMap,depVersionText,nexusUrl).getNode().getTextContent();
						}
						else {
							finalDepVersion = depVersionText;
						}

						boolean isDepInArtifacts = false;
						artifacts.each { ArtifactObject ao ->
							if(ao.getArtifactId().equals(depArtifactId) &&
							ao.getGroupId().equals(depGroup) &&
							ao.getVersion().equals(finalDepVersion)) {
								isDepInArtifacts = isDepInArtifacts || true;
							}
						}

						ArtifactObject originArtifact = artifacts.find {
							it.getArtifactId().equals(depArtifactId) && it.getGroupId().equals(depGroup);
						}

						if(!isDepInArtifacts) {
							// Si la dependencia NO está en los artifacts no se le permite estar abierta.
							def depVersion = depVersionNode.getTextContent();
							def resolvedDepVersionText;
							if(depVersion.contains("\${") && !depVersion.contains("\${project.") && !depVersion.contains("\${parent.")) {
								resolvedDepVersionText = utils.getFinalPropNode(file,treeNodesMap,depVersion,nexusUrl).getNode().getTextContent();
							}
							else {
								resolvedDepVersionText = depVersion;
							}
							if(resolvedDepVersionText.contains("SNAPSHOT")) {
								if(originArtifact != null) {
									throw new NumberFormatException(
									"\nLa version de la dependencia con el artefacto \"${depArtifactId}\" en el pom ${file.getCanonicalPath()} ahora mismo " +
									"vale ${resolvedDepVersionText}. No puede estar en formato \"-SNAPSHOT\" o " +
									"deberia estar apuntando a ${originArtifact.getVersion()}, que es la version del artefacto que entra en esta release. " +
									"Corrijalo antes de volver a intentar otra release por favor."
									);
								} else {
									throw new NumberFormatException(
									"\nLa version de la dependencia ${depArtifactId} del pom ${file.getCanonicalPath()} no puede estar abierta por estar " +
									"apuntando a un artefacto del cual no se esta haciendo release."
									);
								}
							}
						}
					}
					else {
						println("[WARNING] La dependencia \"${depArtifactId}\" del pom \"${file.getCanonicalPath()}\" viene indicada sin la tag <version>!");
					}
				}
			}
		}
	}	

	/**
	 * Crea un archivo 'version.txt' con el contenido de la versión.
	 * @param File dir
	 * @return String version
	 */
	public static String createVersionFile(File dir) {
		def pomMainFile = new File(dir, "pom.xml");
		if(pomMainFile.exists()) {
			XmlUtils utils = new XmlUtils();
			Document doc = utils.parseXml(pomMainFile);

			Node versionNode = utils.xpathNode(doc, "/project/version");
			Node groupIdNode = utils.xpathNode(doc, "/project/groupId")

			if(versionNode == null || groupIdNode == null) {
				throw new NullPointerException(
				"O versión o el groupId del pom.xml que se encuentra en " +
				"\"${dir.getCanonicalPath()}/pom.xml\" no están correctamente indicadas.");
			}

			def version = versionNode.getTextContent();
			def groupId = groupIdNode.getTextContent();

			version = utils.solve(doc, version);

			def versionFile = new File(dir, "version.txt");
			versionFile.text = "";
			versionFile.append("version=\"${version}\"\n");
			versionFile.append("groupId=\"${groupId}\"");
		} else {
			println("No se ha encontrado pom.xml en la raíz del proyecto. No se crea el version.txt por ahora.");
		}
	}

	/**
	 * Comprueba únicamente que la versión de un pom.xml está abierta.
	 * No mira ni dependencias ni nada más.
	 * @param dir Directorio de origen de los pom.xml
	 */
	public static void checkOpenVersion(File dir, String nexusUrl) {
		XmlUtils utils = new XmlUtils();
		def treeNodesMap = utils.getTreeNodesMap(dir);
		dir.eachFileRecurse { File file ->
			if(VersionerComun.pathAllowed(file) && file.getName() == "pom.xml") {
				Document doc = utils.parseXml(file);
				Node mainVersionNode = utils.xpathNode(doc, "/project/version");
				String artifactId = utils.xpathNode(doc, "/project/artifactId").getTextContent();
				if(mainVersionNode != null) {
					def mainVersion = mainVersionNode.getTextContent();
					def resolvedMainVersion = mainVersion.contains("\${") ? 
						utils.getFinalPropNode(file, treeNodesMap, mainVersion,nexusUrl).
							getNode().getTextContent() : mainVersion;
					if(!resolvedMainVersion.trim().endsWith("-SNAPSHOT")) {
						throw new NumberFormatException(
							"La versión del pom \"${file.getCanonicalPath()}\" debe estar abierta. Ahora mismo es ${mainVersion}");
					}
				}
			}
		}
	}

	/**
	 * Comprueba que la versión de un pom.xml está cerrada, así como todas
	 * las dependencias.
	 * @param dir Directorio de origen de los pom.xml
	 */
	public static void checkAllClosedVersions(File dir) {
		def pomRaiz = new File(dir.getCanonicalPath() + "/pom.xml")
		if(pomRaiz != null) {
			XmlUtils utils = new XmlUtils();
			def docRaiz = utils.parseXml(pomRaiz);
			dir.eachFileRecurse { File file ->
				if(VersionerComun.pathAllowed(file) && file.getName() == "pom.xml") {
					Document doc = utils.parseXml(file);
					Node mainVersionNode = utils.xpathNode(doc, "/project/version");
					if(mainVersionNode != null) {
						if(mainVersionNode.getNodeType() == Node.ELEMENT_NODE) {
							def version = utils.solveRecursive(dir, doc, 
								mainVersionNode.getTextContent(), file);
	
							if(version != null) {
								version = utils.solve(docRaiz, version);
								if(version.trim().endsWith("-SNAPSHOT")) {
									throw new NumberFormatException(
									"[${file.getCanonicalPath()}]: La versión del pom.xml NO debe acabar en -SNAPSHOT.");
								}
							}
						}
					}
					Node[] dependencies = utils.xpathNodes(doc, "/dependencies/dependency");
					dependencies.each { Node dependency ->
						if (dependency.getNodeType() == Node.ELEMENT_NODE) {
							def version = utils.solve(docRaiz, mainVersionNode.getTextContent())
							if (version != null) {
								version = utils.solve(docRaiz, version);
								if(version.trim().endsWith("-SNAPSHOT")) {
									throw new NumberFormatException(
									"[${file.getCanonicalPath()}]: La versión de las dependencias NO debe acabar en -SNAPSHOT.");
								}
							}
						}
					}
				}
			}
		}
	}


	/**
	 * Comprueba que todas las versiones, tanto las de poms como
	 * las de dependencias, estén cerradas, sin excepción. 
	 * Se usará en los procesos de fix y hotfix. 
	 * @param dir
	 */
	@Deprecated
	public static void checkClosedVersion(File dir, String nexusUrl) {
		XmlUtils utils = new XmlUtils();
		def treeMap = utils.getTreeNodesMap(dir);
		dir.eachFileRecurse { File file ->
			if(VersionerComun.pathAllowed(file) && file.getName() == "pom.xml") {
				println("Analizamos ${file.getCanonicalPath()}...")
				def doc = utils.parseXml(file);
				// Comprobamos que la version del pom.xml esté cerrada.
				def nodeVersion = utils.xpathNode(doc, "/project/version");
				if(nodeVersion != null) {
					def nodeVersionText = nodeVersion.getTextContent();
					def resolvedNode = utils.getFinalPropNode(file, treeMap, nodeVersionText, nexusUrl);
					if(resolvedNode.getNode().getTextContent().contains("-SNAPSHOT")) {
						throw new NumberFormatException("La versión del pom.xml ${file.getCanonicalPath()} no está cerrada.");
					}
				}

				// Comprobamos que las versiones de las dependencias estén cerradas.
				Node[] depVersionNodes = utils.xpathNodes(doc, "/project/dependencies//version");
				depVersionNodes.each { Node depVersionNode ->
					def depVersionNodeText = depVersionNode.getTextContent();
					def resolvedNode = utils.getFinalPropNode(file, treeMap, depVersionNodeText);
					if(resolvedNode.getNode().getTextContent().contains("-SNAPSHOT")) {
						throw new NumberFormatException("El pom.xml ${file.getCanonicalPath()} tiene dependencias sin cerrar.");
					}
				}
			}
		}
	}

}
