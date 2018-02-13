package maven
import org.junit.Assert;
import org.junit.Test

import es.eci.utils.TmpDir
import es.eci.utils.ZipHelper
import es.eci.utils.VersionUtils


class TestPomTree {

	@Test
	public void testApp2Tree() {
		TmpDir.tmp { File dir ->
			ZipHelper.unzipInputStream(
				TestReactor.class.
					getClassLoader().
						getResourceAsStream("pomTree/PruebaRelease-App-2.zip"),
				dir
				);
			// Obtener el valor de main-version en cada pom			
			Assert.assertEquals("110.0.0.0-SNAPSHOT",
				new VersionUtils().solveRecursive(dir,
					new XmlParser().parse(new File(dir, "pom.xml")),
					'${main-version}'))
			
			Assert.assertEquals("110.0.0.0-SNAPSHOT", 
				new VersionUtils().solveRecursive(dir, 
					new XmlParser().parse(new File(dir, "App2-EAR/pom.xml")), 
					'${main-version}'))
			
			Assert.assertEquals("110.0.0.0-SNAPSHOT",
				new VersionUtils().solveRecursive(dir,
					new XmlParser().parse(new File(dir, "App2-WAR/pom.xml")),
					'${main-version}'))
		}
	}
}
