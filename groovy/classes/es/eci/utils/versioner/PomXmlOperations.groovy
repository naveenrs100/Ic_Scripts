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
	public static checkVariables(File dir) {
		def treeNodesMap = XmlUtils.getTreeNodesMap(dir);
		ArrayList<NodeProps> notResolvedVariables = [];
		dir.eachFileRecurse { File file ->
			if(VersionerComun.pathAllowed(file) && file.getName() == "pom.xml") {
				Document doc = XmlUtils.parseXml(file);
				def mainVersionNode = XmlUtils.xpathNode(doc, "/project/version");
				if(mainVersionNode != null) {
					def mainVersion = mainVersionNode.getTextContent();
					if(mainVersion.contains("\$") && !mainVersion.contains("\${project.") && !mainVersion.contains("\${parent.")) {
						try {
							// Se intenta resolver la variable. Si da NumberFormatException se almacena para devolverla como error.
							XmlUtils.getFinalPropNode(file, treeNodesMap, mainVersion).getNode();
						} catch(NumberFormatException e) {							
							NodeProps mainVersionNodeProps = new NodeProps(doc, mainVersionNode, file);
							notResolvedVariables.add(mainVersionNodeProps);
							
						}
					}
				}

				// Comprobamos ahora las posibles variables en dependencias.
				def depNodes = XmlUtils.xpathNodes(doc, "/project/dependencies/dependency");
				depNodes.each { Node docDepNode ->
					Node depVersionNode = XmlUtils.getChildNodes(docDepNode).find { it.getNodeName() == "version" }
					if(depVersionNode != null) {
						def depVersion = depVersionNode.getTextContent();
						if(depVersion.contains("\${") && !depVersion.contains("\${project.") && !depVersion.contains("\${parent.")) {
							try {
								XmlUtils.getFinalPropNode(file, treeNodesMap, depVersion).getNode().getTextContent();
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
	public static checkOpenVersionAndDeps(File dir, String artifactsJson) {
		def treeNodesMap = XmlUtils.getTreeNodesMap(dir);
		def rootPom = new File(dir.getCanonicalPath() + "/pom.xml");
		def rootDoc = XmlUtils.parseXml(rootPom);
		ArrayList<ArtifactObject> artifacts = XmlUtils.getArtifactsMap(artifactsJson);
		dir.eachFileRecurse { File file ->
			if(VersionerComun.pathAllowed(file) && file.getName() == "pom.xml") {
				Document doc = XmlUtils.parseXml(file);
				// Comprobamos que la versión principal del pom.xml está abierta.
				def mainVersionNode = XmlUtils.xpathNode(doc, "/project/version");
				def artifactId = XmlUtils.xpathNode(doc, "/project/artifactId").getTextContent();
				if(mainVersionNode != null) {
					def mainVersion = mainVersionNode.getTextContent();
					if(!mainVersion.contains("\${")) {
						if(!mainVersion.trim().endsWith("-SNAPSHOT")) {
							throw new NumberFormatException("La versión del pom.xml \"${file.getCanonicalPath()}\" debe estar abierta. Ahora mismo vale ${mainVersion}.");
						}
					} else if(!mainVersion.contains("\${project.") && !mainVersion.contains("\${parent.")) {
						def finalNode = XmlUtils.getFinalPropNode(file, treeNodesMap, mainVersion).getNode();
						if(!finalNode.getTextContent().endsWith("-SNAPSHOT")) {
							throw new NumberFormatException("La versión del pom.xml \"${file.getCanonicalPath()}\" debe estar abierta. Ahora mismo vale ${finalNode.getTextContent()}.");
						}
					}
				}

				// Comprobamos las dependencias. Si las que no están en el artifactsJson están abiertas damos error.
				def depNodes = XmlUtils.xpathNodes(doc, "/project/dependencies/dependency");
				depNodes.each { Node docDepNode ->
					String depArtifactId = (XmlUtils.getChildNodes(docDepNode).find { it.getNodeName() == "artifactId" }).getTextContent();
					Node depVersionNode = XmlUtils.getChildNodes(docDepNode).find { it.getNodeName() == "version" }
					String depGroup = XmlUtils.getChildNodes(docDepNode).find { it.getNodeName() == "groupId" }.getTextContent();

					println("Mirando dependencia ${depArtifactId} del archivo ${file.getCanonicalPath()}")

					if(depVersionNode != null) {

						String depVersionText = depVersionNode.getTextContent();
						def finalDepVersion;
						if(depVersionText.contains("\${") && !depVersionText.contains("\${project.") && !depVersionText.contains("\${parent.")) {
							finalDepVersion = XmlUtils.getFinalPropNode(file,treeNodesMap,depVersionText).getNode().getTextContent();
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

						if(!isDepInArtifacts) {
							// Si la dependencia NO está en los artifacts no se le permite estar abierta.

							def depVersion = depVersionNode.getTextContent();
							def resolvedDepVersionText;
							if(depVersion.contains("\${") && !depVersion.contains("\${project.") && !depVersion.contains("\${parent.")) {
								resolvedDepVersionText = XmlUtils.getFinalPropNode(file,treeNodesMap,depVersion).getNode().getTextContent();
							}
							else {
								resolvedDepVersionText = depVersion;
							}
							if(resolvedDepVersionText.contains("SNAPSHOT")) {
								throw new NumberFormatException("La versión de la dependencia ${depArtifactId} del pom ${file.getCanonicalPath()} no puede estar abierta. Ahora mismo vale ${resolvedDepVersionText}");
							}
						}
					}
					else {
						println("[WARNING] La dependencia \"${depArtifactId}\" del pom \"${depArtifactId}\" viene indicada sin la tag <version>!");
					}
				}
			}
		}
	}

	/**
	 * Comprueba que una versión cumple el formato X.Y.Z.K(-SNAPSHOT) y, si no
	 * lo cumple, se le añaden los dígitos que faltan
	 * 
	 * cDirect: Closure que indica las acciones a realizar cuando la versión a tratar
	 * viene escrita de forma directa.
	 * cVariable: Closure que indica las acciones a realizar cuando la versión a tratar
	 * viene escrita en forma de variable.
	 * 
	 * @param dir 
	 * @param artifactsJsonFile
	 * @return
	 */
	public static fillVersion(File dir, String artifactsJson) {
		def c1 = { nodeVersion, doc, file, artifacts, artifactId ->
			// Si la versión viene escrita lo resolvemos en el propio pom.xml
			def versionText = nodeVersion.getTextContent();
			def isSnapshot = versionText.contains("-SNAPSHOT");
			def numericPart = versionText.split("-SNAPSHOT")[0];
			ArrayList numericPartArray = numericPart.split("\\.");
			if(numericPartArray.size() < 4) {
				println("La version de ${file.getCanonicalPath()} tiene menos de 4 dígitos.");
				def digitsToFill = 4 - numericPartArray.size();
				for(int i=0; i<digitsToFill; i++) {
					numericPartArray.add("0");
				}
				numericPart = numericPartArray[0] + "." + numericPartArray[1] + "." + numericPartArray[2] + "." +numericPartArray[3];
				if(isSnapshot) {
					def finalVersion = numericPart + "-SNAPSHOT";
					nodeVersion.setTextContent(finalVersion);
					XmlUtils.transformXml(doc, file);
				} else {
					def finalVersion = numericPart;
					nodeVersion.setTextContent(finalVersion);
					XmlUtils.transformXml(doc, file);
				}
			}
		}

		def c2 = { nodeVersionText, touchedProperties, thisDoc, pomRaiz, isDependency, doc, file, artifactId, artifacts ->
			// Si la versión viene en formato de variable lo resolvemos recursivamente hasta el pom raiz
			def treePomsMap = XmlUtils.getTreeNodesMap(dir);
			NodeProps finalPropNode = XmlUtils.getFinalPropNode(file, treePomsMap, nodeVersionText);

			ArrayList numericPartArray = finalPropNode.getNode().getTextContent().split("-SNAPSHOT")[0].split("\\.")
			if(numericPartArray.size() < 4) {
				def digitsToFill = 4 - numericPartArray.size();
				for(int i=0; i<digitsToFill; i++) {
					numericPartArray.add("0");
				}
				def numericPart = numericPartArray[0] + "." + numericPartArray[1] + "." + numericPartArray[2] + "." +numericPartArray[3];
				if(finalPropNode.getNode().getTextContent().endsWith("-SNAPSHOT")) {
					numericPart = numericPart + "-SNAPSHOT";
					def versionPropNode = finalPropNode.getNode();
					versionPropNode.setTextContent(numericPart);
					XmlUtils.transformXml(finalPropNode.getDoc(), finalPropNode.getPomFile());
					touchedProperties.add(finalPropNode.getNode().getNodeName());
				} else {
					def versionPropNode = finalPropNode.getNode();
					versionPropNode.setTextContent(numericPart);
					XmlUtils.transformXml(finalPropNode.getDoc(), finalPropNode.getPomFile());
					touchedProperties.add(finalPropNode.getNode().getNodeName());
				}
			}
			return touchedProperties;
		}

		VersionerComun.action(dir, artifactsJson, c1, c2);
	}

	/**
	 * Elimina la coletilla "-SNAPSHOT" de todas las versiones y de las dependencias
	 * que estén en el artifacts.json
	 * 
	 * cDirect: Closure que indica las acciones a realizar cuando la versión a tratar
	 * viene escrita de forma directa.
	 * cVariable: Closure que indica las acciones a realizar cuando la versión a tratar
	 * viene escrita en forma de variable.
	 * 
	 * @param dir
	 * @param artifactsJsonFile
	 * @return
	 */
	public static removeSnapshot(File dir, String artifactsJson) {
		def c1 = { nodeVersion, doc, file, artifacts, artifactId ->
			// Si la version viene escrita se actualiza de forma normal en el pom.xml que la contiene.
			def nodeVersionText = nodeVersion.getTextContent();
			def version = nodeVersionText.split("-SNAPSHOT")[0];
			nodeVersion.setTextContent(version);
			XmlUtils.transformXml(doc, file);
		}

		def c2 = { nodeVersionText, touchedProperties, thisDoc, pomRaiz, isDependency, doc, file, artifactId, artifacts ->
			// Si la versión viene como variable se actualiza su valor en el pom.xml raíz, que es donde viene definida.
			def treeNodesMap = XmlUtils.getTreeNodesMap(dir);
			def finalPropNode = XmlUtils.getFinalPropNode(file, treeNodesMap, nodeVersionText);
			// Si la propiedad ya ha sido tocada no volver a tocarla.
			if(!touchedProperties.contains(finalPropNode.getNode().getNodeName())) {
				def newVersion = finalPropNode.getNode().getTextContent().split("-SNAPSHOT")[0];
				finalPropNode.getNode().setTextContent(newVersion);

				XmlUtils.transformXml(finalPropNode.getDoc(), finalPropNode.getPomFile());
				touchedProperties.add(finalPropNode.getNode().getNodeName());
			}
			return touchedProperties;
		}

		VersionerComun.action(dir, artifactsJson, c1, c2);
	}

	/**
	 * Añade la coletilla "-SNAPSHOT" a las versiones y a las dependencias que 
	 * estén dentro del artifacts.json
	 * 
	 * cDirect: Closure que indica las acciones a realizar cuando la versión a tratar
	 * viene escrita de forma directa.
	 * cVariable: Closure que indica las acciones a realizar cuando la versión a tratar
	 * viene escrita en forma de variable.
	 * 
	 * @param dir
	 * @param artifactsJsonFile
	 * @return
	 */
	public static addSnapshot(File dir, String artifactsJson) {
		def c1 = { nodeVersion, doc, file, artifacts, artifactId ->
			// Si la version viene escrita se actualiza de forma normal en el pom.xml que la contiene.
			if(!nodeVersion.getTextContent().endsWith("-SNAPSHOT")) {
				def newVersion = nodeVersion.getTextContent() + "-SNAPSHOT";
				nodeVersion.setTextContent(newVersion);
				XmlUtils.transformXml(doc, file);
			}
		}

		def c2 = { nodeVersionText, touchedProperties, thisDoc, pomRaiz, isDependency, doc, file, artifactId, artifacts ->
			// Si la versión viene como variable se actualiza su valor en el pom.xml raíz, que es donde viene definida.
			def treeNodesMap = XmlUtils.getTreeNodesMap(dir);
			def finalNodeProp = XmlUtils.getFinalPropNode(file, treeNodesMap, nodeVersionText);
			if(!touchedProperties.contains(finalNodeProp.getNode().getNodeName())) { // Si la propiedad ya ha sido tocada no volver a tocarla.
				def isSnapshot = finalNodeProp.getNode().getTextContent().contains("-SNAPSHOT");
				if(!isSnapshot) {
					def newVersion = finalNodeProp.getNode().getTextContent() + "-SNAPSHOT";
					finalNodeProp.getNode().setTextContent(newVersion);

					XmlUtils.transformXml(finalNodeProp.getDoc(), finalNodeProp.getPomFile());
					touchedProperties.add(finalNodeProp.getNode().getNodeName());
				}
			}
			return touchedProperties;
		}

		VersionerComun.action(dir, artifactsJson, c1, c2);
	}

	/**
	 * Incrementa el dígito indicado a las versiones y dependencias que
	 * estén en el artifacts.json.
	 * 
	 * cDirect: Closure que indica las acciones a realizar cuando la versión a tratar
	 * viene escrita de forma directa.
	 * cVariable: Closure que indica las acciones a realizar cuando la versión a tratar
	 * viene escrita en forma de variable.
	 *  
	 * @param dir
	 * @param index
	 * @param artifactsJsonFile
	 * @return
	 */
	public static increaseVersion(File dir, int index, String artifactsJson) {
		def c1 = { nodeVersion, doc, file, artifacts, artifactId ->
			// Si la version viene escrita se actualiza de forma normal en el pom.xml que la contiene.
			def nodeVersionText = nodeVersion.getTextContent();
			def isSnapshot = nodeVersionText.contains("-SNAPSHOT");
			def version = nodeVersionText.split("-SNAPSHOT")[0];
			ArrayList versionDigits = version.split("\\.");
			boolean isHotfix = (index == 5);

			if(isSnapshot && !isHotfix) {
				def increasedDigit = versionDigits[index - 1].toInteger() + 1;
				versionDigits.set(index - 1, increasedDigit);
				if(index < versionDigits.size()) {
					for(int i = index; i < versionDigits.size(); i++) {
						versionDigits.set(i, 0);
					}
				}
				def newVersion = versionDigits[0] + "." + versionDigits[1] + "." + versionDigits[2] + "." + versionDigits[3] + "-SNAPSHOT";
				nodeVersion.setTextContent(newVersion);
				XmlUtils.transformXml(doc, file);
			}
			else if(!isSnapshot && !isHotfix) {
				def increasedDigit = versionDigits[index - 1].toInteger() + 1;
				versionDigits.set(index - 1, increasedDigit);
				if(index < versionDigits.size()) {
					for(int i = index; i < versionDigits.size(); i++) {
						versionDigits.set(i, 0);
					}
				}
				def newVersion = versionDigits[0] + "." + versionDigits[1] + "." + versionDigits[2] + "." + versionDigits[3];
				nodeVersion.setTextContent(newVersion);
				XmlUtils.transformXml(doc, file);
			}
			else if(!isSnapshot && isHotfix) {
				if(versionDigits[3].split("-").size() == 1) {
					versionDigits.set(3, versionDigits[3] + "-1");
					def newVersion = versionDigits[0] + "." + versionDigits[1] + "." + versionDigits[2] + "." + versionDigits[3];
					nodeVersion.setTextContent(newVersion);
					XmlUtils.transformXml(doc, file);
				}
				else if(versionDigits[3].split("-").size() == 2) {
					def hotFixDigit = versionDigits[3].split("-")[1].toInteger() + 1;
					versionDigits.set(3, versionDigits[3].split("-")[0] + "-" + hotFixDigit);
					def newVersion = versionDigits[0] + "." + versionDigits[1] + "." + versionDigits[2] + "." + versionDigits[3];
					nodeVersion.setTextContent(newVersion);
					XmlUtils.transformXml(doc, file);
				}
			}
			else if(isSnapshot || isHotFix) {
				throw new NumberFormatException("Se está intentando convertir a hotFix una versión SNAPSHOT en pom ${file.getCanonicalPath()}.");
			}
		}

		def c2 = { nodeVersionText, touchedProperties, thisDoc, pomRaiz, isDependency, doc, file, artifactId, artifacts ->
			// Si la versión viene como variable se actualiza su valor en el pom.xml raíz, que es donde viene definida.
			def treeNodesMap = XmlUtils.getTreeNodesMap(dir);
			def finalPropNode = XmlUtils.getFinalPropNode(file, treeNodesMap, nodeVersionText);
			if(!touchedProperties.contains(finalPropNode.getNode().getNodeName())) { // Si la propiedad ya ha sido tocada no volver a tocarla.
				def isSnapshot = finalPropNode.getNode().getTextContent().contains("-SNAPSHOT");
				def version = finalPropNode.getNode().getTextContent().split("-SNAPSHOT")[0];
				ArrayList versionDigits = version.split("\\.");
				boolean isHotfix = (index == 5);

				if(isSnapshot && !isHotfix) {
					def increasedDigit = versionDigits[index - 1].toInteger() + 1;
					versionDigits.set(index - 1, increasedDigit);
					def newVersion = versionDigits[0] + "." + versionDigits[1] + "." + versionDigits[2] + "." + versionDigits[3] + "-SNAPSHOT";
					finalPropNode.getNode().setTextContent(newVersion);
				}
				else if(!isSnapshot && !isHotfix) {
					def increasedDigit = versionDigits[index - 1].toInteger() + 1;
					versionDigits.set(index - 1, increasedDigit);
					def newVersion = versionDigits[0] + "." + versionDigits[1] + "." + versionDigits[2] + "." + versionDigits[3];
					finalPropNode.getNode().setTextContent(newVersion);
				}
				else if(!isSnapshot && isHotfix) {
					println("Caso hotfix con versionDigits = ${versionDigits}");
					if(versionDigits[3].split("-").size() == 1) {
						versionDigits.set(3, versionDigits[3] + "-1");
						def newVersion = versionDigits[0] + "." + versionDigits[1] + "." + versionDigits[2] + "." + versionDigits[3];
						println("newVersion = ${newVersion}")
						finalPropNode.getNode().setTextContent(newVersion);
					}
					else if(versionDigits[3].split("-").size() == 2) {
						def hotFixDigit = versionDigits[3].split("-")[1].toInteger() + 1;
						versionDigits.set(3, versionDigits[3].split("-")[0] + "-" + hotFixDigit);
						def newVersion = versionDigits[0] + "." + versionDigits[1] + "." + versionDigits[2] + "." + versionDigits[3];
						println("newVersion = ${newVersion}")
						finalPropNode.getNode().setTextContent(newVersion);
					}
				}
				else if(isSnapshot && isHotfix) {
					throw new NumberFormatException("Se está intentando convertir a hotFix una versión SNAPSHOT en pom ${finalPropNode.getPomFile().getCanonicalPath()}.");
				}
				XmlUtils.transformXml(finalPropNode.getDoc(), finalPropNode.getPomFile());
				touchedProperties.add(finalPropNode.getNode().getNodeName());
			}
			return touchedProperties;
		}

		VersionerComun.action(dir, artifactsJson, c1, c2);
	}

	/**
	 * Crea un archivo 'version.txt' con el contenido de la versión.
	 * @param File dir
	 * @return String version
	 */
	public static String createVersionFile(File dir) {
		def pomMainFile = new File(dir.getCanonicalPath() + "/pom.xml");
		Document doc = XmlUtils.parseXml(pomMainFile);

		Node versionNode = XmlUtils.xpathNode(doc, "/project/version");
		Node groupIdNode = XmlUtils.xpathNode(doc, "/project/groupId")

		if(versionNode == null || groupIdNode == null) {
			throw new NullPointerException(
			"O versión o el groupId del pom.xml que se encuentra en " +
			"\"${dir.getCanonicalPath()}/pom.xml\" no están correctamente indicadas.");
		}

		def version = versionNode.getTextContent();
		def groupId = groupIdNode.getTextContent();

		version = XmlUtils.solve(doc, version);

		def versionFile = new File(dir.getCanonicalPath() + "/version.txt");
		versionFile.text = "";
		versionFile.append("version=\"${version}\"\n");
		versionFile.append("groupId=\"${groupId}\"");
	}

	/**
	 * Comprueba únicamente que la versión de un pom.xml está abierta.
	 * No mira ni dependencias ni nada más.
	 * @param dir Directorio de origen de los pom.xml
	 */
	public static void checkOpenVersion(File dir) {
		def treeNodesMap = XmlUtils.getTreeNodesMap(dir);
		def pomRaiz = new File(dir.getCanonicalPath() + "/pom.xml")
		def docRaiz = XmlUtils.parseXml(pomRaiz);
		dir.eachFileRecurse { File file ->
			if(VersionerComun.pathAllowed(file) && file.getName() == "pom.xml") {
				Document doc = XmlUtils.parseXml(file);
				Node mainVersionNode = XmlUtils.xpathNode(doc, "/project/version");
				String artifactId = XmlUtils.xpathNode(doc, "/project/artifactId").getTextContent();
				if(mainVersionNode != null) {
					def mainVersion = mainVersionNode.getTextContent();
					def resolvedMainVersion = mainVersion.contains("\${") ? XmlUtils.getFinalPropNode(file, treeNodesMap, mainVersion).getNode().getTextContent() : mainVersion;
					if(!resolvedMainVersion.trim().endsWith("-SNAPSHOT")) {
						throw new NumberFormatException("La versión del pom \"${file.getCanonicalPath()}\" debe estar abierta. Ahora mismo es ${mainVersion}");
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
		def docRaiz = XmlUtils.parseXml(pomRaiz);
		dir.eachFileRecurse { File file ->
			if(VersionerComun.pathAllowed(file) && file.getName() == "pom.xml") {
				Document doc = XmlUtils.parseXml(file);
				Node mainVersionNode = XmlUtils.xpathNode(doc, "/project/version");
				if(mainVersionNode != null) {
					if(mainVersionNode.getNodeType() == Node.ELEMENT_NODE) {
						def version = XmlUtils.solveRecursive(dir, doc, mainVersionNode.getTextContent(), file);

						if(version != null) {
							version = XmlUtils.solve(docRaiz, version);
							if(version.trim().endsWith("-SNAPSHOT")) {
								throw new NumberFormatException(
								"[${file.getCanonicalPath()}]: La versión del pom.xml NO debe acabar en -SNAPSHOT.");
							}
						}
					}
				}
				Node[] dependencies = XmlUtils.xpathNodes(doc, "/dependencies/dependency");
				dependencies.each { Node dependency ->
					if (dependency.getNodeType() == Node.ELEMENT_NODE) {
						def version = XmlUtils.solve(docRaiz, mainVersionNode.getTextContent())
						if (version != null) {
							version = XmlUtils.solve(docRaiz, version);
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


	/**
	 * Comprueba que todas las versiones, tanto las de poms como
	 * las de dependencias, estén cerradas, sin excepción. 
	 * Se usará en los procesos de fix y hotfix. 
	 * @param dir
	 */
	@Deprecated
	public static void checkClosedVersion(File dir) {
		def docRaiz = XmlUtils.parseXml(new File(dir.getCanonicalPath() + "/pom.xml"));
		def treeMap = XmlUtils.getTreeNodesMap(dir);
		dir.eachFileRecurse { File file ->
			if(VersionerComun.pathAllowed(file) && file.getName() == "pom.xml") {
				println("Analizamos ${file.getCanonicalPath()}...")
				def doc = XmlUtils.parseXml(file);
				// Comprobamos que la version del pom.xml esté cerrada.
				def nodeVersion = XmlUtils.xpathNode(doc, "/project/version");
				if(nodeVersion != null) {
					def nodeVersionText = nodeVersion.getTextContent();
					def resolvedNode = XmlUtils.getFinalPropNode(file, treeMap, nodeVersionText);
					if(resolvedNode.getNode().getTextContent().contains("-SNAPSHOT")) {
						throw new NumberFormatException("La versión del pom.xml ${file.getCanonicalPath()} no está cerrada.");
					}
				}

				// Comprobamos que las versiones de las dependencias estén cerradas.
				Node[] depVersionNodes = XmlUtils.xpathNodes(doc, "/project/dependencies//version");
				depVersionNodes.each { Node depVersionNode ->
					def depVersionNodeText = depVersionNode.getTextContent();
					def resolvedNode = XmlUtils.getFinalPropNode(file, treeMap, depVersionNodeText);
					if(resolvedNode.getNode().getTextContent().contains("-SNAPSHOT")) {
						throw new NumberFormatException("El pom.xml ${file.getCanonicalPath()} tiene dependencias sin cerrar.");
					}
				}
			}
		}
	}

}


















