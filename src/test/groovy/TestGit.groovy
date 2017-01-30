import org.junit.Test

import es.eci.utils.GitBuildFileHelper
import es.eci.utils.TmpDir;

class TestGit {

	//@Test
	public void testGitBuildTree() {
		TmpDir.tmp {
			def components = [ "PruebaRelease-Biblioteca-2",
				"PruebaRelease-App-1",
				"PruebaRelease-App-2",
				"PruebaRelease-Biblioteca-1"];

			String technology = "maven";
			String gitHost = "mx00000018d0323.eci.geci";
			String gitGroup = "GrupoPruebaRelease";
			String branch = "DESARROLLO";
			String maven = "mvn";

			File baseDirectory = new File("C:/jenkins/workspace/test2");

			GitBuildFileHelper fileHelper = new GitBuildFileHelper("build", baseDirectory);
			fileHelper.initLogger{ println it };
			components.each { component ->
				fileHelper.createBuildFileStructure(baseDirectory, component, technology, gitHost, gitGroup, branch);
			}

			fileHelper.createStreamReactor(baseDirectory, technology, maven, components);
		}
	}

}
