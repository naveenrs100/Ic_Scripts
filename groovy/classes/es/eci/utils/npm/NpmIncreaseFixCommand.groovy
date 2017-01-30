package es.eci.utils.npm

class NpmIncreaseFixCommand extends VersionCommand {

	/**
	 * Esta clase modela el Comando para Incrementar la versión de un Fix
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
	
		public void execute() {
		def parentWorkspace = this.getParentWorkspace()
		def hVersion = new NpmVersionHelper()
		def obj = hVersion.getVersionFile(parentWorkspace, this.fileVersion)
		def version = obj.version
		def groupId = this.getGroupId()
		version = hVersion.addPaddingToVersion(version)
		
		if(hVersion.isClosed(version))
			version= hVersion.incrementFix(version)
		else
			throw new Exception("We are expectiong a Closed Version but we received :"+version)

		obj.version=version
		hVersion.saveFile(parentWorkspace, this.fileVersion,obj)
		hVersion.createVersionFile(parentWorkspace.absolutePath, version, groupId)
			
	}
}
