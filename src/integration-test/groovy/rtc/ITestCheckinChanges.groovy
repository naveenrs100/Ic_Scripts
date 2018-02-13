package rtc
import base.BaseTest;

import org.junit.Assert
import org.junit.Test

import rtc.commands.RTCCheckinCommand
import rtc.commands.RTCDownloaderCommand
import es.eci.utils.TmpDir
import es.eci.utils.commandline.CommandLineHelper

class ITestCheckinChanges extends BaseTest {

	@Test
	public void testCheckinChangedTXT() {
		// Prueba que sube solo los cambios indicados en el changed.txt
		
	}
	
	@Test
	public void testCheckin() {
		long timestamp = new Date().getTime();
		String fragment = "\n<!-- modified at $timestamp-->"
		String command = null;
		if (System.properties['os.name'].toLowerCase().contains('windows')) {
			command = "dir";
		}
		else {
			command = "ls -la";
		}
		TmpDir.tmp { File tempDirectory ->
			// Descargar un componente
			RTCDownloaderCommand checkOut = new RTCDownloaderCommand();
			init(checkOut);
			checkOut.setStream("EVOLUTING_TESTSTREAM");
			checkOut.setWorkspaceRTC("WSR - EVOLUTING TMP");
			checkOut.setComponent("SimpleComponent1");
			checkOut.setParentWorkspace(tempDirectory);
			
			checkOut.execute();
			
			// Este cambio no debe existir
			File change = new File(tempDirectory, "didnt_exist_before_$timestamp");
			change.createNewFile();
			
			File pom = new File(tempDirectory, "pom.xml")
			pom.text += fragment
			
			
			CommandLineHelper helper = new CommandLineHelper(command);
			helper.initLogger { println it }
			helper.execute(tempDirectory)
			
			// Comprobar el estado
			RTCCheckinCommand checkIn = new RTCCheckinCommand();
			init(checkIn);
			checkIn.setWorkspaceRTC("WSR - EVOLUTING TMP");
			checkIn.setDescription("Test change");
			checkIn.setWorkItem(workItem);
			checkIn.setParentWorkspace(tempDirectory);
			
			checkIn.execute();
		}
		
		TmpDir.tmp { File downloadDirectory ->
			RTCDownloaderCommand checkOut = new RTCDownloaderCommand();
			init(checkOut);
			// Baja el workspace, tal cual se ha quedado
			checkOut.setWorkspaceRTC("WSR - EVOLUTING TMP");
			checkOut.setComponent("SimpleComponent1");		
			checkOut.setParentWorkspace(downloadDirectory);
			
			checkOut.execute();
			CommandLineHelper helper = new CommandLineHelper(command);
			helper.initLogger { println it }
			helper.execute(downloadDirectory)
			// Comprobar que el fragmento está en el pom.xml
			
			File pom = new File(downloadDirectory, "pom.xml")
			Assert.assertTrue(pom.text.contains(fragment))
			
			// Este cambio no debe haber subido, ya que al principio
			//	hemos cargado el componente con include-root = false
			File change = new File(downloadDirectory, "didnt_exist_before_$timestamp");
			Assert.assertFalse(change.exists());
		}
	}
}
