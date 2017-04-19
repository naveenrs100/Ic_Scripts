import org.junit.Assert
import org.junit.Test

import rtc.ProjectAreaCacheReader

class TestProjectAreaCache {
	
	@Test
	public void testCache() {
		// Leer del fichero areas.xml en src/test/resources
		ProjectAreaCacheReader reader = 
			new ProjectAreaCacheReader(
				ProjectAreaCacheReader.class.getClassLoader().
					getResourceAsStream("areas.xml"));
		reader.initLogger { println it }
		Assert.assertEquals("Nominas",
			reader.getProjectArea("Personal - Nominas - RELEASE"));
		Assert.assertEquals("SSP-Servicios de Tienda",
			reader.getProjectArea("CCCC-SSP_105_01V_C-RELEASE"));
	}
}
