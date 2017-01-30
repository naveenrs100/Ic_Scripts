package es.eci.utils.npm

import java.io.File;
import es.eci.utils.base.Loggable
import es.eci.utils.commandline.CommandLineHelper;

/**
 */
public class NpmInstallCommand extends Loggable {


	protected File parentWorkspace;

	/**
	 * ejecuta la compilacón en npm dentro el parentWorkspace
	 * 
	 * npm install
	 * 
	 * grunt package (si encuentra un gruntfile.js)
	 *  
	 */

	public void execute() {


		def commandlist =
				[
					"npm install"
//					, "npm prune --production"
				];
		
		if (NpmUtils.existsGruntfile(this.parentWorkspace)) {
			commandlist << "grunt package";
		}
		
		commandlist.each {
			CommandLineHelper buildCommandLineHelper = new CommandLineHelper(it);
			buildCommandLineHelper.initLogger(this);
			def returnCode = buildCommandLineHelper.execute(this.parentWorkspace);

			if(returnCode != 0) {
				throw new Exception("Error al ejecutar el comando ${it}. Código -> ${returnCode}");
			}
		}
	}

	public void setParentWorkspace(File parentWorkspace) {
		this.parentWorkspace = parentWorkspace;
	}
}
