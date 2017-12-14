import java.security.DigestInputStream
import java.security.MessageDigest

import org.junit.Assert
import org.junit.Test

import es.eci.utils.MultipartUtility
import es.eci.utils.NexusHelper
import es.eci.utils.TmpDir
import es.eci.utils.ZipHelper
import es.eci.utils.pom.MavenCoordinates

class ITestNexus extends BaseTest {

	
	private checkDownload(MavenCoordinates coords, String md5, String repo = null) {
		TmpDir.tmp { File dir ->
			NexusHelper helper = new NexusHelper(repo==null?nexusURL:repo);
			helper.initLogger { println it }
			helper.setNexus_user(nexusUser);
			helper.setNexus_pass(nexusPwd);
			File f = helper.download(coords, dir);
			Assert.assertNotNull(f);
			Assert.assertTrue(f.exists());
			// El primer zip es el que necesitamos
			InputStream is = null;
			try {
				is = new FileInputStream(f); 
				DigestInputStream dis = new DigestInputStream(is, MessageDigest.getInstance('MD5'));
				dis.eachByte { };
				Assert.assertEquals(md5,
					dis.getMessageDigest().digest().encodeHex().toString())
			}
			catch (Throwable t) {
				throw t;
			}
			finally {
				is.close();
			}
		}
	}
	
	@Test
	public void testNexusDownload() {
		MavenCoordinates coords = 
			new MavenCoordinates(
				"es.eci.pruebas", 
				"notClassifiedArtifact", 
				"1.0.0.1");
		coords.setPackaging("zip");
		checkDownload(coords, "3c522317e3b1c5bf15d9d831dca11ef2");
	}
	
	@Test
	public void testNexusDownloadWithClassifier() {
		MavenCoordinates coords = 
			new MavenCoordinates(
				"es.eci.pruebas", 
				"classifiedArtifact", 
				"1.0.0.1");
		coords.setPackaging("zip");
		coords.setClassifier("myclassifier")
		checkDownload(coords, "b55f6a285da6dd464757e8cd49da488f");
	}
	
	@Test
	public void testNexusUploadWithClassifier() {
		TmpDir.tmp { File dir ->
			File data = new File(dir, "data");
			data.mkdirs();
			
			File dataFile = new File(data, "textFile.txt");
			dataFile.createNewFile();
			dataFile.text = "askldjasldfkjasdlfksdf"
			
			File zipFile = ZipHelper.addDirToArchive(data);
			
			MavenCoordinates coords = 
				new MavenCoordinates('grupo.pruebas.wso2', 'prueba-wso', '0.0.1');
			coords.setClassifier("api")
			coords.setPackaging("zip")
			coords.setRepository("eci");
			
			def helper = new NexusHelper(nexusURL)
			helper.setNexus_user(nexusUser)
			helper.setNexus_pass(nexusPwd)
			helper.upload(coords, zipFile)
			
			zipFile.delete();
		}
	}
	
	@Test
	public void testPrivateNexusDownload() {
		MavenCoordinates coords = 
			new MavenCoordinates(
				"es.eci.pruebas", 
				"privateNoClassifierArtifact", 
				"1.2.3.4");
		coords.setPackaging("zip");
		coords.setRepository("private");
		checkDownload(coords, "8353550df86b97831ff2c6bb1f7190a9");
	}
	
	@Test
	public void testPrivateWithClassifierNexusDownload() {
		MavenCoordinates coords = 
			new MavenCoordinates(
				"es.eci.pruebas", 
				"privateClassifierArtifact", 
				"1.2.3.4");
		coords.setPackaging("zip");
		coords.setClassifier("anotherclassifier");
		coords.setRepository("private");
		checkDownload(coords, "579547cd23db1cfbe316937be4799ba4");
	}
	
	@Test
	public void testNexusDownloadAltRepo() {
		MavenCoordinates coords = 
			new MavenCoordinates(
				"es.eci.pruebas", 
				"notClassifiedArtifact", 
				"1.0.0.1");
		coords.setPackaging("zip");
		checkDownload(coords, "3c522317e3b1c5bf15d9d831dca11ef2",
			"http://nexus.elcorteingles.pre/content/repositories/fichas_despliegue/");
	}	
}
