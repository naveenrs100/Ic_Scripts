package rtc
import base.BaseTest;;

import org.junit.Assert
import org.junit.Test

import rtc.commands.RTCDeleteRepositoryWorkspace
import rtc.commands.RTCReplaceCommand
import rtc.commands.RTCTaggerCommand
import es.eci.utils.TmpDir

class ITestReplaceBaseline extends BaseTest {
	
	private final static String STREAM_NAME = "CHECKIN_AND_DELIVER_STREAM"
	private final static String COMPONENT_NAME = "SimpleComponent3"

	@Test
	public void simpleReplaceBaseline() {
		// 1.- Crear un WSR temporal con el SimpleComponent1
		// 2.- Bajarlo a local, hacer un cambio (cambio1)
		// 3.- Checkin
		// 4.- Hacer baseline BL1 sobre el WSR
		// 5.- Hacer otro cambio (cambio2)
		// 6.- Checkin
		// 7.- Comprobar que cambio1 y cambio2 han subido al WSR
		// 8.- Reemplazar el componente en el WSR temporal por BL1
		// 9.- Bajar el WSR a local en un directorio nuevo
		// 10.- Comprobar que solo está cambio1
		
		long timestamp = new Date().getTime();
		final String WORKSPACE_NAME = "WSR - " + STREAM_NAME + timestamp; 
		final String FRAGMENT1 = String.format("<!-- Change 1 :: %s -->", timestamp);
		final String FRAGMENT2 = String.format("<!-- Change 2 :: %s -->", timestamp);
		final String BASELINE = "Baseline " + timestamp;
		
		try {
			TmpDir.tmp { File dir ->
				// 1.- Crear un WSR temporal con el SimpleComponent1
				// 2.- Bajarlo a local, hacer un cambio (cambio1)
				// 3.- Checkin
				download(STREAM_NAME, COMPONENT_NAME, WORKSPACE_NAME, dir);
				changeFileAndCheckinChanges(workItem, WORKSPACE_NAME, dir, FRAGMENT1);
				
				// 4.- Hacer baseline BL1 sobre el WSR
				RTCTaggerCommand baseline = new RTCTaggerCommand();
				init(baseline);
				
				baseline.setStream("aaa");
				baseline.setTagType("baseline");
				baseline.setWorkspaceRTC(WORKSPACE_NAME);
				baseline.setComponent(COMPONENT_NAME);
				
				// Funcionalidad del comando RTCTagger:
				// Version=local y fichero version.txt existente, hace la
				//	línea base con el contenido del version.txt
				File versionTxt = new File(dir, "version.txt");
				versionTxt.createNewFile();
				versionTxt.text = "version=$BASELINE";
				
				baseline.setVersion("local");
				baseline.setParentWorkspace(dir);
				
				baseline.execute();
				
				// 5.- Hacer otro cambio (cambio2)
				// 6.- Checkin
				changeFileAndCheckinChanges(workItem, WORKSPACE_NAME, dir, FRAGMENT2);
				
			}
			
			TmpDir.tmp { File dir2 ->
				// 7.- Comprobar que cambio1 y cambio2 han subido al WSR
				download(null, COMPONENT_NAME, WORKSPACE_NAME, dir2);
				File pom = new File(dir2, "pom.xml");
				Assert.assertTrue(pom.text.contains(FRAGMENT1));
				Assert.assertTrue(pom.text.contains(FRAGMENT2));
			}
			
			TmpDir.tmp { File dirReplace ->
				// 8.- Reemplazar el componente en el WSR temporal por BL1
				RTCReplaceCommand replace = new RTCReplaceCommand();
				init(replace);
				// A RTC le es indiferente corriente o WSR de repositorio, en tanto
				//	que ambos son ramas
				replace.setStream(WORKSPACE_NAME);
				replace.setComponent(COMPONENT_NAME);
				replace.setBaseline(BASELINE);
				replace.setParentWorkspace(dirReplace);
				replace.execute();
			}
			
			// 9.- Bajar el WSR a local en un directorio nuevo
			// 10.- Comprobar que solo está cambio1
			TmpDir.tmp { File dir3 ->
				// 7.- Comprobar que cambio1 y cambio2 han subido al WSR
				download(null, COMPONENT_NAME, WORKSPACE_NAME, dir3);
				File pom = new File(dir3, "pom.xml");
				Assert.assertTrue(pom.text.contains(FRAGMENT1));
				Assert.assertFalse(pom.text.contains(FRAGMENT2));
			}
			
		}
		finally {
			RTCDeleteRepositoryWorkspace delete = new RTCDeleteRepositoryWorkspace();
			init(delete);
			
			delete.setWorkspaceRTC(WORKSPACE_NAME);
			delete.execute();
		}
	}
}
