package es.eci.utils.npm;

import groovy.io.FileType;

import es.eci.utils.base.Loggable;
import es.eci.utils.commandline.CommandLineHelper;

/**
 */
public class NpmMochaIstanbulCommand extends Loggable {
	
	protected File parentWorkspace;
	
	/**
	 * Ejecuta las pruebas unitarias con Mocha, almacenando el resultado en el fichero
	 * xunit.xml en la ruta de ejecución. En el mismo comando se genera el informe de
	 * cobertura con Istanbul, en formato "Cobertura" que Jenkins puede procesar. El
	 * reporte queda almacenado en coverage.xml en la ruta de ejecución.
	 **/

	public void execute() {

		def commandlist =
			[
				"istanbul cover --report cobertura --include-all-sources true _mocha -- -R xunit-file"
			];
		
		def dirs = [];
		this.parentWorkspace.eachFile(FileType.DIRECTORIES) {
			dirs << it.name
		}
		
		// Si existe algún directorio test lanzaremos Mocha
		if ( dirs.indexOf("test") != -1 ) {
			commandlist.each {
				CommandLineHelper buildCommandLineHelper = new CommandLineHelper(it);
				buildCommandLineHelper.initLogger(this);
				def returnCode = buildCommandLineHelper.execute(this.parentWorkspace);
	
				if(returnCode != 0) {
					throw new Exception("Error al ejecutar el comando ${it}. Código -> ${returnCode}");
				} else {
					new File("target/site/cobertura").mkdirs();
					new File("target/surefire-reports").mkdirs();
					File file_mocha = new File("xunit.xml");
					File file_istanbul = new File("coverage/cobertura-coverage.xml");
					boolean mochaMoved = file_mocha.renameTo(new File("target/surefire-reports", "xunit.xml"));
					boolean istanbulMoved = file_istanbul.renameTo(new File("target/site/cobertura", "coverage.xml"));
					
					if ( !mochaMoved )
						throw new Exception("Error al mover el fichero de test de Mocha");
					if ( !istanbulMoved )
						throw new Exception("Error al mover el fichero de cobertura de Istanbul");
				}
			}
		} else { // en caso contrario, warning sobre la inexistencia de tests.
			this.log("### No hay directorio test en la ruta de ejecución de Mocha: ${this.parentWorkspace} ###")
		}
		
		
	}

	public void setParentWorkspace(File parentWorkspace) {
		this.parentWorkspace = parentWorkspace;
	}
	
}