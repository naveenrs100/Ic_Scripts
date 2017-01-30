import org.junit.Assert
import org.junit.Test

import rtc.commands.RTCgetComponentsCommand
import es.eci.utils.Stopwatch
import es.eci.utils.TmpDir


class ITestGetComponents extends BaseTest {

	// La corriente TESTSTREAM utilizada para estas pruebas tiene 4 componentes:
	// 	 SimpleComponent1
	// 	 SimpleComponent2
	// 	 SimpleComponent3
	// 	 SimpleComponent4
	// El wsr ONEMODIFIED tiene cambios entrantes desde la corriente en uno de ellos
	// Al wsr 2NEWCOMPONENTS le faltan 2 componentes respecto a la corriente, los otros son iguales
	// El wsr NOCHANGES está al día respecto a la corriente
	// El wsr TWO_MODIFIED_ONE_LEFT tiene dos componentes por detrás de la corriente, y le falta
	//	otro componente además
	
	@Test
	public void compareComponents() {
		int components = launchTest("TESTSTREAM", "WSR - TESTCOMPARE - ONEMODIFIED", true)
		// Devuelve el único componente con cambios con respecto a la corriente
		Assert.assertEquals(1, components)
	}
	
	@Test
	public void getComponentsNewInStream() {
		// Devuelve 2 componentes: los dos que faltan, que se considera que han cambiado
		int components = launchTest("TESTSTREAM", "WSR - TESTCOMPARE - 2NEWCOMPONENTS", true)
		Assert.assertEquals(2, components)
	}
	
	@Test
	public void getAllComponents() {
		int components = launchTest("TESTSTREAM", null, false)
		// Devuelve los 4 componentes, en tanto que no le hemos dicho WSR con el que comparar
		Assert.assertEquals(4, components)
	}
	
	@Test
	public void getNoChanges() {
		int components = launchTest("TESTSTREAM", "WSR - TESTCOMPARE - NOCHANGES", true)
		// No devuelve componentes, en tanto que nos aseguramos que el WSR esté a
		//	la altura de los cambios reflejados en la corriente
		Assert.assertEquals(0, components)
	}
	
	@Test
	public void getThreeChanges() {
		int components = launchTest("TESTSTREAM", "WSR - TESTCOMPARE - TWO_MODIFIED_ONE_LEFT", true)
		Assert.assertEquals(3, components)
	}
	
	private int launchTest(String stream, String workspaceRTC, boolean onlyChanges) {
		int modified = 0;
		
		TmpDir.tmp { File tempDir ->
			def output = new File(tempDir, "my_components.txt")
			long millis = Stopwatch.watch { 
				
				RTCgetComponentsCommand command = new RTCgetComponentsCommand();
				
				init(command);
				
				// Listar los componentes
				/*
				 * --- OBLIGATORIOS<br/>
				 * <b>fileOut</b> Fichero en el que se almacenan los resultados<br/>
				 * <b>baseDir</b> Directorio de ejecución<br/>
				 * <b>stream</b> Corriente cuyos componentes deseamos listar<br/>
				 * <b>light</b> Indica si se debe usar o no la versión ligera del comando scm<br/>
				 * --- OPCIONALES<br/>
				 * <b>workspaceRTC</b> Workspace de RTC de referencia para la comparación de cambios<br/>
				 */
							
				command.setFileOut(output);
				command.setTypeTarget("stream");
				command.setNameTarget(stream);
				command.setParentWorkspace(tempDir);
				command.setOnlyChanges(onlyChanges);
				
				if (workspaceRTC != null) {
					command.setTypeOrigin("workspace")
					command.setNameOrigin(workspaceRTC)
				}
				
				// Crear el listado
				command.execute();
			}
			
			println "";
			println "";
			println "[$millis ms.] --COMPONENTES ($workspaceRTC)";
			// Validar el fichero
			boolean currentComponent = false;
			output.text.eachLine { String line ->
				
				if (line.trim().startsWith("Component:")) {
					// Caso trivial
					modified++;	
				}
				else if (line.trim().startsWith("Component (")){
					currentComponent = true;
				}
				else if (currentComponent) {
					modified++;
					currentComponent = false;
				}
			} 
		}
		return modified;
	}
}
