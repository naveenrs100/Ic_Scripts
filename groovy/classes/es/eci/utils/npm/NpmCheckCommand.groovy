package es.eci.utils.npm

import es.eci.utils.base.Loggable

/**
 * Esta clase modela el Comando para Cerrar una versión 
 * 
 * <br/>
 * Parámetros de entrada:<br/>
 * <br/>
 * --- OBLIGATORIOS<br/>
 * <b>parentWorkspace</b> Directorio de ejecución<br/>
 * <br/>
 * --- OPCIONALES<br/>
 * <b>filename</b> Nombre del fichero de configuración<br/>
 */

class NpmCheckCommand extends VersionCommand {
	
	public void execute() {
		
		println "execute NPM Check Command"
		
		def parentWorkspace = this.getParentWorkspace()	
		def hVersion = new NpmVersionHelper()
		def obj = hVersion.getVersionFile(parentWorkspace, this.fileVersion)
		def version = obj.version
		//def groupId = obj.groupId
		def groupId = this.getGroupId()
		version = hVersion.addPaddingToVersion(version)
		

		if(!hVersion.isOpen(version))
			throw new Exception("We are expectiong an Open Version but we received :"+version)
			
		obj.version=version
		hVersion.saveFile(parentWorkspace, this.fileVersion,obj)
		hVersion.createVersionFile(parentWorkspace.absolutePath, version, groupId)
			
	}
}
