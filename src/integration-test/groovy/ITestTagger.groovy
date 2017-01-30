import org.junit.Assert
import org.junit.Test

import rtc.commands.RTCTaggerCommand
import es.eci.utils.ComponentVersionHelper
import es.eci.utils.TmpDir

class ITestTagger extends BaseTest {

	
	@Test
	public void testBaseline() {
		TmpDir.tmp { File tempDir ->
			RTCTaggerCommand tagger = new RTCTaggerCommand();
			
			init(tagger);
			
			// Corriente: TESTSTREAM
			tagger.setWorkspaceRTC("WSR - TESTTAGGER");
			long timestamp = new Date().getTime();
			String baselineName = "baseline" + timestamp;
			
			tagger.setStream("aaa");
			tagger.setTagType("baseline");
			tagger.setComponent("SimpleComponent1");
			
			File versionTXT = new File(tempDir, "version.txt");
			versionTXT.createNewFile();
			versionTXT.text = "version=$baselineName"
			
			tagger.setVersion("local");
			tagger.setParentWorkspace(tempDir);
			
			tagger.execute();
			
			// Verificar que ha salido bien
			ComponentVersionHelper helper = 
				new ComponentVersionHelper(scmToolsHome);
			Assert.assertEquals(baselineName, 
				helper.getVersion("SimpleComponent1", 
					"WSR - TESTTAGGER", user, password, url));
		}
	}
	
	@Test
	public void testSnapshot() {
		TmpDir.tmp { File tempDir ->
			RTCTaggerCommand tagger = new RTCTaggerCommand();
			
			ComponentVersionHelper helper =
				new ComponentVersionHelper(scmToolsHome);
			List<String> initialSnapshots = 
				helper.getSnapshots("SNAPSHOTS_TESTSTREAM", user, password, url);
			
			init(tagger);
			
			long timestamp = new Date().getTime();
			tagger.setStream("SNAPSHOTS_TESTSTREAM");
			
			String snapshotName = "snapshot " + timestamp;
			String snapshotDescription = "description of snapshot " + timestamp;
			
			tagger.setTagType("snapshot");
			tagger.setInstantanea(snapshotName);
			tagger.setDescription(snapshotDescription);
			tagger.setParentWorkspace(tempDir);
			tagger.setVersion("local");
			
			tagger.execute();
			
			List<String> expected = new LinkedList<String>();
			expected.add(snapshotName);
			expected.addAll(initialSnapshots);
			
			List<String> finalSnapshots = 
				helper.getSnapshots("SNAPSHOTS_TESTSTREAM", user, password, url);
				
			// RTC nos devuelve un máximo de resultados, de forma que la lista 'expected',
			//	compuesta a mano, puede tener un resultado más que la obtenida de RTC. 
			// Por lo tanto se le exige a la comparación limitarse a los registros
			//	devueltos por RTC.
			int max = finalSnapshots.size();
			Assert.assertEquals(expected.subList(0, max), finalSnapshots.subList(0, max));
		}
	}
}
