package maven
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import components.MavenComponent

import es.eci.utils.RTCBuildFileHelper
import es.eci.utils.TmpDir
import es.eci.utils.ZipHelper

class TestReactor {
	
	private String maven = null;
	
	@Before
	public void setup() {
		maven = System.getProperty("MAVEN_HOME") + "/bin/mvn";
	}
	
	
	@Test 
	public void testMavenComp() {
		MavenComponent comp1 = new MavenComponent("comp1");
		MavenComponent comp2 = new MavenComponent("comp2");
		comp1.addDependency(comp2);
	}
	
	@Test
	public void testMavenCompCircularDependency() {
		MavenComponent comp1 = new MavenComponent("comp1");
		MavenComponent comp2 = new MavenComponent("comp2");
		MavenComponent comp3 = new MavenComponent("comp3");
		comp1.addDependency(comp2);
		comp2.addDependency(comp3);
		try {
			comp3.addDependency(comp1);
			Assert.fail();
		}
		catch(Exception e) {
			// Correcto
			println "Intento detectado de crear una dependencia circular"
		}
	}
	
	/* @Test
	public void testMavenDependencyGraph() {
		MavenComponent comp1 = new MavenComponent("comp1");
		MavenComponent comp2 = new MavenComponent("comp2");
		MavenComponent comp3 = new MavenComponent("comp3");
		// Comp1 --> Comp2
		// Comp3 --> Comp1
		comp1.addDependency(comp2);
		comp3.addDependency(comp1);
		// Grafo de dependencias esperado
		// [ comp2 , comp1, comp3 ]
		List<MavenComponent> expected = [ comp2 , comp1, comp3 ] 
		List<MavenComponent> componentsList = [ comp1 , comp2, comp3 ]
		// Ordenar el grafo
		Collections.sort(componentsList);
		Assert.assertEquals(expected, componentsList)
	} */

	@Test
	public void testComplexMavenReactor() {
		testReactor("reactor/pruebaReactor.zip")
	}
	
	@Test
	public void test6BRAPIMavenReactor() {
		testReactor("reactor/6BR_test_Reactor.zip")
	}
	
	@Test
	public void testQSPMavenReactor() {
		testReactor("reactor/QSP_test_Reactor.zip")
	}
	
	private void testReactor(String zipName) {
		TmpDir.tmp { File parentWorkspace ->
			// Directorio ficticio
			RTCBuildFileHelper helper = new RTCBuildFileHelper("build", parentWorkspace);	
			helper.initLogger { println it }
			TmpDir.tmp { File tmpDir ->
				ZipHelper.unzipInputStream(
					TestReactor.class.
						getClassLoader().
							getResourceAsStream(zipName),
					tmpDir
					);
				// def components = ["AppWeb1", "AppWeb2", "Lib1", "Lib2", "Lib3"]
				def components = [];
				tmpDir.eachDir {
					components.add(it.getName())
				}
				
				List<MavenComponent> sortedGraph = helper.buildArtifactsFile(components, tmpDir);
				File artifacts = new File(parentWorkspace, "artifacts.json");
				Assert.assertTrue(artifacts.exists())
				println "artifacts.text: " + artifacts.text
				println "----> Grafo ordenado: $sortedGraph"
				
				testSortedComponentMap(sortedGraph);
			}
		}
	}
	
	private void testSortedComponentMap (List<MavenComponent> sortedGraph) {
		boolean correcto = true;
		
		for ( int loop = 0; loop < sortedGraph.size(); loop ++ ) {
			for ( int iloop = 0; iloop < loop; iloop ++ ) {
				// println "Test " + sortedGraph[loop] + " <> " + sortedGraph[iloop] + " : " +
				correcto &= (sortedGraph[loop].compareTo(sortedGraph[iloop]) >= 0)
			}
		}
		Assert.assertTrue(correcto);
	}
}
