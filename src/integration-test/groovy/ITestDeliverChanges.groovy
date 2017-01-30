import org.junit.Assert
import org.junit.Test

import rtc.commands.RTCCheckinCommand
import rtc.commands.RTCDeleteRepositoryWorkspace
import rtc.commands.RTCDeliverCommand
import es.eci.utils.TmpDir
import es.eci.utils.commandline.CommandLineHelper

class ITestDeliverChanges extends BaseTest {

	private static final String STREAM_NAME = "CHECKIN_AND_DELIVER_STREAM"
	private static final String COMPONENT_NAME = "SimpleComponent1"
	private static final String INNER_DIRECTORY_PATH = "App1-WAR"
	private static final String NEW_FILE_NAME = "didntexistpreviously";
	
	@Test
	public void testDeliverNewFile() {
		String command = null;
		if (System.properties['os.name'].toLowerCase().contains('windows')) {
			command = "dir";
		}
		else {
			command = "ls -la";
		}
		
		CommandLineHelper listFiles = new CommandLineHelper(command);
		
		long timestamp = new Date().getTime();
		final String WORKSPACE_NAME = STREAM_NAME + timestamp		
		try {		
			
			// 1.- Entregar un cambio a la corriente
			TmpDir.tmp { File tempDirectory ->
				
				download(STREAM_NAME, COMPONENT_NAME, WORKSPACE_NAME, tempDirectory);
				// Añadir un fichero a la altura de App1-WAR
				File appWAR = new File(tempDirectory, INNER_DIRECTORY_PATH)
				File newFile = new File(appWAR, NEW_FILE_NAME);
				
				newFile.createNewFile();
				newFile.text = timestamp;
				
				RTCCheckinCommand checkIn = new RTCCheckinCommand();
				init(checkIn);
				
				checkIn.setParentWorkspace(tempDirectory);
				checkIn.setDescription("Deliver unit test " + timestamp);
				checkIn.setWorkspaceRTC(WORKSPACE_NAME);
				checkIn.setWorkItem(workItem);
				
				checkIn.execute();
				
				// Entregar el cambio a la corriente
				RTCDeliverCommand deliver = new RTCDeliverCommand();
				init(deliver);
				deliver.setStreamTarget(STREAM_NAME);
				deliver.setWorkspaceRTC(WORKSPACE_NAME);
				deliver.setParentWorkspace(tempDirectory);
				deliver.setComponent(COMPONENT_NAME);
				
				deliver.execute();
			}
			
			// 2.- Bajar el componente a otro directorio para asegurarnos de que 
			//	se ha entregado el cambio a la corriente
			TmpDir.tmp { File validationDirectory ->
				download(STREAM_NAME, COMPONENT_NAME, WORKSPACE_NAME, validationDirectory);
				File appWAR = new File(validationDirectory, INNER_DIRECTORY_PATH)
				File checkFile = new File(appWAR, NEW_FILE_NAME);
				// Validar que el fichero existe
				Assert.assertTrue(checkFile.exists());
				// Validar el contenido
				Assert.assertEquals(Long.toString(timestamp), checkFile.text);
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
