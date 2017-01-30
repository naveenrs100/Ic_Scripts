import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;
import es.eci.utils.TmpDir;
import es.eci.utils.pom.ArtifactObject;
import es.eci.utils.pom.NodeProps
import es.eci.utils.versioner.XmlUtils;
import groovy.xml.XmlUtil;
import static org.junit.Assert.*;

import org.apache.commons.jexl.junit.Asserter;

import es.eci.utils.encoding.EncodingUtils;

class TestXmlUtils {

	def resourceZipPath = "src/test/resources/versioner/PruebaRelease-App-2.zip";	

	@Test
	public void testTransformXml() {
		TmpDir.tmp { File tmpDir ->
			println("##### testTransformXml:");
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath())
			def pomFile = new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2/App2-WAR/pom.xml");
			def originalEncoding = EncodingUtils.getEncodingName(pomFile);

			Document doc = XmlUtils.parseXml(pomFile);
			XmlUtils.transformXml(doc, pomFile);
			def finalEncoding = EncodingUtils.getEncodingName(pomFile);

			assertEquals(originalEncoding,finalEncoding);
		}
	}

	@Test
	public void testGetEncoding() {
		TmpDir.tmp { File tmpDir ->
			println("##### testGetEncoding:");
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath())
			def file1 = new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2/pom.xml");
			def file2 = new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2/App2-WAR/pom.xml")
			def encoding1 = EncodingUtils.getEncodingName(file1);
			def encoding2 = EncodingUtils.getEncodingName(file2);

			assertEquals("UTF-8", encoding1)
			assertEquals("ISO-8859-1", encoding2)
		}
	}

	@Test
	public void testParseXml() {
		TmpDir.tmp { File tmpDir ->
			println("##### testParseXml:");
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath())
			Document doc = XmlUtils.parseXml(new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2/App2-WAR/pom.xml"));
			assertNotNull(doc);

			def artifactId = XmlUtils.xpathNode(doc, "/project/artifactId").getTextContent()
			assertEquals("App2-WAR",artifactId);

			def dependencies = XmlUtils.xpathNodes(doc,"/project/dependencies/dependency");
			assertEquals(4,dependencies.size());

			def plugins = XmlUtils.xpathNodes(doc,"/project/build/plugins/plugin");
			assertEquals(2,plugins.size());
		}
	}

	@Test
	public void testXpathNode() {
		println("##### testXpathNode:");
		TmpDir.tmp { File tmpDir ->
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath())
			Document doc = XmlUtils.parseXml(new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2/App2-WAR/pom.xml"));

			def node = XmlUtils.xpathNode(doc, "/project/artifactId")
			assertTrue(node instanceof org.w3c.dom.Node);

			assertEquals("App2-WAR",node.getTextContent());
		}
	}

	@Test
	public void testXpathNodes() {
		println("##### testXpathNodes:");
		TmpDir.tmp { File tmpDir ->
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath())
			Document doc = XmlUtils.parseXml(new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2/pom.xml"));
			def nodes = XmlUtils.xpathNodes(doc, "/project/modules/module")

			assertTrue(nodes instanceof org.w3c.dom.Node[]);
			assertEquals("App2-WAR",nodes[0].getTextContent());
			assertEquals("App2-EAR",nodes[1].getTextContent());
		}
	}

	@Test
	public void testLookupProperty() {
		println("##### testLookupProperty:");
		TmpDir.tmp { File tmpDir ->
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath())
			Document doc = XmlUtils.parseXml(new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2/App2-EAR/pom.xml"));
			def propValue = XmlUtils.lookupProperty(doc, "plugin_was_version");
			def nullValue = XmlUtils.lookupProperty(doc, "no_existe_prop");

			assertNotNull(propValue);
			assertNull(nullValue);
			assertEquals("1.0.5.0",propValue);
		}
	}

	@Test
	public void testConvertToArray() {
		println("##### testConvertToArray:");
		TmpDir.tmp { File tmpDir ->
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath())
			Document doc = XmlUtils.parseXml(new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2/pom.xml"));

			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodeList = xPath.evaluate("/project/modules/module", doc.getDocumentElement(), XPathConstants.NODESET);

			def nodes = XmlUtils.convertToArray(nodeList);

			assertTrue(nodes instanceof org.w3c.dom.Node[]);
			assertEquals(2,nodes.size());
		}
	}

	@Test
	public void testSolve() {
		println("##### testSolve:");
		TmpDir.tmp { File tmpDir ->
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath())
			Document doc = XmlUtils.parseXml(new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2/App2-EAR/pom.xml"));
			def direct = XmlUtils.solve(doc, "plugin_was_version");
			def variable = XmlUtils.solve(doc, '${plugin_was_version}');
			
			assertEquals("plugin_was_version",direct);
			assertEquals("1.0.5.0",variable);
		}
	}
	
	@Test
	public void testSolveRecursive() {
		println("##### testSolveRecursive:");
		TmpDir.tmp { File tmpDir ->
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath())
			File file = new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2/App2-WAR/hijo/pom.xml");
			Document doc = XmlUtils.parseXml(file);
			def version = XmlUtils.xpathNode(doc, "/project/dependencies/dependency/version").getTextContent();
			
			String valor = XmlUtils.solveRecursive(new File(tmpDir.getCanonicalPath() + "/PruebaRelease-App-2"), doc, version, file);
			
			assertEquals("1.0.0.0-SNAPSHOT", valor);
		}
	}
	
	@Test
	public void testSetProperty() {
		println("##### testSetProperty:");
		TmpDir.tmp { File tmpDir ->
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath())
			Document doc = XmlUtils.parseXml(new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2/App2-EAR/pom.xml"));
			XmlUtils.setProperty(doc, "plugin_was_version", "CAMBIADA");
			XmlUtils.transformXml(doc, new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2/App2-EAR/pom.xml"));
			
			doc = XmlUtils.parseXml(new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2/App2-EAR/pom.xml"));
			def newProNode = XmlUtils.xpathNode(doc, "/project/properties/plugin_was_version");
			
			assertEquals("CAMBIADA",newProNode.getTextContent());
		}
	}

	@Test
	public void testParse() {
		println("##### testParse:");
		String[] result1 = XmlUtils.parse('aaa');
		String[] expected1 = ['aaa'];
		
		String[] result2 = XmlUtils.parse('asldkf${pom-variable}B');
		String[] expected2 = ['asldkf', '${pom-variable}', 'B'];
		
		String[] result3 = XmlUtils.parse('${pom-variable-1}-${pom-variable2}-${pom-variable3}');
		String[] expected3 = ['${pom-variable-1}', '-', '${pom-variable2}', '-', '${pom-variable3}'];
		
		String[] result4 = XmlUtils.parse('${pom-variable}-SNAPSHOT');
		String[] expected4 = ['${pom-variable}', '-SNAPSHOT'];
		
		assertArrayEquals(expected1, result1);
		assertArrayEquals(expected2, result2);
		assertArrayEquals(expected3, result3);
		assertArrayEquals(expected4, result4);
	}
	
	@Test
	public void testXpathGetChildNodes() {
		println("##### testXpathGetChildNodes:");
		TmpDir.tmp { File tmpDir ->
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath())
			Document doc = XmlUtils.parseXml(new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2/pom.xml"));			
			Node[] childNodes = XmlUtils.xpathGetChildNodes(doc, "/project/modules");
			
			assertEquals(2,childNodes.size());
			
			assertEquals("App2-WAR",childNodes[0].getTextContent());
			assertEquals("module",childNodes[0].getNodeName());
			
			assertEquals("App2-EAR",childNodes[1].getTextContent());
			assertEquals("module",childNodes[1].getNodeName());
		}
	}
	
	@Test
	public void testgetChildNodes() {
		println("##### testgetChildNodes:");
		TmpDir.tmp { File tmpDir ->
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath())
			Document doc = XmlUtils.parseXml(new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2/pom.xml"));
			Node node = XmlUtils.xpathNode(doc, "/project/modules");
			Node[] childNodes = XmlUtils.getChildNodes(node);
			
			assertEquals(2,childNodes.size());
			
			assertEquals("App2-WAR",childNodes[0].getTextContent());
			assertEquals("module",childNodes[0].getNodeName());
			
			assertEquals("App2-EAR",childNodes[1].getTextContent());
			assertEquals("module",childNodes[1].getNodeName());
		}
	}
	
	@Test
	public void testGetArtifactsMap() {
		println("##### testGetArtifactsStream:");
		File artifactsFile = new File("src/test/resources/versioner/artifactsJson/01artifactsInicial.json");		
		ArrayList<ArtifactObject> artifacts = XmlUtils.getArtifactsMap(artifactsFile.getText());
		
		assertEquals(8,artifacts.size());
		assertEquals("App1-EAR",artifacts[0].getArtifactId());
		assertEquals("App1-WAR",artifacts[1].getArtifactId());
		assertEquals("App1-POM",artifacts[2].getArtifactId());
		assertEquals("App2-EAR",artifacts[3].getArtifactId());
		assertEquals("App2-WAR",artifacts[4].getArtifactId());
		assertEquals("App2-POM",artifacts[5].getArtifactId());
		assertEquals("lib1",artifacts[6].getArtifactId());
		assertEquals("lib2",artifacts[7].getArtifactId());		
	}
	
	@Test
	public void testGetFinalPropNode() {
		println("##### testGetFinalPropNode:");
		TmpDir.tmp { File tmpDir ->
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath());
			File pomFile = new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2/App2-WAR/hijo/pom.xml");
			def treeNodes = XmlUtils.getTreeNodesMap(new File(tmpDir,"PruebaRelease-App-2"));
			NodeProps result = XmlUtils.getFinalPropNode(pomFile, treeNodes, '${version-no-entra}');
			
			assertEquals("2.0.0-SNAPSHOT",result.getNode().getTextContent())
						
			result.getNode().setTextContent("CAMBIADO");
			XmlUtils.transformXml(result.getDoc(), result.getPomFile());
			println result.getPomFile().getText()
			Document newDoc = XmlUtils.parseXml(result.getPomFile());
			Node finalNode = XmlUtils.xpathNode(newDoc, "/project/properties/mas-version-no-entra")
			
			assertEquals("CAMBIADO",finalNode.getTextContent());
			
		}
	}
	
	/**
	 * Descomprime un zip en la ruta especificada.
	 * @param zipPath
	 * @param destination
	 * @return
	 */
	private void unzipTestsZip(zipPath, destination) {
		def ant = new AntBuilder();
		ant.unzip(  src: zipPath,
		dest: destination,
		overwrite:"true" )
	}

}
