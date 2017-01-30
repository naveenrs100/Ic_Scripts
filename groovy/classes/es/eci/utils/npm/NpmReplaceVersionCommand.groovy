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
 * <b>tag</b> se remplaza la versión por el tag de git<br/>
 * <br/>
 * --- OPCIONALES<br/>
 * <b>filename</b> Nombre del fichero de configuración<br/>
 */




class NpmReplaceVersionCommand extends VersionCommand {
	
	
	protected String tag =""
	
	public String getTag(){
		return tag;
	}
	
	public void setTag(String tag){
		this.tag=tag;
	}
	
	public void execute() {
		def parentWorkspace = this.getParentWorkspace()	
		def hVersion = new NpmVersionHelper()
		def obj = hVersion.getVersionFile(parentWorkspace, this.fileVersion)

		if("".equals(tag) || tag == null || !hVersion.isNpmValidVersion(tag)) {
			throw new Exception("Error al ejecutar el comando NpmReplaceVersionCommand. tag  -> ${tag}");
		}
		
		obj.version=tag
		
		hVersion.saveFile(parentWorkspace, this.fileVersion,obj)
		hVersion.createVersionFile(parentWorkspace.absolutePath, tag, groupId)
			
	}
}
