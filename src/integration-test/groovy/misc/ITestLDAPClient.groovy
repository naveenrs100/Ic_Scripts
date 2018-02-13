package misc
import base.BaseTest;
import ldap.LDAPClient

import org.junit.Before
import org.junit.Test

class ITestLDAPClient extends BaseTest {

	private LDAPClient client = null;
	
	@Before
	public void initializeClient() {
		client = new LDAPClient(
				ldapHost, 
				ldapUser, 
				ldapPassword);
	}
	
	@Test
	public void testByName() {
		System.out.println(
			client.getByUserName("JENKINS_RTC"));
	}
	
	@Test
	public void testByGroupName() {
		client.getByGroupName("CN=GitAdmins,CN=Users,DC=grupoeci,DC=elcorteingles,DC=corp").each {
			println it
		}
	}
}
