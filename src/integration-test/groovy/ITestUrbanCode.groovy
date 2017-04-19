import org.junit.Assert
import org.junit.Before
import org.junit.Test

import urbanCode.UrbanCodeComponentInfoService
import urbanCode.UrbanCodeExecutor
import urbanCode.UrbanCodeSnapshot
import es.eci.utils.pom.MavenCoordinates

class ITestUrbanCode extends BaseTest {

	
	@Test
	public void testMavenCoordinates() {
		
		UrbanCodeExecutor exec = new UrbanCodeExecutor(
			urbanCodeCommand, urbanCodeURL, urbanCodeUser, urbanCodePassword);
		exec.initLogger { println it }
		UrbanCodeComponentInfoService service = 
			new UrbanCodeComponentInfoService(exec);
		service.initLogger { println it }
		
		MavenCoordinates coords = service.getCoordinates("PruebaRelease - App 1");
		// Grupo, artefacto y packaging
		Assert.assertNotNull(coords);
		Assert.assertEquals("es.eci.release.prueba", coords.getGroupId());
		Assert.assertEquals("App1-EAR", coords.getArtifactId());
		Assert.assertEquals("ear", coords.packaging);
	}
	
	@Test
	public void testMavenCoordinatesTemplate() {
		
		UrbanCodeExecutor exec = new UrbanCodeExecutor(
			urbanCodeCommand, urbanCodeURL, urbanCodeUser, urbanCodePassword);
		exec.initLogger { println it }
		UrbanCodeComponentInfoService service =
			new UrbanCodeComponentInfoService(exec);
		service.initLogger { println it }
		
		MavenCoordinates coords = service.getCoordinates("QSP-Integraciones-CFG");
		// Grupo, artefacto y packaging
		Assert.assertNotNull(coords);
		Assert.assertEquals("es.elcorteingles.ad.food.integraciones", coords.getGroupId());
		Assert.assertEquals("integraciones-cfg", coords.getArtifactId());
		Assert.assertEquals("zip", coords.packaging);
	}
	
	@Test
	public void testDownloadSnapshot() {
		UrbanCodeExecutor exec = new UrbanCodeExecutor(
			urbanCodeCommand, urbanCodeURL, user, password);
		exec.initLogger { println it }
		UrbanCodeSnapshot snapshot = exec.downloadSnapshot(
				"es.eci.fichas_urbancode",
				"API_Super",
				"PRE_-_Enlace_RTC_Jenkins_y_Nexus-Supermercado2016-HOTFIX-11.0.1-Supermercado2016_20170124095637",
				nexusURL
			);
		Assert.assertNotNull(snapshot);
		List<Map<String, String>> versions = snapshot.getVersions();
		Assert.assertEquals('1.0.27.0-4', versions.find { it.keySet().contains('QSP-EndecaApp') }['QSP-EndecaApp'])
	}
}
