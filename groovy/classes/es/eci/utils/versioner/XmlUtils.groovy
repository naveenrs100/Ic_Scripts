package es.eci.utils.versioner;

import java.nio.charset.Charset

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList

import es.eci.utils.NexusHelper;
import es.eci.utils.TmpDir;
import es.eci.utils.base.Loggable
import es.eci.utils.pom.ArtifactObject
import es.eci.utils.pom.MavenCoordinates
import es.eci.utils.pom.MavenCoordinatesDocument
import es.eci.utils.pom.NodeProps
import es.eci.utils.pom.PomNode
import es.eci.utils.pom.PomTree
import groovy.json.JsonSlurper

public class XmlUtils extends Loggable {

	// Cache de propiedades
	private Map<File, Map<String, String>> propertiesCache = [:]

	/**
	 * Obtiene el encoding de un archivo utilizando herramientas
	 * nativas de Groovy
	 * @param pomFile
	 * @return String encoding
	 */
	@Deprecated
	public String getEncodingNameWithGroovy(File pomFile) {
		CharsetToolkit toolkit = new CharsetToolkit(pomFile);
		Charset guessedCharset = toolkit.getCharset();
		log("CHARSET de ${pomFile.getAbsolutePath()}-> " + guessedCharset.name())

		String encoding = null;
		if(guessedCharset.name().equals("UTF-8")) {
			encoding = "UTF-8";
		} else if(guessedCharset.name().equals("windows-1252")
		|| guessedCharset.name().equals("ISO-8859-1")) {
			encoding = "ISO-8859-1";
		} else {
			throw new Exception("Encoding desconocido para el archivo ${pomFile.getAbsolutePath()}");
		}
		return encoding;
	}

	/**
	 * Devuelve un org.w3c.dom.Document con el contenido de un xmlFile parseado.
	 * @param xmlFile
	 * @return org.w3c.dom.Document
	 */
	public Document parseXml(File xmlFile) {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

		Document doc = dBuilder.parse(xmlFile);
		doc.getDocumentElement().normalize();

		return doc;
	}

	/**
	 * Devuelve un org.w3c.dom.Document con el contenido de un string xml.
	 * @param xml
	 * @return org.w3c.dom.Document
	 */
	public Document parseXmlString(String xml) {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

		Document doc = dBuilder.parse(xml);
		doc.getDocumentElement().normalize();

		return doc;
	}

	/**
	 * Devuelve un Node de un Document doc según la query de xPath
	 * que se le pase como argumento
	 * @param Document doc
	 * @param String xPathQuery
	 * @return NodeList nodes
	 */
	public Node xpathNode(Document doc, String xPathQuery) {
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node node = xPath.evaluate("${xPathQuery}", doc.getDocumentElement(), XPathConstants.NODE);
		return node;
	}

	/**
	 * Devuelve una lista de nodos Node[] de un Document doc según la query de xPath
	 * que se le pase como argumento
	 * @param Document doc
	 * @param String xPathQuery
	 * @return NodeList nodes
	 */
	public Node[] xpathNodes(Document doc, String xPathQuery) {
		XPath xPath = XPathFactory.newInstance().newXPath();
		NodeList nodes = xPath.evaluate("${xPathQuery}", doc.getDocumentElement(), XPathConstants.NODESET);
		Node[] nodesArray = nodes != null ? convertToArray(nodes) : [];
		return nodesArray;
	}

	/**
	 * Intenta resolver una propiedad contra el elemento <properties/>
	 * de un pom.xml
	 */
	public String lookupProperty(Document doc, String value) {
		Node nodeProp = xpathNode(doc, "/project/properties/${value}");
		def ret = nodeProp != null ? nodeProp.getTextContent() : null;
		return ret;
	}

	/**
	 * Resuelve de forma recursiva una variable mirando en el árbol de poms
	 * hacia arriba quién la tiene definida.
	 * @param File baseDirectory Directorio base donde está el componente.
	 * @param Document doc Documento parseado por org.w3c.dom
	 * @param String variable a resolver (en el formato ${variable})
	 * @return
	 */
	public String solveRecursive(File baseDirectory, Document doc, String variable, File docFile) {
		StringBuilder sb = new StringBuilder();
		List<String> tokens = parse(variable);
		for(String token: tokens) {
			if (token.startsWith('${') && token.endsWith('}')) {
				// Inmersión recursiva (resolviendo antes la propiedad)
				sb.append(solve(doc, lookupPropertyRecursive(baseDirectory,
						doc, token.substring(2, token.length() - 1),docFile)));
			}
			else {
				// Caso trivial
				sb.append(token);
			}
		}
		return sb.toString();
	}

	/**
	 * 
	 * @param baseDirectory
	 * @param doc
	 * @param value
	 * @return
	 */
	public String lookupPropertyRecursive(File baseDirectory, Document doc, String value, File docFile) {
		String ret = '';
		def isNull = { String s ->
			return s == null || s.trim().length() == 0;
		}

		// Caso trivial
		def solveProjectParentVersion = { Document document ->
			if (xpathNode(document, "/project/parent") != null
			&& xpathNode(document, "/project/parent/version") != null
			&& !isNull(xpathNode(document, "/project/parent/version").getTextContent())) {
				ret = solveRecursive(baseDirectory, document, xpathNode(document, "/project/parent/version").getTextContent(),docFile);
			}
		}

		if (value == "project.version") {
			// Puede estar en el propio elemento
			if (xpathNode(doc, "/project.version") != null && !isNull(xpathNode(doc, "/project.version").getTextContent())) {
				ret = solveRecursive(baseDirectory, doc, xpathNode(doc, "/project.version").getTextContent(),docFile)
			}
			else {
				// Entonces el parent version
				solveProjectParentVersion(doc);
			}
		}
		else if (value == "project.parent.version") {
			solveProjectParentVersion(doc);
		}
		else {
			// Poblar el mapa de propiedades
			String tmp = lookupProperty(doc, value);
			if (!isNull(tmp) && !tmp.startsWith('${')) {
				ret = tmp;
			}
			else {
				Map<String, String> properties = populatePropertiesMap(baseDirectory, doc, docFile);
				ret = properties[value];
			}
		}
		return ret;
	}

	/**
	 * Construye una tabla de propiedades desde la raíz hasta el pom indicado
	 * @param baseDirectory
	 * @param doc
	 * @param docFile
	 * @return
	 */
	private Map<String, String> populatePropertiesMap(File baseDirectory, Document doc, File docFile) {
		Map<String, String> ret = null;
		if (propertiesCache[baseDirectory] != null) {
			ret = propertiesCache[baseDirectory];
		}
		else {
			ret = new HashMap<String, String>();
			PomTree tree = new PomTree(baseDirectory);
			MavenCoordinates soughtCoordinates = MavenCoordinatesDocument.readPomDocument(doc, docFile);
			PomNode actual = null;
			for (Iterator<PomNode> iterator = tree.widthIterator();
			iterator.hasNext() && actual == null;) {
				// Buscar el nodo
				PomNode tmp = iterator.next();
				MavenCoordinates tmpCoordinates = tmp.getCoordinates();
				if(soughtCoordinates.artifactId == tmpCoordinates.artifactId
				&& soughtCoordinates.groupId == tmpCoordinates.groupId
				&& soughtCoordinates.version == tmpCoordinates.version
				&& soughtCoordinates.packaging == tmpCoordinates.packaging
				&& soughtCoordinates.classifier == tmpCoordinates.classifier) {
					actual = tmp;
				}
			}
			if (actual != null) {
				// Recorrer hacia arriba
				Deque<PomNode> nodes = new LinkedList<PomNode>();
				PomNode n = actual;
				nodes << n;
				while (n.getParent() != null) {
					nodes.push(n.getParent());
					n = n.getParent();
				}
				// Ahora descabezar la cola, empezando por el padre
				while(nodes.size() > 0) {
					PomNode node = nodes.pop();
					for (String key: node.getProperties().keySet()) {
						ret[key] = node.getProperties()[key];
					}
				}
			}
			propertiesCache.put(baseDirectory, ret);
		}
		return ret;
	}

	/**
	 * Convert NodelList to ArrayList
	 * @param list
	 * @return
	 */
	public Node[] convertToArray(NodeList list) {
		int length = list.getLength();
		Node[] copy = new Node[length];
		for (int n = 0; n < length; ++n) {
			copy[n] = list.item(n);
		}
		return copy;
	}

	/**
	 * Intenta resolver los valores posibles de la versión
	 * de un pom a una String. En el peor caso:
	 ...
	 <version>${core-version}</version>
	 ...
	 <properties>
	 <toolkit-version>21.0.0</toolkit-version>
	 <core-version>${toolkit-version}-SNAPSHOT</core-version>
	 </properties>
	 Debería resolver la versión a:
	 21.0.0-SNAPSHOT
	 */	
	public String solve(Document doc, String s) {
		StringBuilder sb = new StringBuilder();
		List<String> tokens = parse(s);
		for(String token in tokens) {
			if (token.startsWith('${') && token.endsWith('}')) {
				// Inmersión recursiva (resolviendo antes la propiedad)
				def propertyValue = lookupProperty(doc, token.substring(2, token.length() - 1));
				if(propertyValue != null) {
					sb.append(solve(doc, propertyValue));
				}
			}
			else {
				// Caso trivial
				sb.append(token);
			}
		}
		return sb != null ? sb.toString() : null;
	}

	/**
	 * Hace lo que "solve" pero devuelve además el nombre del parámetro que
	 * tenía el valor final de la variable.
	 * @param doc
	 * @param s
	 * @return Map<String,String> sb
	 */
	@Deprecated
	public solveWithParam(Document doc, String propName) {
		def result = [:];
		def propertyName = propName.substring(2, propName.length() - 1);
		if (propName.startsWith('${') && propName.endsWith('}')) {
			def propertyValue = lookupProperty(doc, propertyName);
			if(propertyValue != null && !propertyValue.startsWith('${')) {
				result.put("${propertyName}", "${propertyValue}");
				return result;
			} else {
				solveWithParam(doc, propertyValue);
			}
		}
	}

	/**
	 * Cambia el valor de una propiedad en un pom.xml
	 * @param doc
	 * @param property
	 * @param value
	 */
	public void setProperty(Document doc, String property, String value) {
		Node node = xpathNode(doc, "/project/properties/${property}")
		if(node != null) {
			node.setTextContent(value);
		}

	}

	/**
	 * Parsea una cadena en sus componentes, separando propiedades
	 * Por ejemplo:
	 * 'aaa' -> ['aaa']
	 * 'asldkf${pom-variable}B' -> ['asldkf', '${pom-variable}', 'B']
	 * '${pom-variable-1}-${pom-variable2}-${pom-variable3}' -> ['${pom-variable-1}', '-', '${pom-variable-2}', '-', '${pom-variable-3}']
	 * '${pom-variable}-SNAPSHOT' -> ['${pom-variable}', '-SNAPSHOT']
	 */
	public List<String> parse(String s) {
		List<String> ret = null;
		if (s != null) {
			ret = new LinkedList<String>();
			int counter = 0;
			while (counter < s.length()) {
				int forward = counter;
				if (s.charAt(counter) == '$') {
					// Variable
					while(forward < s.length() && s.charAt(forward) != '}') {
						forward++;
					}
					// Consumir el último '}'
					forward++;
				}
				else {
					while (forward < s.length() && s.charAt(forward) != '$') {
						forward++;
					}
				}
				ret.add(s.substring(counter, forward));
				counter = forward;
			}
		}
		return ret;
	}

	/**
	 * Devuelve una lista de org.w3c.dom.Nodes ya limpia y filtrada que sólo sean 
	 * del tipo Node.ELEMENT_NODE a partir de un xpath indicado.
	 * @param doc
	 * @param xPathQuery
	 * @return Node[] nodePropsArray
	 */
	public Node[] xpathGetChildNodes(Document doc, String xPathQuery) {
		Node[] nodePropsArray = [];
		Node nodeProps = xpathNode(doc, xPathQuery);
		if(nodeProps != null) {
			nodePropsArray = convertToArray(nodeProps.getChildNodes()).findAll { it.getNodeType() == Node.ELEMENT_NODE };
		}
		return nodePropsArray;
	}

	/**
	 * Devuelve una lista de Nodes ya limpia y filtrada que sólo sean
	 * del tipo Node.ELEMENT_NODE a partir de un org.w3c.dom.Node
	 * @param Document doc
	 * @param Node node
	 * @return Node[] nodePropsArray
	 */
	public Node[] getChildNodes(Node node) {
		Node[] nodePropsArray = convertToArray(node.getChildNodes()).findAll { it.getNodeType() == Node.ELEMENT_NODE };
		return nodePropsArray;
	}


	/**
	 * Devuelve un nodo que cuelgue de otro en base a un xpath
	 * relativo indicado
	 * @param Node node
	 * @param String xPathQuery
	 * @return Node
	 */
	public Node getChildNode(Node node, String xPathQuery) {
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node retNode = xPath.evaluate("${xPathQuery}", node, XPathConstants.NODE);
		return retNode;
	}

	/**
	 * Devuelve los artifacts indicados en el artifacts.json
	 * @param dir
	 * @return
	 */
	@Deprecated
	public String[] getArtifactsStream(String artifactsJson) {
		def artifacts = []
		if(artifactsJson.trim() != "") {
			log("Se parsea el parametro artifactsJson con contenido: ${artifactsJson}")
			JsonSlurper jsonSlurper = new JsonSlurper();
			def jsonObject = jsonSlurper.parseText(artifactsJson);
			jsonObject.each {
				artifacts.add(it.artifactId);
			}
		}
		return artifacts;
	}

	/**
	 * Devuelve los un mapa a partir del artifacts.json que relaciona
	 * artifacts con versiones.
	 * @param dir
	 * @return
	 */
	public ArrayList<ArtifactObject> getArtifactsMap(String artifactsJson) {
		ArrayList<ArtifactObject> artifacts = []

		if(artifactsJson.trim() != "") {
			log("Se parsea el parametro artifactsJson con contenido: ${artifactsJson}")
			JsonSlurper jsonSlurper = new JsonSlurper();
			def jsonObject = jsonSlurper.parseText(artifactsJson);
			jsonObject.each {
				def artifObject = new ArtifactObject(it.groupId, it.artifactId, it.version);
				artifacts.add(artifObject);
			}
		}
		return artifacts;
	}

	/**
	 * DEPRECATED: Use "getFinalPropNode" instead.
	 * Devuelve el nodo que define una propiedad, el cual puede estar 
	 * definido en el propio pom.xml o en el pom.xml raíz.
	 * @param propName
	 * @param doc
	 * @param thisDoc
	 * @param artifactId
	 * @param file
	 * @return
	 */
	@Deprecated
	public Node getPropNode(propName, doc, thisDoc, artifactId, file) {
		def propNode;
		if(xpathNode(doc, "/project/properties/${propName}") != null) {
			propNode = xpathNode(doc, "/project/properties/${propName}");
		} else if(xpathNode(thisDoc, "/project/properties/${propName}") != null) {
			propNode = xpathNode(thisDoc, "/project/properties/${propName}");
		} else {
			throw new NumberFormatException(
			"La dependencia \"${artifactId}\" del pom \"${file.getCanonicalPath()}\" tiene " +
			"como versión una variable que no puede ser resuelta ni contra las propiedades del " +
			"pom.xml raíz ni contra el propio pom.xml que la define.");
		}

		return propNode;
	}

	/**
	 * Recorre el arbol de nodos y devuelve cuál es el Nodo final donde
	 * está resuelta la propiedad.
	 * @param pomFile
	 * @param treeMap
	 * @param value
	 * @return Node ret
	 */
	public NodeProps getFinalPropNode(File pomFile, Map<String,Map<String,String>> treeMap, String value, String nexusUrl = null) {
		log("Intentando resolver la propiedad \"${value}\" contra el pom.xml \"${pomFile.getCanonicalPath()}\"");
		NodeProps ret = null;
		def doc = parseXml(pomFile);
		def cleanValue = value.substring(2, value.length() -1);

		def propNode = xpathNode(doc, "/project/properties/${cleanValue}");
		if(propNode != null) {
			def propValue = propNode.getTextContent();
			if(!propValue.contains("\${")) {
				log("Encontrado valor para ${cleanValue} en ${pomFile} = ${propNode.getTextContent()}")
				ret = new NodeProps(doc, propNode, pomFile);
				return ret;

			} else if(propValue.contains("\${")) {
				def cleanPropValue = propValue.substring(2, propValue.length() -1);
				def nextPropNode = xpathNode(doc,"/project/properties/${cleanPropValue}");
				if(nextPropNode == null) {
					def parentNode = xpathNode(doc, "/project/parent/artifactId");
					if(parentNode != null) {
						Map<String,String> parentProp = treeMap.getAt(parentNode.getTextContent());
						def parentFilePath = parentProp.entrySet().iterator().next().getKey();
						File parentFile = new File(parentFilePath);
						ret = getFinalPropNode(parentFile , treeMap, propValue, nexusUrl);
					}
				} else {
					ret = getFinalPropNode(pomFile, treeMap, propValue, nexusUrl);
				}
			}
		}
		else if(propNode == null) {
			def parentNode = xpathNode(doc, "/project/parent");
			if(parentNode != null) {
				String parentArtifactId = getChildNode(parentNode, "artifactId").getTextContent();
				String parentGroupId = getChildNode(parentNode, "groupId").getTextContent();
				String parentVersion = getChildNode(parentNode, "version").getTextContent();
				if(parentArtifactId.equals("eci-pom") || parentArtifactId.equals("atg-super-pom")) {
					// Bajarse el eci-pom y el atg-super-pom de Nexus, abrirlo y ver si está definida ahí.
					if(nexusUrl != null) {
						TmpDir.tmp { File tmpDir ->
							NexusHelper nxHelper = new NexusHelper(nexusUrl);
							nxHelper.initLogger(this)
							MavenCoordinates coord = new MavenCoordinates(parentGroupId, parentArtifactId, parentVersion);
							coord.setPackaging("pom");
							File parentPomFile = nxHelper.download(coord,tmpDir);
							Document parentPomDoc = parseXml(parentPomFile);
							def parentPropNode = xpathNode(parentPomDoc, "/project/properties/${cleanValue}");
							if(parentPropNode == null) {
								// Propiedad no resuelta y estamos al final del árbol.
								throw new NumberFormatException("La propiedad \"${cleanValue}\" no se puede resolver.");
							} else {
								ret = new NodeProps(parentPomDoc, parentPropNode, parentPomFile);
							}
						}
					} else {
						log("[WARNING] No se ha indicado valor para \"nexusUrl\", no se podrá resolver la propiedad contra el pom padre \"${parentArtifactId}\".")
					}

				}
				else {
					Map<String,String> parentProp = treeMap.getAt(parentArtifactId);
					def parentFilePath = parentProp.entrySet().iterator().next().getKey();
					File parentFile = new File(parentFilePath);
					ret = getFinalPropNode(parentFile , treeMap, value, nexusUrl);
				}
			}
		}

		return ret;
	}

	/**
	 * Mete todos los nodos que cuelgan de un baseDir en un Map.
	 * @param baseDir
	 * @return treeMap
	 */
	public Map<String,Map<String,String>> getTreeNodesMap(File baseDir) {
		Map<String,Map<String,String>> treeMap = [:];
		baseDir.eachFileRecurse { File file ->
			if(VersionerComun.pathAllowed(file) && file.getName() == "pom.xml") {
				def thisFileProp = [:];
				Document doc = parseXml(file);
				def artifactId = xpathNode(doc, "/project/artifactId").getTextContent();

				def parentNode = xpathNode(doc, "/project/parent/artifactId");
				def parentArtifactId;
				if(parentNode != null) {
					parentArtifactId = parentNode.getTextContent();
				}
				def thisFilePath = file.getCanonicalPath();
				thisFileProp.put(thisFilePath, parentArtifactId);
				treeMap.put(artifactId, thisFileProp);
			}
		}
		log("treemap->\n${treeMap}\n");
		return treeMap;
	}
	
	/**
	 * Método que incrementa el dígito necesario en una versión según
	 * la acción que se le asocie.
	 * @param versionText Versión a modificar.
	 * @param action Acción a realizar sobre la version.
	 * @return newVersion Versión modificada.
	 */
	public String increaseVersionDigit(String versionText, String action, String releaseMantenimiento) {
		def isSnapshot = versionText.contains("-SNAPSHOT");
		def version = versionText.split("-SNAPSHOT")[0];
		List versionDigits = version.split("\\.");		
		def lastDigit = versionDigits.last();		
		def penultimateDigit = versionDigits[versionDigits.size() - 2];		
		def newVersion = "";
		
		if(action.toLowerCase().equals("release") && !releaseMantenimiento.equals("true")) {
			def increasedDigit = penultimateDigit.toInteger() + 1;
			versionDigits.set(versionDigits.size() - 2, increasedDigit);
			// Se pone el último dígito a 0.
			versionDigits.set(versionDigits.size() - 1, 0);
			
			for(int i = 0; i < versionDigits.size(); i++) {
				if(i < versionDigits.size() - 1) {
					newVersion = newVersion + versionDigits[i] + ".";
				} else {
					newVersion = newVersion + versionDigits[i];
				}
			}
			if(isSnapshot) {
				newVersion = newVersion + "-SNAPSHOT"
			}			
		}
		
		if(action.toLowerCase().equals("addfix") || (releaseMantenimiento.equals("true") && action.equals("release"))) {
			def increasedDigit = lastDigit.split("-")[0].toInteger() + 1;
			versionDigits.set(versionDigits.size() - 1, increasedDigit);			
			for(int i = 0; i < versionDigits.size(); i++) {
				if(i < versionDigits.size() - 1) {
					newVersion = newVersion + versionDigits[i] + ".";
				} else {
					newVersion = newVersion + versionDigits[i];
				}
			}
			if(isSnapshot) {
				throw new NumberFormatException("Se está intentando hacer Fix de versiones en formato -SNAPSHOT");
			}			
		}
		
		if(action.toLowerCase().equals("addhotfix")) {
			def increasedDigit;
			if(lastDigit.split("-").size() == 1) {
				// La versión no tenía todavía formato de hotfix (X.X.X... Y.Z)
				increasedDigit = lastDigit + "-1";
				
			} else if(lastDigit.split("-").size() == 2) {
				// La versión ya tenía formato de hotfix (X.X.X... Y.Z-H)
				def hotFixDigit = lastDigit.split("-")[1].toInteger() + 1;
				increasedDigit = lastDigit.split("-")[0] + "-" + hotFixDigit;				
			}
							
			versionDigits.set(versionDigits.size() - 1, increasedDigit);			
			for(int i = 0; i < versionDigits.size(); i++) {
				if(i < versionDigits.size() - 1) {
					newVersion = newVersion + versionDigits[i] + ".";
				} else {
					newVersion = newVersion + versionDigits[i];
				}
			}
			if(isSnapshot) {
				throw new NumberFormatException("Se está intentando hacer hotFix de versiones en formato -SNAPSHOT");
			}			
		}
		return newVersion;
	}

}
