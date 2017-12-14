import es.eci.utils.versioner.XmlUtils;
import es.eci.utils.versioner.PomXmlOperations;
import es.eci.utils.versioner.PomXmlWriteOperations;
import es.eci.utils.TmpDir;
import java.io.File;
import java.util.zip.ZipInputStream;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import static org.junit.Assert.*;
import org.apache.commons.jexl.junit.Asserter;

class TestVersioner {

	def resourceZipPath = "src/test/resources/versioner/PruebaRelease-App-2.zip";
	def resourceZipPathCerrada = "src/test/resources/versioner/PruebaRelease-App-2-Cerrada.zip";
	def resourceZipTestCheckOpen = "src/test/resources/versioner/PruebaRelease - App 2 TestCheckOpen.zip";
	
	@Test
	public void testCreateVersionFile() {
		TmpDir.tmp { File tmpDir ->
			println("##### testCreateVersionFile:")
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath())
			PomXmlOperations.createVersionFile(new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2"));
			def versionFile = new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2/version.txt");
			Properties properties = new Properties()
			versionFile.withInputStream {
				properties.load(it)
			}

			assertTrue(versionFile.exists());
			assertEquals("\"1.0.0.0-SNAPSHOT\"",properties.version);
			assertEquals("\"es.eci.release.prueba\"",properties.groupId);
		}
	}

	@Test
	public void testCheckOpenVersion() {
		TmpDir.tmp { File tmpDir ->
			println("##### testCheckOpenVersion:")
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath())
			def error = false;
			try {
				PomXmlOperations.checkOpenVersion(new File(tmpDir,"PruebaRelease-App-2"), null);
			}
			catch(NumberFormatException e) {
				error = true;
			}
			assertFalse(error);
			//TODO: Añadir uno con un resourceZipPath cerrado.
		}
	}

	@Test
	public void testCheckOpenVersionAndDeps() {
		println("##### testCheckOpenVersionAndDeps:")
		TmpDir.tmp { File tmpDir ->
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath());
			def artifactsJson = new File("src/test/resources/versioner/artifactsJson/01artifactsInicial.json");
			def error = false;
			try {
				PomXmlOperations.checkOpenVersionAndDeps(new File(tmpDir,"PruebaRelease-App-2"), artifactsJson.getText(), null);
			}
			catch(NumberFormatException e) {
				e.printStackTrace();
				error = true;
			}
			assertTrue(error);
		}
		
		TmpDir.tmp { File tmpDir ->
			unzipTestsZip(resourceZipTestCheckOpen, tmpDir.getAbsolutePath());
			def artifactsJson = new File("src/test/resources/versioner/artifactsJson/testCheckOpen.json");
			def error = false;
			try {
				PomXmlOperations.checkOpenVersionAndDeps(new File(tmpDir,"PruebaRelease - App 2"), artifactsJson.getText(), null);
			}
			catch(NumberFormatException e) {
				e.printStackTrace();
				error = true;
			}
			assertTrue(error);
		}
	}

	@Test
	public void testFillVersion() {
		TmpDir.tmp { File tmpDir ->
			println("##### testFillVersion:")
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath())
			def dir = new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2");
			def artifactsJson = new File("src/test/resources/versioner/artifactsJson/01artifactsInicial.json");
			PomXmlWriteOperations.fillVersion(dir,artifactsJson.getText(), null);

			XmlUtils utils = new XmlUtils();
			Document doc = utils.parseXml(new File(dir.getAbsolutePath() + "/pom.xml"));
			Node mainVersionNode = utils.xpathNode(doc, "/project/properties/main-version");
			Node lib1VersionNode = utils.xpathNode(doc, "/project/properties/lib1-version");
			Node depVersionNode =  utils.xpathNode(doc, "/project/properties/dep-version");
			Node otraDepVersionNode = utils.xpathNode(doc, "/project/properties/mas-otra-dep-version");

			assertEquals("1.0.0.0-SNAPSHOT",mainVersionNode.getTextContent());
			assertEquals("1.0.0.0-SNAPSHOT",lib1VersionNode.getTextContent());
			assertEquals("1.0.0.0",depVersionNode.getTextContent());
			assertEquals("2.0.0.0-SNAPSHOT", otraDepVersionNode.getTextContent());

			Document doc2 = utils.parseXml(new File(dir.getAbsolutePath() + "/App2-WAR/pom.xml"));
			Node propertyNode = utils.xpathNode(doc2, "/project/properties/mas-version-no-entra");

			assertEquals("2.0.0-SNAPSHOT",propertyNode.getTextContent());
		}
	}

	@Test
	public void testRemoveSnapshot() {
		TmpDir.tmp { File tmpDir ->
			println("##### testRemoveSnapshot:")
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath());
			def dir = new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2");

			// Camino hasta el removeSnapshot
			def artifactsJson = new File("src/test/resources/versioner/artifactsJson/01artifactsInicial.json");
			PomXmlWriteOperations.fillVersion(dir, artifactsJson.getText(), null);
			artifactsJson = new File("src/test/resources/versioner/artifactsJson/02artifacts-postFillVersion.json");
			PomXmlWriteOperations.removeSnapshot(dir, artifactsJson.getText(), null);

			XmlUtils utils = new XmlUtils();
			Document docRaiz = 		utils.parseXml(new File(dir,"pom.xml"));
			Document docApp2WAR = 	utils.parseXml(new File(dir,"App2-WAR/pom.xml"));
			Document docApp2EAR = 	utils.parseXml(new File(dir,"App2-EAR/pom.xml"));

			def mainVersion = utils.xpathNode(docRaiz, "/project/properties/main-version").getTextContent();
			def lib1Version = utils.xpathNode(docRaiz, "/project/properties/lib1-version").getTextContent();
			def depVersion = utils.xpathNode(docRaiz, "/project/properties/dep-version").getTextContent();
			def masDepVersion = utils.xpathNode(docRaiz, "/project/properties/mas-otra-dep-version").getTextContent();

			assertEquals("1.0.0.0",mainVersion);
			assertEquals("1.0.0.0",lib1Version);
			assertEquals("1.0.0.0",depVersion);
			assertEquals("2.0.0.0-SNAPSHOT",masDepVersion);

		}
	}

	@Test
	public void testIncreaseVersion() {
		TmpDir.tmp { File tmpDir ->
			println("##### testIncreaseVersion:")
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath());
			def dir = new File(tmpDir, "PruebaRelease-App-2");

			// Camino hasta el increaseVersion
			def artifactsJson = new File("src/test/resources/versioner/artifactsJson/01artifactsInicial.json");
			PomXmlWriteOperations.fillVersion(dir, artifactsJson.getText(), null);
			artifactsJson = new File("src/test/resources/versioner/artifactsJson/02artifacts-postFillVersion.json");
			PomXmlWriteOperations.removeSnapshot(dir, artifactsJson.getText(), null);
			artifactsJson = new File("src/test/resources/versioner/artifactsJson/03artifacts-postRemoveSnapshot.json");
			PomXmlWriteOperations.increaseVersion(dir, artifactsJson.getText(), null, "release", null);

			XmlUtils utils = new XmlUtils();
			Document docRaiz = utils.parseXml(new File(dir, "pom.xml"));
			Document docWAR = utils.parseXml(new File(dir, "App2-WAR/pom.xml"));
			
			def mainVersion = utils.xpathNode(docRaiz, "/project/properties/main-version").getTextContent();
			def lib1Version = utils.xpathNode(docRaiz, "/project/properties/lib1-version").getTextContent();
			def depVersion = utils.xpathNode(docRaiz, "/project/properties/dep-version").getTextContent();
			def masDepVersion = utils.xpathNode(docRaiz, "/project/properties/mas-otra-dep-version").getTextContent();
			
			def removeVersion = utils.xpathNode(docWAR, "/project/properties/remove-version").getTextContent();

			assertEquals("1.0.1.0",mainVersion);
			assertEquals("1.0.1.0",lib1Version);
			assertEquals("1.0.0.0",depVersion);
			assertEquals("2.0.0.0-SNAPSHOT",masDepVersion);
			
			assertEquals("1.0.1.0",removeVersion);
		}
	}
	
	@Test
	public void testHotFix() {
		TmpDir.tmp { File tmpDir ->		
			println("##### testHotfix:")
			unzipTestsZip(resourceZipPathCerrada, tmpDir.getAbsolutePath());
			def dir = new File(tmpDir, "PruebaRelease-App-2");

			// Camino hasta el increaseVersion
			def artifactsJson = new File("src/test/resources/versioner/artifactsJson/06artifacts-hotFix.json");
			PomXmlWriteOperations.increaseVersion(dir, artifactsJson.getText(), null, "addHotfix", null);
			
			XmlUtils utils = new XmlUtils();
			Document docHijo = utils.parseXml(new File(dir, "App2-WAR/hijo/pom.xml"));
			Document docWAR = utils.parseXml(new File(dir, "App2-WAR/pom.xml"));
			
			def hotFixNode1 = utils.xpathNodes(docHijo, "/project/dependencies//version")[5].getTextContent();
			def hotFixNode2 = utils.xpathNode(docWAR, "/project/properties/mas-hotfixVersion").getTextContent();
			
			assertEquals("1.0.0.0-1", hotFixNode1);
			assertEquals("1.0.0.0-2", hotFixNode2);
			
		}
	}

	@Test
	public void testCheckAllClosedVersion() {
		TmpDir.tmp { File tmpDir ->
			println("##### testCheckClosedVersion:")		

			def errorAbierta = false;
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath());
			def dir = new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2");
			try {
				PomXmlOperations.checkAllClosedVersions(dir);
			} catch(NumberFormatException e) {
				e.printStackTrace()
				errorAbierta = true;
			}
			assertTrue(errorAbierta)
		}
	}

	@Test
	public void testAddSnapshot() {
		TmpDir.tmp { File tmpDir ->
			println("##### testAddSnapshot:")
			unzipTestsZip(resourceZipPath, tmpDir.getAbsolutePath());

			def dir = new File(tmpDir.getAbsolutePath() + "/PruebaRelease-App-2");
			
			// Camino hasta el addSnapshot
			def artifactsJson = new File("src/test/resources/versioner/artifactsJson/01artifactsInicial.json");
			PomXmlWriteOperations.fillVersion(dir, artifactsJson.getText(), null);
			artifactsJson = new File("src/test/resources/versioner/artifactsJson/02artifacts-postFillVersion.json");
			PomXmlWriteOperations.removeSnapshot(dir, artifactsJson.getText(), null);
			artifactsJson = new File("src/test/resources/versioner/artifactsJson/03artifacts-postRemoveSnapshot.json");
			PomXmlWriteOperations.increaseVersion(dir, artifactsJson.getText(), null, "release", null);
			artifactsJson = new File("src/test/resources/versioner/artifactsJson/04artifacts-postIncreaseVersion.json");
			PomXmlWriteOperations.addSnapshot(dir, artifactsJson.getText(), null);
			
			XmlUtils utils = new XmlUtils();
			Document docRaiz = utils.parseXml(new File(dir.getAbsolutePath() + "/pom.xml"));
			Document docApp2WAR = utils.parseXml(new File(dir.getAbsolutePath() + "/App2-WAR/pom.xml"));
			Document docApp2EAR = utils.parseXml(new File(dir.getAbsolutePath() + "/App2-EAR/pom.xml"));			
			
			def mainVersion = utils.xpathNode(docRaiz, "/project/properties/main-version").getTextContent();
			def lib1Version = utils.xpathNode(docRaiz, "/project/properties/lib1-version").getTextContent();
			def depVersion = utils.xpathNode(docRaiz, "/project/properties/dep-version").getTextContent();
			def masDepVersion = utils.xpathNode(docRaiz, "/project/properties/mas-otra-dep-version").getTextContent();
			
			def removeVersion = utils.xpathNode(docApp2WAR, "/project/properties/remove-version").getTextContent();
			def dep2App2War = utils.xpathNode(docApp2EAR, "/project/dependencies/dependency/version").getTextContent();

			assertEquals("1.0.1.0-SNAPSHOT",mainVersion);
			assertEquals("1.0.1.0-SNAPSHOT",lib1Version);
			assertEquals("1.0.0.0",depVersion);
			assertEquals("2.0.0.0-SNAPSHOT",masDepVersion);
			
			assertEquals("1.0.1.0-SNAPSHOT",removeVersion);
			assertEquals("1.0.1.0-SNAPSHOT",dep2App2War);
		}
	}


	/**
	 * Descomprime un zip en la ruta especificada.
	 * @param zipPath
	 * @param destination
	 * @return
	 */
	private void unzipTestsZip(zipPath = "src/test/resources/versioner/PruebaRelease-App-2.zip",
			destination = "src/test/resources/versioner") {
		def ant = new AntBuilder();
		ant.unzip(  src: zipPath,
		dest: destination,
		overwrite:"true" )
	}


}
