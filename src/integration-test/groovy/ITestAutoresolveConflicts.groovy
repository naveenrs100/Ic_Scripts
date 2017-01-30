import org.junit.Assert
import org.junit.Test

import rtc.commands.RTCDeleteRepositoryWorkspace
import rtc.commands.RTCDeliverCommand
import es.eci.utils.TmpDir

class ITestAutoresolveConflicts extends BaseTest {

	private static final String STREAM_NAME = "CHECKIN_AND_DELIVER_STREAM";
	private static final String COMPONENT_NAME = "SimpleComponent2";
	
	@Test
	public void testConflict() {
		// Se va a provocar un conflicto muy sencillo
		// 1.- Se crean los WSR1 y WSR2 correspondientes al mismo componente
		//		de la misma corriente
		// 2.- Se reincorporan cambios distintos sobre el mismo fichero a ambos 
		//		WSR
		// 3.- Se entregan a la corriente los cambios de WSR1
		// 4.- Se intenta aceptar los cambios de la corriente sobre WSR2, lo cual
		//		provoca que se intente resolver el conflicto a favor de la corriente
		
		long timestamp = new Date().getTime();
		final String WORKSPACE_1_NAME = String.format("%s_%s_1", STREAM_NAME, timestamp);
		final String WORKSPACE_2_NAME = String.format("%s_%s_2", STREAM_NAME, timestamp);
		final String FRAGMENT1 = String.format("<!-- change 1 :: %s -->", timestamp);
		final String FRAGMENT2 = String.format("<!-- change 2 :: %s -->", timestamp);
		
		try {
			TmpDir.tmp { File dir2 ->
				download(STREAM_NAME, COMPONENT_NAME, WORKSPACE_2_NAME, dir2);
				changeFileAndCheckinChanges(workItem, WORKSPACE_2_NAME, dir2, FRAGMENT2);
				
			}
			
			TmpDir.tmp { File dir1 ->
				download(STREAM_NAME, COMPONENT_NAME, WORKSPACE_1_NAME, dir1);
				changeFileAndCheckinChanges(workItem, WORKSPACE_1_NAME, dir1, FRAGMENT1);
				// Este cambio se entrega a corriente
				RTCDeliverCommand deliver = new RTCDeliverCommand();
				init(deliver)
				deliver.setParentWorkspace(dir1);
				deliver.setStreamTarget(STREAM_NAME);
				deliver.setWorkspaceRTC(WORKSPACE_1_NAME);
				deliver.setComponent(COMPONENT_NAME);
				
				deliver.execute();
			}
			
			TmpDir.tmp { File validationDir ->
				println "Checking autoresolve..."
				// Se intenta descargar el código de nuevo a partir del WSR2
				download(STREAM_NAME, COMPONENT_NAME, WORKSPACE_2_NAME, validationDir);
				// Debe detectar el conflicto, informar de él por el log y reemplazarlo
				//	silenciosamente por el contenido de la corriente
				File changedFile = new File(validationDir, "pom.xml");
				Assert.assertTrue(changedFile.text.endsWith(FRAGMENT1));
			}
		}
		finally {
			RTCDeleteRepositoryWorkspace delete = new RTCDeleteRepositoryWorkspace();
			init(delete);
			
			delete.setWorkspaceRTC(WORKSPACE_1_NAME);
			delete.execute();
			
			delete.setWorkspaceRTC(WORKSPACE_2_NAME);
			delete.execute();
		}
	}
}
