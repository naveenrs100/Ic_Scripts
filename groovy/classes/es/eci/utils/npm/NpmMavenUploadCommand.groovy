package es.eci.utils.npm

//import es.eci.utils.GlobalVars
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
 * <b>nexusPathOpen</b> url completa de nexus para subir la versión abierta<br/>
 * <br/>
 * <b>nexusPathClosed</b> url completa de nexus para subir la versión cerrada<br/>
 * <br/>
 * <b>type</b> extension del fichero<br/>
 * <br/>
 * <b>distFolder</b> carpeta donde se encuentra el fichero generado<br/>
 * <br/>
 * 
 *
*/

class NpmMavenUploadCommand extends VersionCommand {

	protected String maven = ""
	protected String fileroute = ""
	protected String nexusPathOpen = ""
	protected String nexusPathClosed = ""
	protected String nexusPath = ""
	protected String type  = ""
	protected String version  = ""
	protected String distFolder = ""
	
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
	
	public void setDistFolder(String distFolder){		
		this.distFolder=distFolder;
	}
	
	public String getDistFolder(){
		return distFolder;
	}

	public void setNexusPathOpen(String np){
		this.nexusPathOpen=np;
	}
	
	public void setNexusPathClosed(String np){
		this.nexusPathClosed=np;
	}

	public void setType(String type){
		this.type=type;
	}

		
	public void execute() {
		
		log "execute NPM Maven Upload Command"

		log "maven :${maven}"
		log "distFolder :${distFolder}"
		// find for version
		def parentWorkspace = this.getParentWorkspace()
		def hVersion = new NpmVersionHelper()
		
		
		
		
		//Si la version esta vacia no es parametro y la leo desde el package.json
		if ("".equals(version)|| version == null){
			def obj = hVersion.getVersionFile(parentWorkspace, this.fileVersion)
			version=obj.version
		}
						
		version = hVersion.addPaddingToVersion(version)
	
		if(!hVersion.isOpen(version))
			nexusPath=nexusPathClosed
		else 
			nexusPath=nexusPathOpen 
		
		if (distFolder == null || "".equals(distFolder))
			fileroute = "${parentWorkspace}/"
		else 
			fileroute = "${parentWorkspace}/${distFolder}/"
			
			
		def filepath=""
		// we need the first file with the correct extension
		new File(fileroute).eachFile(FILES) {
			if("".equals(filepath) && it.name.endsWith(".${type}")) {
				filepath="${fileroute}/${it.name}"
			}
		}
		
		if("".equals(filepath)) {
			throw new Exception("Error al ejecutar uploadToNexus . file not found!");
		}
			
		log "filepath  : ${filepath}"

		def returnCode = NexusHelper.uploadToNexus(
			maven, groupId, artifactId, version, 
			filepath, nexusPath, type, this.logger?.logger);
		
		if( returnCode != 0) {
			throw new Exception("Error al ejecutar uploadToNexus . Código -> ${returnCode}");
		}
		
	}
}
