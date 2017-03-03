package vs

import java.util.List;

class C_VS_aplicacion {

	private String groupId;
	private String artifactId;
	private String version;
	// Entorno de compilación (eVC4, VS2005, VS60)
	private String ide;   
	// ¿Se debe generar el binario en debug?
	private Boolean debug; 
	// Lista de plataformas para las que se genera el binario.  Deben corresponderse
	//	con los identificadores de los SDKs manejados por Visual Studio y, a su vez,
	//	con los targets definidos en los ficheros makefile y .vcproj
	private List<String> platforms = [];

	// Getters y setters
		public String getGroupId() {
			return groupId;
		}
		
		public void setGroupId(String groupId) {
			this.groupId = groupId;
		}
		
		public String getArtifactId() {
			return artifactId;
		}
		
		public void setArtifactId(String artifactId) {
			this.artifactId = artifactId;
		}
		
		public String getVersion() {
			return version;
		}
		
		public void setVersion(String version) {
			this.version = version;
		}
		
		public String getIde() {
			return ide;
		}
		
		public void setIde(String ide) {
			this.ide = ide;
		}
		
		public Boolean isDebug() {
			return debug;
		}
		
		public void setDebug(Boolean debug) {
			this.debug = debug;
		}
		
		public List<String> getPlatforms() {
			return platforms;
		}
		
		public void addPlatform(String platform) {
			platforms << platform
		}
}