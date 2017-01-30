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




class CreateVersionFileCommand extends VersionCommand {
	
	
	protected String tag =""
	protected String groupId =""
	
	public String getTag(){
		return tag;
	}
	
	public void setTag(String tag){
		this.tag=tag;
	}
		
	public void setGroupId(String tag){
		this.groupId=groupId;
	}
	
	public String getGroupId(){
		return groupId;
	}
	

	
	public void execute() {
		def parentWorkspace = this.getParentWorkspace()	
		def hVersion = new NpmVersionHelper()

		if("".equals(tag) || tag == null || !hVersion.isNpmValidVersion(tag)) {
			throw new Exception("Error al ejecutar el comando NpmReplaceVersionCommand. tag  -> ${tag}");
		}

		hVersion.createVersionFile(parentWorkspace.absolutePath, tag, groupId)
			
	}
}
