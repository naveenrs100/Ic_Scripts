package aix

import es.eci.utils.base.Loggable;
import es.eci.utils.ComponentVersionHelper
import es.eci.utils.NexusHelper
import es.eci.utils.ParameterValidator;
import es.eci.utils.Stopwatch;
import es.eci.utils.Utiles;

class SGDAIXNexusUpload extends Loggable {
	
	private String stream;
	private String artifactId;
	private String groupId;
	private String workspace;
	private String urlRTC;
	private String userRTC;
	private String pwdRTC;
	private String urlNexusC;
	private String nexusSnapshotsC;
	private String nexusReleaseC;
	private String userNexus;
	private String pwdNexus;
	private String gradleHome;
	private String scriptsHome;	
	
	public void execute() {
		
		// Validación de obligatorios
		ParameterValidator.builder()
			.add("stream", stream)
			.add("artifactId", artifactId)
			.add("groupId", groupId)
			.add("workspace", workspace)
			.add("urlRTC", urlRTC)
			.add("userRTC", userRTC)
			.add("pwdRTC", pwdRTC)
			.add("urlNexusC", urlNexusC)
			.add("nexusSnapshotsC", nexusSnapshotsC)
			.add("nexusReleaseC", nexusReleaseC)
			.add("userNexus", userNexus)
			.add("pwdNexus", pwdNexus)
			.add("gradleHome", gradleHome)
			.add("scriptsHome", scriptsHome).build().validate();
		
		long millis = Stopwatch.watch {
			
			ComponentVersionHelper componentVersionHelper = new ComponentVersionHelper()
			String version = componentVersionHelper.getVersion(artifactId, stream, userRTC, pwdRTC, urlRTC)
			
			// Almacenar la versión en el fichero version.txt
			Utiles.creaVersionTxt(version, groupId, workspace.toString())
			
			String destinoNexus = nexusReleaseC
			boolean isRelease = true;
			
			// Cambio de destino
			if (version.endsWith("-SNAPSHOT")) {
				destinoNexus = nexusSnapshotsC
				isRelease = false
			}
			
			// Ruta del tar
			String artifactPath = workspace + '/' + artifactId + '.tar'
			File filePath = new File(artifactPath)
			
			log "--- INFO: Subiendo a Nexus..."
			log "--- R: ${destinoNexus}"
			log "--- G: ${groupId}"
			log "--- A: ${artifactId}"
			log "--- V: ${version}"
			log "--- P: tar"
			
			String gradleBin = gradleHome + "/bin/gradle"
			
			NexusHelper.uploadTarNexus(userNexus, pwdNexus, gradleBin, scriptsHome, 
				urlNexusC, groupId, artifactId, version, destinoNexus, isRelease, 
				filePath, "tar")
			
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
	 * @return the artifactId
	 */
	public String getArtifactId() {
		return artifactId;
	}
	
	/**
	 * @param artifactId the artifactId to set
	 */
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
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
	
	/**
	 * @return the urlNexusC
	 */
	public String getUrlNexusC() {
		return urlNexusC;
	}
	
	/**
	 * @param urlNexusC the urlNexusC to set
	 */
	public void setUrlNexusC(String urlNexusC) {
		this.urlNexusC = urlNexusC;
	}

	/**
	 * @return the nexusSnapshotsC
	 */
	public String getNexusSnapshotsC() {
		return nexusSnapshotsC;
	}

	/**
	 * @param nexusSnapshotsC the nexusSnapshotsC to set
	 */
	public void setNexusSnapshotsC(String nexusSnapshotsC) {
		this.nexusSnapshotsC = nexusSnapshotsC;
	}

	/**
	 * @return the nexusReleaseC
	 */
	public String getNexusReleaseC() {
		return nexusReleaseC;
	}

	/**
	 * @param nexusReleaseC the nexusReleaseC to set
	 */
	public void setNexusReleaseC(String nexusReleaseC) {
		this.nexusReleaseC = nexusReleaseC;
	}

	/**
	 * @return the userNexus
	 */
	public String getUserNexus() {
		return userNexus;
	}

	/**
	 * @param userNexus the userNexus to set
	 */
	public void setUserNexus(String userNexus) {
		this.userNexus = userNexus;
	}

	/**
	 * @return the pwdNexus
	 */
	public String getPwdNexus() {
		return pwdNexus;
	}

	/**
	 * @param pwdNexus the pwdNexus to set
	 */
	public void setPwdNexus(String pwdNexus) {
		this.pwdNexus = pwdNexus;
	}

	/**
	 * @return the gradleHome
	 */
	public String getGradleHome() {
		return gradleHome;
	}

	/**
	 * @param gradleHome the gradleHome to set
	 */
	public void setGradleHome(String gradleHome) {
		this.gradleHome = gradleHome;
	}

	/**
	 * @return the scriptsHome
	 */
	public String getScriptsHome() {
		return scriptsHome;
	}

	/**
	 * @param scriptsHome the scriptsHome to set
	 */
	public void setScriptsHome(String scriptsHome) {
		this.scriptsHome = scriptsHome;
	}

}
