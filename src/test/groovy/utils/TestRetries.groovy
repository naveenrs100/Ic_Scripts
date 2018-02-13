package utils
import org.junit.Assert
import org.junit.Test

import es.eci.utils.Retries

class TestRetries {

	
	@Test
	public void testRetriesTrivial1() {
		Assert.assertEquals(2,  Retries.retry(1, 1000, { 1 + 1 }))
	}
	
	@Test
	public void testRetriesTrivial2() {
		Assert.assertEquals("lalalala",  Retries.retry(1, 1000, { "lalal" + "ala" }))
	}
}
