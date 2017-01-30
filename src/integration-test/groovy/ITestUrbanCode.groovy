import org.junit.Assert
import org.junit.Before
import org.junit.Test

import urbanCode.UrbanCodeComponentInfoService
import urbanCode.UrbanCodeExecutor
import es.eci.utils.pom.MavenCoordinates

class ITestUrbanCode {

	private String urbanCodeCommand = "";
	private String urbanCodeURL = "";
	private String user = "";
	private String password = "";
	
	@Before
	public void setup() {
		// Las propiedades deberán haberse inyectado desde Jenkins, o por
		//	línea de comandos con -DuserRTC=XXXX -DurlRTC=XXXX, etc.
		urbanCodeCommand = System.getProperty("UDCLIENT_COMMAND");
		user = System.getProperty("UDCLIENT_USER_PRE");
		password = System.getProperty("UDCLIENT_PASS_PRE");
		urbanCodeURL = System.getProperty("UDCLIENT_URL_PRE")
	}
	
	@Test
	public void testMavenCoordinates() {
		
		UrbanCodeExecutor exec = new UrbanCodeExecutor(urbanCodeCommand, urbanCodeURL, user, password);
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
}
