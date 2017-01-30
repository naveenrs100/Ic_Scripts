import org.junit.Assert;
import org.junit.Before;
import org.junit.Test

import es.eci.utils.NexusHelper
import es.eci.utils.TmpDir
import es.eci.utils.ZipHelper
import es.eci.utils.pom.MavenCoordinates

class ITestNexus {

	private String mavenHome = null;
	
	@Before
	public void setup() {
		// Las propiedades deberán haberse inyectado desde Jenkins, o por
		//	línea de comandos con -DuserRTC=XXXX -DurlRTC=XXXX, etc.
		mavenHome = System.getProperty("MAVEN_HOME");
	}
	
	//@Test
//	public void testSnapshotResolution() {
//		String urlNexus = "http://nexus.elcorteingles.pre";
//		String urlNexusRepoSnapshots = "${urlNexus}/content/repositories/eci-snapshots/"
//		MavenCoordinates coords = new MavenCoordinates("grupo.prueba", "artefacto.prueba", "1.0-SNAPSHOT");
//		coords.setPackaging("jar");
//		
//		// Componer un jar y subirlo a nexus
//		TmpDir.tmp { File dir ->
//			File content = new File(dir, "content");
//			content.mkdirs();
//			
//			// Construir algo en contents
//			File txt = new File(content, "file.txt");
//			txt.text = "contenido de prueba";
//			
//			File artifact = ZipHelper.addDirToArchive(content);
//			String mavenExecutable = mavenHome + "/bin/mvn";
//			if (System.getProperty('os.name').toLowerCase().contains('windows')) {
//				mavenExecutable += ".bat";
//			}
//			NexusHelper.uploadToNexus(
//				// Ejecutable de maven
//				mavenExecutable, 
//				// Coordenadas
//				coords.getGroupId(), 
//				coords.getArtifactId(), 
//				coords.getVersion(),
//				artifact.getCanonicalPath(),
//				urlNexusRepoSnapshots,
//				// Coordenadas 
//				coords.getPackaging(),
//				// Logger
//				{println it});
//		}
//		// Leer el timestamp devuelto por nexus para ese jar
//		NexusHelper helper = new NexusHelper(urlNexus);
//		helper.initLogger { println it }
//		String timestamp = helper.resolveSnapshot(coords, "public");
//		Assert.assertNotNull(timestamp);
//	}
}
