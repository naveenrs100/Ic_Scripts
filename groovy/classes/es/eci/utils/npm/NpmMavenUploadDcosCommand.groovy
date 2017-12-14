package es.eci.utils.npm

import es.eci.utils.NexusHelper
import es.eci.utils.base.Loggable
import static groovy.io.FileType.FILES


/**
 * Esta clase modela el Comando para el upload a Nexus con maven
 *
 * <br/>
 * Parámetros de entrada:<br/>
 * <br/>
 * --- OBLIGATORIOS<br/>
 * <b>parentWorkspace</b> Directorio de ejecución<br/>
 * <br/>
 * <b>maven</b> ejecutable maven (ejemplo ejemplo mvn si esta en el path) <br/>
 * <br/>
 * <b>groupId</b> grupo del artefacto que se subirá a nexus<br/>
 * <br/>
 * <b>artifactId</b> nombde del artifact<br/>
 * <br/>
 * <b>nexusPath</b> url completa de nexus para subir la versión<br/>
 *
*/

class NpmMavenUploadDcosCommand extends VersionCommand {

	protected String maven = ""
	protected String fileroute = ""
	protected String nexusPathOpen = ""
	protected String nexusPathClosed = ""
	protected String version  = ""
	
	public String getMaven(){
		return maven;
	}

	public void setMaven(String maven){
		this.maven=maven;
	}
	
	public String getVersion(){
		return version;
	}

	public void setVersion(String version){
		this.version=version;
	}

	/**
	 * @return the nexusPathOpen
	 */
	public String getNexusPathOpen() {
		return nexusPathOpen;
	}

	/**
	 * @param nexusPathOpen the nexusPathOpen to set
	 */
	public void setNexusPathOpen(String nexusPathOpen) {
		this.nexusPathOpen = nexusPathOpen;
	}

	/**
	 * @return the nexusPathClosed
	 */
	public String getNexusPathClosed() {
		return nexusPathClosed;
	}

	/**
	 * @param nexusPathClosed the nexusPathClosed to set
	 */
	public void setNexusPathClosed(String nexusPathClosed) {
		this.nexusPathClosed = nexusPathClosed;
	}

	public void execute() {
		
		log "execute NPM Maven Upload Command"

		log "maven :${maven}"

		// find for version
		def parentWorkspace = this.getParentWorkspace()
		def hVersion = new NpmVersionHelper()
		
		//Si la version esta vacia no es parametro y la leo desde el package.json
		if ("".equals(version)|| version == null){
			def obj = hVersion.getVersionFile(parentWorkspace, this.fileVersion)
			version=obj.version
		}
		
		def nexusPath=""
		
		if(!hVersion.isOpen(version))
			nexusPath=nexusPathClosed
		else
			nexusPath=nexusPathOpen
						
		version = hVersion.addPaddingToVersion(version)
			
		def filepath="${parentWorkspace}/dcos-service.json"
			
		log "filepath : ${filepath}"

		def returnCode = NexusHelper.uploadToNexus(
			maven, groupId, artifactId, version, 
			filepath, nexusPath, "json", this.logger?.logger);
		
		if( returnCode != 0) {
			throw new Exception("Error al ejecutar uploadToNexus . Código -> ${returnCode}");
		}
		
	}
}
