package aix

import es.eci.utils.base.Loggable;
import es.eci.utils.versioner.XmlUtils
import es.eci.utils.ComponentVersionHelper
import es.eci.utils.ParameterValidator;
import es.eci.utils.Stopwatch;
import es.eci.utils.Utiles;

class SGDAIXVersioner extends Loggable {
	
	private String builtVersion;
	private String groupId;
	private String workspace;
	private String task;
	
	public void execute() {
		
		// Validación de obligatorios
		ParameterValidator.builder()
			.add("builtVersion", builtVersion)
			.add("groupId", groupId)
			.add("workspace", workspace)
			.add("task", task).build().validate();
		
		long millis = Stopwatch.watch {
			
			// Cerramos versión para la release
			if ( task == "close" ) {
				
				builtVersion = builtVersion.split("-SNAPSHOT")[0];
				
				// Almacenar la versión en el fichero version.txt
				Utiles.creaVersionTxt(builtVersion, groupId, workspace.toString())
				
			}
			
			if ( task == "open" ) {
				
				builtVersion = builtVersion + "-SNAPSHOT";
				
				// Almacenar la versión en el fichero version.txt
				Utiles.creaVersionTxt(builtVersion, groupId, workspace.toString())
				
			}
			
			if ( task == "openAndIncrease" ) {
				
				XmlUtils utils = new XmlUtils();
				String newVersion = utils.increaseVersionDigit(builtVersion, "release", "false");
				
				// Almacenar la versión en el fichero version.txt
				Utiles.creaVersionTxt(newVersion, groupId, workspace.toString())
				
			}
			
		}
		
	}

	/**
	 * @return the builtVersion
	 */
	public String getBuiltVersion() {
		return builtVersion;
	}

	/**
	 * @param builtVersion the builtVersion to set
	 */
	public void setBuiltVersion(String builtVersion) {
		this.builtVersion = builtVersion;
	}

	/**
	 * @return the groupId
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * @param groupId the groupId to set
	 */
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	/**
	 * @return the workspace
	 */
	public String getWorkspace() {
		return workspace;
	}

	/**
	 * @param workspace the workspace to set
	 */
	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}

	/**
	 * @return the task
	 */
	public String getTask() {
		return task;
	}

	/**
	 * @param task the task to set
	 */
	public void setTask(String task) {
		this.task = task;
	}

}
