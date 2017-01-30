package es.eci.utils.npm

import java.io.File;

import es.eci.utils.base.Loggable
import es.eci.utils.commandline.CommandLineHelper;

/**
 
 */
public class NpmCleanCommand extends Loggable {
	

	protected File parentWorkspace;	
	
	
	/**
	 * ejecuta la limpieza de grunt dentro del parentWorkspace
	 * 
	 *  
	 */
	
	public void execute() {		
		
		
		
		def commandlist = []
		
		if (NpmUtils.existsGruntfile(this.parentWorkspace)) {
			commandlist << 	 "grunt clean";
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
