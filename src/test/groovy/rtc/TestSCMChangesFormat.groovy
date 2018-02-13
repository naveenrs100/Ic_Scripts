package rtc
import java.text.SimpleDateFormat

import org.junit.Assert
import org.junit.Test

import es.eci.utils.mail.PrettyFormatter

class TestSCMChangesFormat {

	
	@Test
	public void testNumberFormat() {
		PrettyFormatter pretty = new PrettyFormatter();
		
		Assert.assertEquals("1803", pretty.formatInteger(1803));
		Assert.assertEquals("1803", pretty.formatInteger(1803.0f));
		Assert.assertEquals("1803", pretty.formatInteger(1803.0d));
		Assert.assertEquals("1803", pretty.formatInteger("1803.0"));
	}
	
	@Test
	public void testDate() {
		PrettyFormatter pretty = new PrettyFormatter();
		
		Date now = new Date();
		
		Assert.assertEquals(
			PrettyFormatter.SCM_CHANGE_DATE_FORMAT.format(now), 
			pretty.formatDate(now))
		// No se puede ser más preciso con la presentación debido a la influencia del
		//	timezone local
		// 2017-12-19-16:13:02
		Assert.assertTrue( 
			pretty.formatDate("2017-12-19-16:13:02").startsWith("19/12/2017 - "));
		// Tue Dec 19 15:19:28 2017 +0100
		Assert.assertTrue( 
			pretty.formatDate("Tue Dec 19 15:19:28 2017 +0100").startsWith("19/12/2017 - "));
	}
}
