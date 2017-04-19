import org.junit.Assert
import org.junit.Before
import org.junit.Test

import urbanCode.UrbanCodeFichaDespliegue

class ITestUrbanCodeFichaDespliegue {

	private String udClientCommand = "";
	private String urlUrbanCode = "";
	private String urbanUser = "";
	private String urbanPassword = "";
	private String urlNexus = "";
	
	@Before
	public void setup() {
		// Las propiedades deberán haberse inyectado desde Jenkins, o por
		// línea de comandos con -DudClientCommand=XXXX -DurlUrbanCode=XXXX, etc.
		udClientCommand = System.getProperty("UDCLIENT_COMMAND");
		urlUrbanCode = System.getProperty("UDCLIENT_URL_PRE");
		urbanUser = System.getProperty("UDCLIENT_USER_PRE");
		urbanPassword = System.getProperty("UDCLIENT_PASS_PRE");
		urlNexus = System.getProperty("NEXUS_FICHAS_DESPLIEGUE_URL");
	}
	
	@Test
	public void testUrbanCodeFichaDespliegueConstruccion() {
		
		UrbanCodeFichaDespliegue exec = new UrbanCodeFichaDespliegue();
		exec.initLogger { println it }
		
		exec.setDescriptor('{"name":"Pruebens_two8","application":"88888 - Prueba GATES","description":"Snapshot Urban Code","versions":[{"PruebaRelease - App 1":"1.0.42.0"},{"PruebaRelease - App 2":"1.0.42.0"},{"PruebaRelease - App 2.doc":"1.0.43.0"}]}')
		
		exec.setUdClientCommand(udClientCommand)
		exec.setUrlUrbanCode(urlUrbanCode)
		exec.setUrbanUser(urbanUser)
		exec.setUrbanPassword(urbanPassword)
		exec.setUrlNexus(urlNexus)
		
		exec.execute()

	}
	
	@Test
	public void testUrbanCodeFichaDesplieguePeticion() {
		
		UrbanCodeFichaDespliegue exec = new UrbanCodeFichaDespliegue();
		exec.initLogger { println it }
		
		exec.setNombreAplicacionUrban("API_Super")
		exec.setInstantaneaUrban("pruebas_15022017")
		
		exec.setUdClientCommand(udClientCommand)
		exec.setUrlUrbanCode(urlUrbanCode)
		exec.setUrbanUser(urbanUser)
		exec.setUrbanPassword(urbanPassword)
		exec.setUrlNexus(urlNexus)
		
		exec.execute()

	}

}
