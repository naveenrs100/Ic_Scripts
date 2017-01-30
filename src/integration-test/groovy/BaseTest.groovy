import org.junit.Before

import rtc.commands.AbstractRTCCommand
import rtc.commands.RTCCheckinCommand
import rtc.commands.RTCDownloaderCommand


class BaseTest {

	// Propiedades de RTC
	protected String user = null;
	protected String password = null;
	protected String url = null;
	protected String scmToolsHome = null;
	protected String workItem = null;
	
	// Propiedades de git
	protected String gitCommand = null;
	protected String gitHost = null;
	protected String gitlabKeystoreVersion = null;
	protected String gitUser = null;
	protected String gitlabPrivateToken = null;
	protected String gitURL = null;
	
	// Propiedades de LDAP
	protected String ldapHost = null;
	protected String ldapUser = null;
	protected String ldapPassword = null;
	
	// Propiedades de Nexus
	protected String nexusURL = null;
	
	
	// Setup con los valores iniciales
	protected void init(AbstractRTCCommand command) {
		// Logger más sencillo posible
		command.initLogger { println it }
		
		command.setUrlRTC(url);
		command.setUserRTC(user);
		command.setPwdRTC(password);
		command.setScmToolsHome(scmToolsHome);
	}
	
	protected void download(String stream, String component, 
							String workspace, File directory, String baseline = null) {
		// Bajar el componente
		RTCDownloaderCommand checkOut = new RTCDownloaderCommand();
		init(checkOut);
		
		if (stream != null) {
			checkOut.setStream(stream);
		}
		if (baseline != null) {
			checkOut.setBaseline(baseline);
		}
		checkOut.setWorkspaceRTC(workspace);
		checkOut.setComponent(component);
		checkOut.setParentWorkspace(directory);
		
		checkOut.execute();
	}
							
	protected void changeFileAndCheckinChanges(
			String workItem, String workspaceName, File directory, String fragment) {
		File changedFile = new File(directory, "pom.xml");
		changedFile.text += fragment;
		RTCCheckinCommand checkIn = new RTCCheckinCommand();
		init(checkIn);
		checkIn.setWorkspaceRTC(workspaceName);
		checkIn.setDescription("Test change");
		checkIn.setWorkItem(workItem);
		checkIn.setParentWorkspace(directory);
		
		checkIn.execute();
	}
	
	def validate(String s) {
		return s != null && s.trim().length() > 0;
	}
			
	@Before
	public void setup() {		
		// Las propiedades deberán haberse inyectado desde Jenkins, o por
		//	línea de comandos con -DuserRTC=XXXX -DurlRTC=XXXX, etc.
		
		// Propiedades de RTC
		user = System.getProperty("userRTC");
		url = System.getProperty("urlRTC");
		password = System.getProperty("pwdRTC");
		scmToolsHome = System.getProperty("SCMTOOLS_HOME");
		workItem = System.getProperty("workItem");
		
		// Propiedades de git
		if (validate(System.getProperty("GIT_SH_COMMAND"))) {
			gitCommand = System.getProperty("GIT_SH_COMMAND");
		}
		else {
			gitCommand = "git";
		}
		gitHost = System.getProperty("GIT_HOST");
		gitlabKeystoreVersion = System.getProperty("GITLAB_KEYSTORE_VERSION");
		gitUser = System.getProperty("userGit");
		gitlabPrivateToken = System.getProperty("GITLAB_PRIVATE_TOKEN");
		gitURL = System.getProperty("GITLAB_URL");
		
		// Propiedades de LDAP
		ldapHost = System.getProperty("LDAP_URL");
		ldapUser = System.getProperty("LDAP_USER");
		ldapPassword = System.getProperty("LDAP_PASSWORD");
		
		// Propiedades de Nexus
		nexusURL = System.getProperty("NEXUS_PUBLIC_URL");
	}
}
