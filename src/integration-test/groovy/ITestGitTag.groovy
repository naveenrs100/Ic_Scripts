import git.GitUtils

import org.junit.Assert
import org.junit.Test

class ITestGitTag extends BaseTest {

	
	@Test
	public void testLastRepositoryTag() {
		
		// La última tag debería ser 1.0.0.0
		GitUtils utils = new GitUtils("git", gitHost, gitCommand);
		utils.initLogger { println it }
		
		Assert.assertEquals("1.0.0.0", 
			utils.getRepositoryLastTag("JuegoDePruebas", "RepositorioPruebasTag"));
	}
	
}
