package rtc
import base.BaseTest;

import org.junit.Assert
import org.junit.Test

import rtc.commands.RTCDeleteRepositoryWorkspace
import rtc.commands.RTCDownloaderCommand
import es.eci.utils.TmpDir

class ITestRTCDownloader extends BaseTest {
	
	
	// La corriente utilizada para estas pruebas tiene 4 componentes:
	// 	 PruebaRelease - App 1
	// 	 PruebaRelease - App 2
	// 	 PruebaRelease - Biblioteca 1
	// 	 PruebaRelease - Biblioteca 2
	
	
	// Baja un ejemplo sencillo de aplicación con un pom.xml, un módulo
	//	WAR y un módulo EAR
	
	@Test
	public void test() {
		TmpDir.tmp { File daemonConfigDir ->
			TmpDir.tmp { File tempDownload ->
				RTCDownloaderCommand command = new RTCDownloaderCommand();
				
				init(command);
				
				// Intenta bajar el código
				/* Obligatorios (tomados de la documentación)
				    workspaceRTC Nombre del workspace de repositorio al que reincorporar los cambios
					component Nombre de componente RTC
					stream Corriente de origen
					baseDir Directorio de ejecución
					light Indica si se debe usar o no la versión ligera del comando scm

				 */
				command.setWorkspaceRTC("TMP - UNITTESTS");
				command.setComponent("SimpleComponent1");
				command.setStream("EVOLUTING_TESTSTREAM");
				command.setParentWorkspace(tempDownload);
				
				// Lanzar el comando
				command.execute();
				
				// Verificar el contenido
				Assert.assertTrue(new File(tempDownload, "pom.xml").exists());
				Assert.assertTrue(new File(tempDownload, "App1-EAR").exists());
				Assert.assertTrue(new File(tempDownload, "App1-WAR").exists());
			}
		}
	}
	
	@Test
	public void testRecreateWS() {
		TmpDir.tmp { File daemonConfigDir ->
			TmpDir.tmp { File tempDownload ->
				RTCDownloaderCommand command = new RTCDownloaderCommand();
				
				init(command);
				
				// Intenta bajar el código
				/* Obligatorios (tomados de la documentación)
				    workspaceRTC Nombre del workspace de repositorio al que reincorporar los cambios
					component Nombre de componente RTC
					stream Corriente de origen
					baseDir Directorio de ejecución
					light Indica si se debe usar o no la versión ligera del comando scm

				 */
				command.setWorkspaceRTC("TMP - UNITTESTS - RWS");
				command.setComponent("SimpleComponent1");
				command.setStream("EVOLUTING_TESTSTREAM");
				command.setRecreateWS(true);
				command.setParentWorkspace(tempDownload);
				
				// Lanzar el comando
				command.execute();
				
				// Verificar el contenido
				Assert.assertTrue(new File(tempDownload, "pom.xml").exists());
				Assert.assertTrue(new File(tempDownload, "App1-EAR").exists());
				Assert.assertTrue(new File(tempDownload, "App1-WAR").exists());
			}
		}
	}
	
	@Test
	public void testCreateWorkspaceOnFly() {
		TmpDir.tmp { File daemonConfigDir ->
			TmpDir.tmp { File tempDownload ->
				RTCDownloaderCommand command = new RTCDownloaderCommand();
				
				init(command);
				
				// Intenta bajar el código
				/* Obligatorios (tomados de la documentación)
				    workspaceRTC Nombre del workspace de repositorio al que reincorporar los cambios
					component Nombre de componente RTC
					stream Corriente de origen
					baseDir Directorio de ejecución
					light Indica si se debe usar o no la versión ligera del comando scm

				 */
				long timestamp = new Date().getTime();
				String workspaceName = "TMP - UNITTESTS - testCreateWorkspaceOnFly - $timestamp";
				command.setWorkspaceRTC(workspaceName);
				command.setComponent("SimpleComponent1");
				command.setStream("EVOLUTING_TESTSTREAM");
				command.setParentWorkspace(tempDownload);
				
				// Lanzar el comando
				command.execute()
				
				// Verificar el contenido
				Assert.assertTrue(new File(tempDownload, "pom.xml").exists());
				Assert.assertTrue(new File(tempDownload, "App1-EAR").exists());
				Assert.assertTrue(new File(tempDownload, "App1-WAR").exists());
				
				RTCDeleteRepositoryWorkspace delete = new RTCDeleteRepositoryWorkspace();
				init(delete);
				delete.setWorkspaceRTC(workspaceName);
				delete.execute();
			}
		}
	}
	
	@Test
	public void testDownloadBaselineOnFly() {
		TmpDir.tmp { File daemonConfigDir ->
			TmpDir.tmp { File tempDownload ->
				RTCDownloaderCommand command = new RTCDownloaderCommand();
				
				init(command);
				
				// Intenta bajar el código
				/* Obligatorios (tomados de la documentación)
				    workspaceRTC Nombre del workspace de repositorio al que reincorporar los cambios
					component Nombre de componente RTC
					stream Corriente de origen
					baseDir Directorio de ejecución
					light Indica si se debe usar o no la versión ligera del comando scm

				 */
				long timestamp = new Date().getTime();
				String workspaceName = "TMP - UNITTESTS - testDownloadBaselineOnFly - $timestamp";
				command.setWorkspaceRTC(workspaceName);
				command.setComponent("SimpleComponent1");
				command.setStream("EVOLUTING_TESTSTREAM");
				command.setBaseline("FIRST_BASELINE_TESTCASE");
				command.setParentWorkspace(tempDownload);
				
				// Lanzar el comando
				command.execute();
				
				// Verificar el contenido
				def fPom = new File(tempDownload, "pom.xml");
				Assert.assertTrue(fPom.exists());
				Assert.assertTrue(new File(tempDownload, "App1-EAR").exists());
				Assert.assertTrue(new File(tempDownload, "App1-WAR").exists());
				
				// La versión del pom.xml es la que esperamos
				def pom = new XmlSlurper().parseText(fPom.text);
				Assert.assertEquals("6.0.1.0-SNAPSHOT", pom.version.text());
				
				RTCDeleteRepositoryWorkspace delete = new RTCDeleteRepositoryWorkspace();
				init(delete);
				delete.setWorkspaceRTC(workspaceName);
				delete.execute();
			}
		}
	}
	
	@Test
	public void testDownloadSnapshotOnFly() {
		TmpDir.tmp { File daemonConfigDir ->
			TmpDir.tmp { File tempDownload ->
				RTCDownloaderCommand command = new RTCDownloaderCommand();
				
				init(command);
				
				// Intenta bajar el código
				/* Obligatorios (tomados de la documentación)
				    workspaceRTC Nombre del workspace de repositorio al que reincorporar los cambios
					component Nombre de componente RTC
					stream Corriente de origen
					baseDir Directorio de ejecución
					light Indica si se debe usar o no la versión ligera del comando scm

				 */
				long timestamp = new Date().getTime();
				String workspaceName = "TMP - UNITTESTS - testDownloadSnapshotOnFly - $timestamp";
				command.setWorkspaceRTC(workspaceName);
				command.setComponent("SimpleComponent1");
				command.setStream("EVOLUTING_TESTSTREAM");
				command.setSnapshot("EVOLUTING_TESTSTREAM - 21.22.23.46");
				command.setParentWorkspace(tempDownload);
				
				// Lanzar el comando
				command.execute();
				
				// Verificar el contenido
				def fPom = new File(tempDownload, "pom.xml");
				Assert.assertTrue(fPom.exists());
				
				// La versión del pom.xml es la que esperamos
				def pom = new XmlSlurper().parseText(fPom.text);
				Assert.assertEquals("7.0.1.0-SNAPSHOT", pom.version.text());
				
				RTCDeleteRepositoryWorkspace delete = new RTCDeleteRepositoryWorkspace();
				init(delete);
				delete.setWorkspaceRTC(workspaceName);
				delete.execute();
			}
		}
	}
}
