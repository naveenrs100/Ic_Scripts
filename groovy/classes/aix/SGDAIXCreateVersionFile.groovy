package aix

import es.eci.utils.base.Loggable;
import es.eci.utils.ComponentVersionHelper
import es.eci.utils.NexusHelper
import es.eci.utils.ParameterValidator;
import es.eci.utils.Stopwatch;
import es.eci.utils.Utiles;

class SGDAIXCreateVersionFile extends Loggable {
	
	private String stream;
	private String component;
	private String groupId;
	private String workspace;
	private String urlRTC;
	private String userRTC;
	private String pwdRTC;
	
	public void execute() {
		
		// Validación de obligatorios
		ParameterValidator.builder()
			.add("stream", stream)
			.add("component", component)
			.add("groupId", groupId)
			.add("workspace", workspace)
			.add("urlRTC", urlRTC)
			.add("userRTC", userRTC)
			.add("pwdRTC", pwdRTC).build().validate();
		
		long millis = Stopwatch.watch {
			
			ComponentVersionHelper componentVersionHelper = new ComponentVersionHelper()
			String version = componentVersionHelper.getVersion(component, stream, userRTC, pwdRTC, urlRTC)
			
			// Almacenar la versión en el fichero version.txt
			Utiles.creaVersionTxt(version, groupId, workspace.toString())
			
		}
		
	}
	
	/**
	 * @return the stream
	 */
	public String getStream() {
		return stream;
	}
	
	/**
	 * @param stream the stream to set
	 */
	public void setStream(String stream) {
		this.stream = stream;
	}
	
	/**
	 * @return the component
	 */
	public String getComponent() {
		return component;
	}

	/**
	 * @param component the component to set
	 */
	public void setComponent(String component) {
		this.component = component;
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
	 * @return the urlRTC
	 */
	public String getUrlRTC() {
		return urlRTC;
	}
	
	/**
	 * @param urlRTC the urlRTC to set
	 */
	public void setUrlRTC(String urlRTC) {
		this.urlRTC = urlRTC;
	}
	
	/**
	 * @return the userRTC
	 */
	public String getUserRTC() {
		return userRTC;
	}
	
	/**
	 * @param userRTC the userRTC to set
	 */
	public void setUserRTC(String userRTC) {
		this.userRTC = userRTC;
	}
	
	/**
	 * @return the pwdRTC
	 */
	public String getPwdRTC() {
		return pwdRTC;
	}
	
	/**
	 * @param pwdRTC the pwdRTC to set
	 */
	public void setPwdRTC(String pwdRTC) {
		this.pwdRTC = pwdRTC;
	}

}
