import es.eci.utils.TmpDir
import git.commands.GitSetComponentsFromGroupCommand

import org.junit.Assert;
import org.junit.Test

class ITestGitEmptyRepos extends BaseTest {

	
	@Test
	public void testEmptyRepo() {
		
		
		TmpDir.tmp { File dir ->
			
			GitSetComponentsFromGroupCommand command = 
				new GitSetComponentsFromGroupCommand();
				
			command.setAction("build");
			command.setBranch("master");
			command.setGetOrdered("true");
			command.setGitCommand(gitCommand);
			command.setGitGroup("GrupoPruebaReposVacios");
			command.setGitHost(gitHost);
			command.setKeystoreVersion(gitlabKeystoreVersion);
			command.setLastUserIC(gitUser);
			command.setOnlyChanges("true");
			command.setParentWorkspace(dir.getCanonicalPath());
			command.setPrivateGitLabToken(gitlabPrivateToken);
			command.setUrlGitlab(gitURL);
			command.setUrlNexus(nexusURL);
			command.setTechnology("maven");
			
			command.initLogger { println it }
			

			command.execute();
			
			// Verificar que el jenkinsComponents.txt se ha construido bien
			File f = new File(dir, "jenkinsComponentsJobs.txt");
			Assert.assertTrue(f.exists());
			Assert.assertEquals('[["GrupoPruebaReposVacios -COMP- RepoLleno"]]', f.text)
		}
	}
	
}
