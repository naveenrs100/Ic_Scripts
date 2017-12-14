import java.io.File;

import org.junit.Test

import es.eci.utils.TmpDir
import es.eci.utils.npm.NpmCheckCommand;
import es.eci.utils.npm.NpmCloseVersionCommand
import es.eci.utils.npm.NpmIncreaseAndOpenVersionCommand
import es.eci.utils.npm.NpmIncreaseFixCommand
import es.eci.utils.npm.NpmIncreaseHotFixCommand
import es.eci.utils.npm.NpmMavenUploadCommand;
import es.eci.utils.npm.NpmVersionHelper
import groovy.json.JsonOutput
import groovy.json.JsonSlurper


class TestNpm extends GroovyTestCase{

	
	@Test
	public void testAddPaddingToVersion() {
		NpmVersionHelper nh = new NpmVersionHelper()
		String test = nh.addPaddingToVersion("1.0");
		assert test  == "1.0.0"
						
		test = nh.addPaddingToVersion("1.0.0");
		assert test  == "1.0.0"
	
		test = nh.addPaddingToVersion("1.0.1234");
		assert test  == "1.0.1234"
		
		test = nh.addPaddingToVersion("1.0.0.0");
		assert test  == "1.0.0.0"
		
		test = nh.addPaddingToVersion("1.0.1234-SNAPSHOT");
		assert test  == "1.0.1234-SNAPSHOT"
		
		test = nh.addPaddingToVersion("1.0.1234-2");
		assert test  == "1.0.1234-2"

		def msg = shouldFail {
			test = nh.addPaddingToVersion("1");
		}
		
		assertEquals 'Version : 1 format incorrect', msg
	
			// test to check that the regular expression use dot as char 
		test = "1a0a0a0";
		assert !(test  =~ /1\.0\.0\.0/)
		
		
				
	}

	
	@Test
	public void testCheck() {
		TmpDir.tmp { File dir ->
//			File json = new File(dir, "version.json");
//			json.text =	"""version=1.0.0.0-SNAPSHOT"""
			InputStream is = TestNpm.class.getClassLoader().
				getResourceAsStream("version_1.json");
			def obj = new JsonSlurper().parse(is);
			is.close();
			File json = new File(dir, "package.json");
			json.createNewFile();
			json.text = JsonOutput.toJson(obj)
			
			NpmCheckCommand command = new NpmCheckCommand();
			command.setParentWorkspace(dir);
			command.execute();
			
			File file = new File(dir, "package.json")
			assert file.text  =~ /1\.0\.0-SNAPSHOT/
			
		}
	}
	
	
	@Test
	public void testCloseVersion() {
		TmpDir.tmp { File dir ->
//			File json = new File(dir, "version.json");
//			json.text =	"""version=1.0.0.0-SNAPSHOT"""
			InputStream is = TestNpm.class.getClassLoader().
				getResourceAsStream("version_1.json");
			def obj = new JsonSlurper().parse(is);
			is.close();
			File json = new File(dir, "package.json");
			json.createNewFile();
			json.text = JsonOutput.toJson(obj)
			
			NpmCloseVersionCommand command = new NpmCloseVersionCommand();
			command.setParentWorkspace(dir);
			command.execute();
			
			File file = new File(dir, "package.json")
			assert file.text  =~ /1\.0\.0/
			
		}
	}
	
	@Test
	public void testIncreaseAndOpen() {
		TmpDir.tmp { File dir ->
			InputStream is = TestNpm.class.getClassLoader().
				getResourceAsStream("version_2.json");
			def obj = new JsonSlurper().parse(is);
			is.close();
			File json = new File(dir, "package.json");
			json.createNewFile();
			json.text = JsonOutput.toJson(obj)
			
			NpmIncreaseAndOpenVersionCommand command = new NpmIncreaseAndOpenVersionCommand();
			command.setParentWorkspace(dir);
			command.execute();
			
			File file = new File(dir, "package.json")
			assert file.text  =~ /1\.1\.0-SNAPSHOT/
			
		}
	}
	
	@Test
	public void testIncreaseFix() {
		TmpDir.tmp { File dir ->
			InputStream is = TestNpm.class.getClassLoader().
				getResourceAsStream("version_3.json");
			def obj = new JsonSlurper().parse(is);
			is.close();
			File json = new File(dir, "package.json");
			json.createNewFile();
			json.text = JsonOutput.toJson(obj)
			
			NpmIncreaseFixCommand command = new NpmIncreaseFixCommand();
			command.setParentWorkspace(dir);
			command.execute();
			
			File file = new File(dir, "package.json")
			assert file.text  =~ /1\.2\.4/
			
		}
	}
	
	
	@Test
	public void testIncreaseHotFix() {
		TmpDir.tmp { File dir ->
			InputStream is = TestNpm.class.getClassLoader().
				getResourceAsStream("version_3.json");
			def obj = new JsonSlurper().parse(is);
			is.close();
			File json = new File(dir, "package.json");
			json.createNewFile();
			json.text = JsonOutput.toJson(obj)
			
			NpmIncreaseHotFixCommand command = new NpmIncreaseHotFixCommand();
			command.setParentWorkspace(dir);
			command.execute();

			assert json.text  =~ /1\.2\.3-1/
			command.execute();
			assert json.text  =~ /1\.2\.3-2/
			
		}
	}
	
	//@Test
//	public void testExceptionsCases()   {
//		
//		TmpDir.tmp { File dir ->
//			InputStream is = TestNpm.class.getClassLoader().
//				getResourceAsStream("version_2.json");
//			def obj = new JsonSlurper().parse(is);
//			is.close();
//			File json = new File(dir, "package.json");
//			json.createNewFile();
//			json.text = JsonOutput.toJson(obj)
//			
//			NpmCloseVersionCommand command = new NpmCloseVersionCommand();
//			command.setParentWorkspace(dir);
//			
//			// Test for exception resulting message.
//			def msg = shouldFail {
//				command.execute();
//			}
//			
//			assertEquals 'We are expectiong an Open Version but we received :1.0.0.0', msg  
//			
//			// we are going to open the version 
//			NpmIncreaseAndOpenVersionCommand commandOpen = new NpmIncreaseAndOpenVersionCommand();
//			commandOpen.setParentWorkspace(dir);
//			commandOpen.execute()
//
//			// now the version is open should fail if we try to open it again
//			// Test for exception resulting message.
//			msg = shouldFail {
//				commandOpen.execute();
//			}
//
//			assertEquals 'We are expectiong a Closed Version but we received :1.0.1.0-SNAPSHOT', msg
//
//			// now the version is open should fail if we try to increase a fix
//			// Test for exception resulting message.
//			
//			NpmIncreaseFixCommand commandFix = new NpmIncreaseFixCommand();
//			commandFix.setParentWorkspace(dir);
//
//			msg = shouldFail {
//				commandFix.execute()
//			}
//			
//			assertEquals 'We are expectiong a Closed Version but we received :1.0.1.0-SNAPSHOT', msg
//
//			// now the version is open should fail if we try to increase an hotfix
//			// Test for exception resulting message.
//			
//			NpmIncreaseHotFixCommand commandHotFix = new NpmIncreaseHotFixCommand();
//			commandHotFix.setParentWorkspace(dir);
//
//			msg = shouldFail {
//				commandHotFix.execute()
//			}
//			
//			assertEquals 'We are expectiong a Closed ir Hotfixed Version but we received :1.0.1.0-SNAPSHOT', msg
//			
//		}
//	}
//
//
//	@Test
//	public void testUploadNexusClosedVersion() {
//		
//		println "test upload nexus Closed Version"
//		
//		TmpDir.tmp { File dir ->
//			
//			// preparing version file 
//			InputStream is = TestNpm.class.getClassLoader().
//			getResourceAsStream("version_2.json");
//			def obj = new JsonSlurper().parse(is);
//			is.close();
//			File json = new File(dir, "package.json");
//			json.createNewFile();
//			json.text = JsonOutput.toJson(obj)
//			
//			// preparing package to upload in tmp Workspace 
//			
//			URL resource = TestNpm.class.getResource("mitarjeta-0.0.0-1.x86_64.zip");
//			String path = new File(resource.toURI()).getAbsolutePath();
//			InputStream fis = TestNpm.class.getClassLoader().getResourceAsStream("mitarjeta-0.0.0-1.x86_64.zip");
//			File file = new File(dir, "mitarjeta-1.0.0.0.x86_64.zip");
//			FileOutputStream  fos = new FileOutputStream(file)
//			fos << fis
//			fis.close()
//			fos.close()
//			
//			
//			
//			NpmMavenUploadCommand command = new NpmMavenUploadCommand();
//			println "created"
////			command.setUrl(path);
//			
//			command.initLogger({println it});
//			
//			command.setMaven("mvn")
//			command.setGroupId("es.eci.test")
//			command.setArtifactId("mitarjeta")
//			command.setNexusPathClosed("http://nexus.elcorteingles.pre/content/repositories/eci/")
//			command.setNexusPathOpen("http://nexus.elcorteingles.pre/content/repositories/eci-snapshot/")
//			command.setType("zip")
//						
//			command.setParentWorkspace(dir);
//			command.execute();
//			
//		}
//	}
//
//	@Test
//	public void testUploadNexusOpenVersion() {
//		
//		println "test upload nexus Open Version"
//		
//		TmpDir.tmp { File dir ->
//			
//			// preparing version file
//			InputStream is = TestNpm.class.getClassLoader().
//			getResourceAsStream("version_1.json");
//			def obj = new JsonSlurper().parse(is);
//			is.close();
//			File json = new File(dir, "package.json");
//			json.createNewFile();
//			json.text = JsonOutput.toJson(obj)
//			
//			// preparing package to upload in tmp Workspace
//			
//			URL resource = TestNpm.class.getResource("mitarjeta-0.0.0-1.x86_64.rpm");
//			String path = new File(resource.toURI()).getAbsolutePath();
//			InputStream fis = TestNpm.class.getClassLoader().getResourceAsStream("mitarjeta-0.0.0-1.x86_64.rpm");
//			File file = new File(dir, "mitarjeta-1.0.0.0-SNAPSHOT.x86_64.rpm");
//			FileOutputStream  fos = new FileOutputStream(file)
//			fos << fis
//			fis.close()
//			fos.close()
//						
//			NpmMavenUploadCommand command = new NpmMavenUploadCommand();
//			println "created"
//			
//			command.initLogger({println it});
//			command.setMaven("mvn")
//			command.setGroupId("es.eci.test")
//			command.setArtifactId("mitarjeta")
//			command.setNexusPathClosed("http://nexus.elcorteingles.pre/content/repositories/eci/")
//			command.setNexusPathOpen("http://nexus.elcorteingles.pre/content/repositories/eci-snapshots/")
//			command.setType("rpm")
//						
//			command.setParentWorkspace(dir);
//			command.execute();
//			
//		}
//	}
//
//	
//	@Test
//	public void testUploadNexusError() {
//		
//		println "test upload nexus Closed Version"
//		
//		TmpDir.tmp { File dir ->
//			
//			// preparing version file
//			InputStream is = TestNpm.class.getClassLoader().
//			getResourceAsStream("version_2.json");
//			def obj = new JsonSlurper().parse(is);
//			is.close();
//			File json = new File(dir, "package.json");
//			json.createNewFile();
//			json.text = JsonOutput.toJson(obj)
//			
//			// preparing package to upload in tmp Workspace
//			
//			URL resource = TestNpm.class.getResource("mitarjeta-0.0.0-1.x86_64.zip");
//			String path = new File(resource.toURI()).getAbsolutePath();
//			InputStream fis = TestNpm.class.getClassLoader().getResourceAsStream("mitarjeta-0.0.0-1.x86_64.zip");
//			File file = new File(dir, "mitarjeta-1.0.0.0.x86_64.zip");
//			FileOutputStream  fos = new FileOutputStream(file)
//			fos << fis
//			fis.close()
//			fos.close()
//			
//			
//			
//			NpmMavenUploadCommand command = new NpmMavenUploadCommand();
//			println "created"
//
//			command.initLogger({println it});
//			
//			command.setMaven("mvn")
//			command.setGroupId("es.eci.test")
//			command.setArtifactId("mitarjeta")
//			command.setNexusPathClosed("http://nexus.elcorteingles.pre/content/repositories/eci/")
//			command.setNexusPathOpen("http://nexus.elcorteingles.pre/content/repositories/eci-snapshot/")
//			// set wrong type to make it fail 
//			command.setType("rpm")
//						
//			command.setParentWorkspace(dir);
//			
//			def msg = shouldFail {
//				command.execute()
//			}
//			
//			print  "error message : ${msg}"  
//			
//			assertEquals 'Error al ejecutar uploadToNexus . file not found!', msg
//		
//		}
//	}
	
	
	@Test
	public void testIsNpmValidVersion() {
		NpmVersionHelper nh = new NpmVersionHelper()
		
		assert nh.isNpmValidVersion("1.0.0")
		assert nh.isNpmValidVersion("1.0.0-s")
		assert nh.isNpmValidVersion("1.0.0-sosdfjsopdj")
		assert !nh.isNpmValidVersion("1.0.0.0")
		assert !nh.isNpmValidVersion("1.0.0.")
		
				
	}
	
	
}
