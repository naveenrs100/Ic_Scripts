import java.nio.charset.Charset

import org.junit.Assert
import org.junit.Test

import ppm.PPMProductParser
import ppm.PPMProductsCollection

class TestPPMParser {

	@Test
	public void testParser() {
		InputStream is = 
			TestPPMParser.class.getClassLoader().
				getResourceAsStream("ppm/productos_activos.txt");
		Reader reader = new InputStreamReader(is, Charset.forName("iso-8859-1"));
		PPMProductsCollection products = 
			new PPMProductParser().parse(
				reader.readLines().join(System.getProperty("line.separator")));
		// Buscar algunos concretos
		Assert.assertNotNull(products.findByName ("Analítica de Venta"));
		Assert.assertNotNull(products.findByName ("Analitica de Venta"));
		Assert.assertNotNull(products.findByName ("Gestión Centros Oportunidades"));
		Assert.assertNotNull(products.findByName ("Gestion Centros Oportunidades"));
		Assert.assertNull(products.findByName ("asdf asdf asdf") );
	}
}
