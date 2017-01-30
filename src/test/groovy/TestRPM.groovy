import org.junit.Test

import rpm.RPMGruntPackageHelper;
import rpm.RPMPackageHelper
import es.eci.utils.TmpDir

class TestRPM {

	
	//@Test
	public void testRPM() {
		TmpDir.tmp { File dir ->
			
			File tmp = new File(dir, "hola.txt");
			tmp.text = "lalalalalalaala";
			
			RPMPackageHelper helper = 
				RPMPackageHelper.builder().
					setInstallationDirectory("/lalala/dir").
					setSourceDirectory(dir).build({ println it });
			
			helper.createPackage();
		}
	}
	
	//@Test
	public void testGruntRPM() {
		TmpDir.tmp { File sourceDir ->
			File test = new File(sourceDir, "test.txt");
			test.text = "lalalalalalalala";
			TmpDir.tmp { File dir ->
				RPMGruntPackageHelper helper =
					RPMGruntPackageHelper.builder().
						setSourceDirectory(sourceDir).
						setDestDirectory(new File("/home/jorge/temp/20160802")).
						setInstallationDirectory("/nodejs/applications/lalala").
						setPackageName("mitarjeta").
						setConfigGroupId("rpm.nodejs").
						setConfigArtifactId("empaquetado").
						setConfigVersion("1.0.0.0").
						setArch("x86_64").
						setNexusURL("http://nexus.elcorteingles.pre/content/groups/public/").
							build({println it});
				helper.createPackage();
			}
		}
	}
}
