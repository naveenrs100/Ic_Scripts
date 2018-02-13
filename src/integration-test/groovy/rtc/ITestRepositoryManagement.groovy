package rtc
import base.BaseTest;

import org.junit.Assert
import org.junit.Test

import rtc.commands.RTCCreateRepositoryWorkspace
import rtc.commands.RTCDeleteRepositoryWorkspace
import es.eci.utils.ComponentVersionHelper
import es.eci.utils.TmpDir


class ITestRepositoryManagement extends BaseTest {

	@Test
	public void testCreateRepository() {
		TmpDir.tmp { File tempDir ->
			RTCCreateRepositoryWorkspace command = new RTCCreateRepositoryWorkspace();
			init(command);
			
			long timestamp = new Date().getTime();
			String workspaceName = "TMP - UNITTESTS - testCreateRepository - $timestamp";
			command.setWorkspaceRTC(workspaceName);
			
			// Añadir un componente
			command.setStream("TESTSTREAM");
			command.setComponent("SimpleComponent1");
			
			command.execute();
			
			List<String> components = null;
			ComponentVersionHelper helper = new ComponentVersionHelper(
				scmToolsHome);
			components = helper.
					getComponents(tempDir, workspaceName, user, password, url);
			
			try {
				Assert.assertEquals(1, components.size());
				Assert.assertTrue(components.contains("SimpleComponent1"));
			}
			catch (Exception e) {
				throw e;
			}
			finally {
				RTCDeleteRepositoryWorkspace delete = new RTCDeleteRepositoryWorkspace();
				init(delete);
				delete.setWorkspaceRTC(workspaceName);
				
				delete.execute();
			}
		}
	}
	
	@Test
	public void testCreateAndUpdateRepository() {		
		TmpDir.tmp { File tempDir ->
			RTCCreateRepositoryWorkspace command = new RTCCreateRepositoryWorkspace();
			init(command);
			
			long timestamp = new Date().getTime();
			String workspaceName = "TMP - UNITTESTS - testCreateAndUpdateRepository - $timestamp";
			command.setWorkspaceRTC(workspaceName);
			
			// Añadir un componente
			command.setStream("TESTSTREAM");
			command.setComponent("SimpleComponent1");
			
			command.execute();
			// Ahora se le añade un componente
			command.setComponent("SimpleComponent2");
			command.execute();
			
			List<String> components = null;
			TmpDir.tmp { File helperDir -> 
				ComponentVersionHelper helper = new ComponentVersionHelper(
					scmToolsHome);
				components = helper.
						getComponents(tempDir, workspaceName, user, password, url);
			}
			
			try {
				Assert.assertEquals(2, components.size());
				Assert.assertTrue(components.contains("SimpleComponent1"));
				Assert.assertTrue(components.contains("SimpleComponent2"));
			}
			catch (Exception e) {
				throw e;
			}
			finally {
				RTCDeleteRepositoryWorkspace delete = new RTCDeleteRepositoryWorkspace();
				init(delete);
				delete.setWorkspaceRTC(workspaceName);
				
				delete.execute();
			}
		}
	}
	
	@Test
	public void testCreateWSRWithoutComponents() {
		TmpDir.tmp { File tempDir ->
			RTCCreateRepositoryWorkspace command = new RTCCreateRepositoryWorkspace();
			init(command);
			
			long timestamp = new Date().getTime();
			String workspaceName = "TMP - UNITTESTS - testCreateCleanWSR - $timestamp";
			command.setWorkspaceRTC(workspaceName);
			
			// Añadir un componente
			command.setStream("TESTSTREAM");
			
			command.execute();
			List<String> initialComponents = null;
				ComponentVersionHelper helper = new ComponentVersionHelper(
					scmToolsHome);
			TmpDir.tmp { File helperDir ->
				initialComponents = helper.
						getComponents(tempDir, workspaceName, user, password, url);
			}
			
			Assert.assertEquals(0, initialComponents.size());
			
			// Ahora se le añade un componente
			command.setComponent("SimpleComponent2");
			command.execute();
			
			List<String> components = null;
			TmpDir.tmp { File helperDir ->
				components = helper.
						getComponents(tempDir, workspaceName, user, password, url);
			}
			
			try {
				Assert.assertEquals(1, components.size());
				Assert.assertTrue(components.contains("SimpleComponent2"));
			}
			catch (Exception e) {
				throw e;
			}
			finally {
				RTCDeleteRepositoryWorkspace delete = new RTCDeleteRepositoryWorkspace();
				init(delete);
				delete.setWorkspaceRTC(workspaceName);
				
				delete.execute();
			}
		}
	}
}
