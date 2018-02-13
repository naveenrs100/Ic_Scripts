package rtc
import org.junit.Assert
import org.junit.Test

import rtc.ProjectAreasMap

class TestBeautifyProjectArea {

	
	@Test
	public void testBeautify() {
		
		Assert.assertEquals("Gestion de Pedidos, Envios y Repartos ( GPER )", 
			ProjectAreasMap.beautify("Gestión de Pedidos, Envíos y Repartos ( GPER )"))
		
		Assert.assertEquals("SSP (Service Store Provider)", 
			ProjectAreasMap.beautify("CC.CC. - SSP (Service Store Provider) (RTC)"))
		
		Assert.assertEquals("Plataforma de Desarrollo y Calidad", 
			ProjectAreasMap.beautify("GIS - Plataforma de Desarrollo y Calidad (RTC)"))
		
		Assert.assertEquals("QUVE", 
			ProjectAreasMap.beautify("GIS - QUVE (RTC)"))
		
		Assert.assertEquals("un producto - sufijo", 
			ProjectAreasMap.beautify("prefijo - un producto - sufijo"))
		
		Assert.assertEquals("un producto - sufijo", 
			ProjectAreasMap.beautify("prefijo - un producto - sufijo (RTC)"))
		
		Assert.assertEquals("un producto - (sufijo)", 
			ProjectAreasMap.beautify("prefijo - un producto - (sufijo)"))
		
	}
}
