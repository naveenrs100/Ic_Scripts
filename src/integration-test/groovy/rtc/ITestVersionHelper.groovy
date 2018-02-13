package rtc
import base.BaseTest;

import org.junit.Assert
import org.junit.Test

import es.eci.utils.ComponentVersionHelper
import es.eci.utils.TmpDir

class ITestVersionHelper extends BaseTest {
	
	@Test
	public void testLastBaseline() {
		TmpDir.tmp { File daemonConfigDir ->
			ComponentVersionHelper helper =
				new ComponentVersionHelper(scmToolsHome);
			helper.initLogger { println it }
				
			Assert.assertEquals('test_baseline_3', 
				helper.getVersion("SimpleComponent1", "SIMPLE_TESTSTREAM", user, password, url));
		}
	}
	
	@Test
	public void testSnapshots() {
		
		TmpDir.tmp { File daemonConfigDir ->
			ComponentVersionHelper helper =
				new ComponentVersionHelper(scmToolsHome);
			helper.initLogger { println it }
			
			List<String> snapshots = helper.getSnapshots("SIMPLE_TESTSTREAM", user, password, url);
			
			println snapshots
			
			// En el juego de datos original hay dos snapshots
			List<String> expected = [ "TestSnapshot2", "TestSnapshot1"]
			Assert.assertEquals(expected, snapshots);
		}
	}
}
