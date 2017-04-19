package es.eci.utils.versioner

import es.eci.utils.pom.NodeProps

/**
 * Esta clase agrupa los métodos de tratamiento de pom que
 * necesitan de escritura de los mismos.
 * 
 * La razón de que se refactorizasen fuera es la necesidad de separar
 * aquellos métodos que tienen necesidad de bibliotecas externas para
 * su uso en jenkins -> execute groovy script, que permite usar
 * grapes.
 */
class PomXmlWriteOperations {

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
	public static removeSnapshot(File dir, String artifactsJson, String nexusUrl) {
		def c1 = { nodeVersion, doc, file, artifacts, artifactId ->
			// Si la version viene escrita se actualiza de forma normal en el pom.xml que la contiene.
			def nodeVersionText = nodeVersion.getTextContent();
			def version = nodeVersionText.split("-SNAPSHOT")[0];
			nodeVersion.setTextContent(version);
			XmlWriter.transformXml(doc, file);
		}

		def c2 = { nodeVersionText, touchedProperties, thisDoc, pomRaiz, isDependency, doc, file, artifactId, artifacts ->
			// Si la versión viene como variable se actualiza su valor en el pom.xml raíz, que es donde viene definida.
			def treeNodesMap = XmlUtils.getTreeNodesMap(dir);
			def finalPropNode = XmlUtils.getFinalPropNode(file, treeNodesMap, nodeVersionText, nexusUrl);
			// Si la propiedad ya ha sido tocada no volver a tocarla.
			if(!touchedProperties.contains(finalPropNode.getNode().getNodeName())) {
				def newVersion = finalPropNode.getNode().getTextContent().split("-SNAPSHOT")[0];
				finalPropNode.getNode().setTextContent(newVersion);

				XmlWriter.transformXml(finalPropNode.getDoc(), finalPropNode.getPomFile());
				touchedProperties.add(finalPropNode.getNode().getNodeName());
			}
			return touchedProperties;
		}

		VersionerComun.action(dir, artifactsJson, c1, c2, nexusUrl);
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
	public static addSnapshot(File dir, String artifactsJson, String nexusUrl) {
		def c1 = { nodeVersion, doc, file, artifacts, artifactId ->
			// Si la version viene escrita se actualiza de forma normal en el pom.xml que la contiene.
			if(!nodeVersion.getTextContent().endsWith("-SNAPSHOT")) {
				def newVersion = nodeVersion.getTextContent() + "-SNAPSHOT";
				nodeVersion.setTextContent(newVersion);
				XmlWriter.transformXml(doc, file);
			}
		}

		def c2 = { nodeVersionText, touchedProperties, thisDoc, pomRaiz, isDependency, doc, file, artifactId, artifacts ->
			// Si la versión viene como variable se actualiza su valor en el pom.xml raíz, que es donde viene definida.
			def treeNodesMap = XmlUtils.getTreeNodesMap(dir);
			def finalNodeProp = XmlUtils.getFinalPropNode(file, treeNodesMap, nodeVersionText, nexusUrl);
			if(!touchedProperties.contains(finalNodeProp.getNode().getNodeName())) { // Si la propiedad ya ha sido tocada no volver a tocarla.
				def isSnapshot = finalNodeProp.getNode().getTextContent().contains("-SNAPSHOT");
				if(!isSnapshot) {
					def newVersion = finalNodeProp.getNode().getTextContent() + "-SNAPSHOT";
					finalNodeProp.getNode().setTextContent(newVersion);

					XmlWriter.transformXml(finalNodeProp.getDoc(), finalNodeProp.getPomFile());
					touchedProperties.add(finalNodeProp.getNode().getNodeName());
				}
			}
			return touchedProperties;
		}

		VersionerComun.action(dir, artifactsJson, c1, c2, nexusUrl);
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
	public static increaseVersion(File dir, int index, String artifactsJson, String nexusUrl) {
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
				XmlWriter.transformXml(doc, file);
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
				XmlWriter.transformXml(doc, file);
			}
			else if(!isSnapshot && isHotfix) {
				if(versionDigits[3].split("-").size() == 1) {
					versionDigits.set(3, versionDigits[3] + "-1");
					def newVersion = versionDigits[0] + "." + versionDigits[1] + "." + versionDigits[2] + "." + versionDigits[3];
					nodeVersion.setTextContent(newVersion);
					XmlWriter.transformXml(doc, file);
				}
				else if(versionDigits[3].split("-").size() == 2) {
					def hotFixDigit = versionDigits[3].split("-")[1].toInteger() + 1;
					versionDigits.set(3, versionDigits[3].split("-")[0] + "-" + hotFixDigit);
					def newVersion = versionDigits[0] + "." + versionDigits[1] + "." + versionDigits[2] + "." + versionDigits[3];
					nodeVersion.setTextContent(newVersion);
					XmlWriter.transformXml(doc, file);
				}
			}
			else if(isSnapshot || isHotFix) {
				throw new NumberFormatException("Se está intentando convertir a hotFix una versión SNAPSHOT en pom ${file.getCanonicalPath()}.");
			}
		}

		def c2 = { nodeVersionText, touchedProperties, thisDoc, pomRaiz, isDependency, doc, file, artifactId, artifacts ->
			// Si la versión viene como variable se actualiza su valor en el pom.xml raíz, que es donde viene definida.
			def treeNodesMap = XmlUtils.getTreeNodesMap(dir);
			def finalPropNode = XmlUtils.getFinalPropNode(file, treeNodesMap, nodeVersionText, nexusUrl);
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
				XmlWriter.transformXml(finalPropNode.getDoc(), finalPropNode.getPomFile());
				touchedProperties.add(finalPropNode.getNode().getNodeName());
			}
			return touchedProperties;
		}

		VersionerComun.action(dir, artifactsJson, c1, c2, nexusUrl);
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
	public static fillVersion(File dir, String artifactsJson, String nexusUrl) {
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
					XmlWriter.transformXml(doc, file);
				} else {
					def finalVersion = numericPart;
					nodeVersion.setTextContent(finalVersion);
					XmlWriter.transformXml(doc, file);
				}
			}
		}

		def c2 = { nodeVersionText, touchedProperties, thisDoc, pomRaiz, isDependency, doc, file, artifactId, artifacts ->
			// Si la versión viene en formato de variable lo resolvemos recursivamente hasta el pom raiz
			def treePomsMap = XmlUtils.getTreeNodesMap(dir);
			NodeProps finalPropNode = XmlUtils.getFinalPropNode(file, treePomsMap, nodeVersionText, nexusUrl);

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
					XmlWriter.transformXml(finalPropNode.getDoc(), finalPropNode.getPomFile());
					touchedProperties.add(finalPropNode.getNode().getNodeName());
				} else {
					def versionPropNode = finalPropNode.getNode();
					versionPropNode.setTextContent(numericPart);
					XmlWriter.transformXml(finalPropNode.getDoc(), finalPropNode.getPomFile());
					touchedProperties.add(finalPropNode.getNode().getNodeName());
				}
			}
			return touchedProperties;
		}

		VersionerComun.action(dir, artifactsJson, c1, c2, nexusUrl);
	}
}
