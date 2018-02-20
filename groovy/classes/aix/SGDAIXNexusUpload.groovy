package aix

import es.eci.utils.base.Loggable;
import es.eci.utils.ComponentVersionHelper
import es.eci.utils.NexusHelper
import es.eci.utils.ParameterValidator;
import es.eci.utils.Stopwatch;
import es.eci.utils.Utiles;

class SGDAIXNexusUpload extends Loggable {
	
	private String artifactId;
	private String groupId;
	private String workspace;
	private String urlNexusC;
	private String nexusSnapshotsC;
	private String nexusReleaseC;
	private String userNexus;
	private String pwdNexus;
	private String gradleHome;
	private String scriptsHome;
	private String builtVersion;
	
	public void execute() {
		
		// Validación de obligatorios
		ParameterValidator.builder()
			.add("artifactId", artifactId)
			.add("groupId", groupId)
			.add("workspace", workspace)
			.add("urlNexusC", urlNexusC)
			.add("nexusSnapshotsC", nexusSnapshotsC)
			.add("nexusReleaseC", nexusReleaseC)
			.add("userNexus", userNexus)
			.add("pwdNexus", pwdNexus)
			.add("gradleHome", gradleHome)
			.add("scriptsHome", scriptsHome)
			.add("builtVersion", builtVersion).build().validate();
		
		long millis = Stopwatch.watch {
						
			String destinoNexus = nexusReleaseC
			String isRelease = "true"
			
			// Cambio de destino
			if (builtVersion.endsWith("-SNAPSHOT")) {
				destinoNexus = nexusSnapshotsC
				isRelease = "false"
			}
			
			// Ruta del tar
			String artifactPath = workspace + '/' + artifactId + '.tar'
			
			// Gradle
			String gradleBin = gradleHome + "/bin/gradle"
			
			NexusHelper.uploadTarNexus(userNexus, pwdNexus, gradleBin, scriptsHome, 
				urlNexusC, groupId, artifactId, builtVersion, destinoNexus, isRelease, 
				artifactPath, "tar", { println it })
			
		}
		
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

}
