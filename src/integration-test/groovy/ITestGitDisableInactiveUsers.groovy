import org.junit.Test

import git.commands.GitDisableInactiveUsersCommand;

class ITestGitDisableInactiveUsers extends BaseTest {

	@Test
	public void testUsers() {
		// Listado de usuarios en pre
		
		GitDisableInactiveUsersCommand command = 
			new GitDisableInactiveUsersCommand();
		
		command.setGitCommand(gitCommand);
		command.setKeystoreVersion(gitlabKeystoreVersion);
		command.setPrivateGitLabToken(gitlabPrivateToken);
		command.setUrlGitlab(gitURL);
		command.setUrlNexus(nexusURL);
		command.setDryRun(true);
		command.setDays(30l)
		
		command.initLogger { println it }
		
		command.execute();
	}
}
