package es.eci.utils.npm

import es.eci.utils.base.Loggable;

abstract class VersionCommand extends Loggable {
	
	/**
	 * Esta clase Abstracta de la que tienen que extender los Comandos
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

	protected File parentWorkspace;
	protected String artifactId;
	protected String groupId;
	

	/**
	 * Este método busca en el workspace los ficheros
	 * 
	 * package.json
	 * npm-shrinkwrap.json
	 * 
	 * Por este orden, devolviendo el fichero que exista.
	 * @return
	 */
	public String getFileVersion() {
		String ret = null;
		File p = new File(parentWorkspace, "package.json");
		if (p.exists()) {
			ret = p.getName();
		}
		else {
			File s = new File(parentWorkspace, "npm-shrinkwrap.json");
			if (s.exists()) {
				ret = s.getName();
			}
		}
		return ret;
	}

	/**
	 * Getter para el Parent Workspace
	 * @return parent Workspace
	 */

	public File getParentWorkspace() {
		return parentWorkspace;
	}

	/**
	 * Setter para el Parent Workspace
	 * @param parent Workspace
	 */
	
	public void setParentWorkspace(File parentWorkspace) {
		this.parentWorkspace = parentWorkspace;
	}

	/**
	 * Getter para el artifactId 
	 * @return artifactId
	 */

	public String getArtifactId() {
		return artifactId;
	}

	/**
	 * Setter para el artifactId
	 * @param artifactId
	 */
	
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}
	
	/**
	 * Getter para el groupId
	 * @return groupId
	 */

	public String getGroupId() {
		return groupId;
	}

	/**
	 * Setter para el groupId
	 * @param groupId
	 */
	
	public void setgroupId(String groupId) {
		this.groupId = groupId;
	}
	
	
	/**
	 * Abstract method to be implemented
	 */
	
	public abstract void execute()
	
}
