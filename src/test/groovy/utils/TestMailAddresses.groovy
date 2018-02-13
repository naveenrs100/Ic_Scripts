package utils
import org.junit.Assert
import org.junit.Test

import es.eci.utils.mail.MailAddressParser

class TestMailAddresses {

	private final List<String> ADDRESSES = [
		'dsanchezr@viewnext.com',
		'macambronero@viewnext.com',
		'jiacedo@viewnext.com',
		'agotor@viewnext.com',
		'jamacia@viewnext.com',
		'publioluis_perez@elcorteingles.es',
		'josem_holgueras@elcorteingles.es',
		'franciscoj_peregrin@elcorteingles.es',
		'macambronero@viewnext.com',
		'jiacedo@viewnext.com',
		'agotor@viewnext.com',
		'jamacia@viewnext.com'
	];
	
	@Test
	public void testMailAddressesNormal() {
		String managersMail = "dsanchezr@viewnext.com,macambronero@viewnext.com,jiacedo@viewnext.com,agotor@viewnext.com,jamacia@viewnext.com,publioluis_perez@elcorteingles.es,josem_holgueras@elcorteingles.es,franciscoj_peregrin@elcorteingles.es,macambronero@viewnext.com,jiacedo@viewnext.com,agotor@viewnext.com,jamacia@viewnext.com"
		List<String> results = MailAddressParser.parseReceivers(managersMail);
		Assert.assertEquals(ADDRESSES, results);
	}
	
	@Test
	public void testMailAddressesSpaces() {
		String managersMail = "dsanchezr@viewnext.com,macambronero@viewnext.com,jiacedo@viewnext.com,agotor@viewnext.com,jamacia@viewnext.com,publioluis_perez@elcorteingles.es,josem_holgueras@elcorteingles.es,franciscoj_peregrin@elcorteingles.es macambronero@viewnext.com, jiacedo@viewnext.com agotor@viewnext.com, jamacia@viewnext.com"
		List<String> results = MailAddressParser.parseReceivers(managersMail);
		Assert.assertEquals(ADDRESSES, results);
	}
}
