package es.eci.utils.npm

import java.io.File;
import es.eci.utils.base.Loggable
import es.eci.utils.commandline.CommandLineHelper;

/**
 
 */
public class NpmCompileCommand extends Loggable {
	

	protected File parentWorkspace;	
	
	/**
	 * ejecuta la compilacón en npm dentro el parentWorkspace
	 * 
	 * npm install
	 * 
	 * bash compiladorMiTarjeta.sh package
	 *  
	 */
	
	public void execute() {		
		
		def exec_folder = "${this.parentWorkspace}";

		def commandlist =
		 [


			"grunt package"
			].each {
						CommandLineHelper buildCommandLineHelper = new CommandLineHelper(it);	
						buildCommandLineHelper.initLogger(this);
						def returnCode = buildCommandLineHelper.execute(new File(exec_folder));
		
						if(returnCode != 0) {
							throw new Exception("Error al ejecutar el comando ${it}. Código -> ${returnCode}");
						}
					}
	
	}

	public void setParentWorkspace(File parentWorkspace) {
		this.parentWorkspace = parentWorkspace;
	}

}
