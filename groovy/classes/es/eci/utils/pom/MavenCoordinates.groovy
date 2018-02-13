/**
 * 
 */
package es.eci.utils.pom

/**
 * Representa un juego de coordenadas maven
 */
class MavenCoordinates {

	//-----------------------------------------------
	// Propiedades de la clase
	
	// GAV
	private String groupId;
	private String artifactId;
	private String version;
	// Opcionales
	private String packaging = "jar";
	private String classifier;
	private String repository = "public";
	
	//-----------------------------------------------
	// Métodos de la clase
	
	/**
	 * Lee las coordenadas de un pom.xml 
	 * @param pom Estructura xml parseada mediante un XmlParser
	 * @return Coordenadas maven del pom
	 */
	public static MavenCoordinates readPom(def pom) {
		String tmpGroup = null;
		if (pom.groupId != null && pom.groupId.text().trim().length() > 0) {
			tmpGroup = 	pom.groupId.text()
		}
		else {
			// El del padre
			tmpGroup = 	pom.parent.groupId.text()
		}
		String tmpVersion = null;
		if (pom.version != null && pom.version.text().trim().length() > 0) {
			tmpVersion = 	pom.version.text()
		}
		else {
			// El del padre
			tmpVersion = 	pom.parent.version.text()
		}
		String artifactId = pom.artifactId.text();
		String packaging = null;
		if (pom.packaging != null && pom.packaging.text().trim().length() > 0) {
			packaging = pom.packaging.text();
		}
		String classifier = null;
		if (pom.classifier != null && pom.classifier.text().trim().length() > 0) {
			classifier = pom.classifier.text();
		}
		MavenCoordinates ret = new MavenCoordinates(tmpGroup, artifactId, tmpVersion);
		if (classifier != null) {
			ret.setClassifier(classifier);
		}
		if (packaging != null) {
			ret.setPackaging(packaging);
		}
		return ret;
	}
		
	/**
	 * Constructor con GAV y parámetros opcionales
	 * @param theGroupId G
	 * @param theArtifactId A
	 * @param theVersion V
	 */
	public MavenCoordinates(
			String theGroupId, 
			String theArtifactId, 
			String theVersion) {
		groupId = theGroupId;
		artifactId = theArtifactId;
		version = theVersion;
	}
	
	/**
	 * Constructor con GAV y parámetros opcionales
	 * @param theGroupId G
	 * @param theArtifactId A
	 * @param theVersion V
	 * @param thePackaging Empaquetado
	 */
	public MavenCoordinates(
			String theGroupId, 
			String theArtifactId, 
			String theVersion, 
			String thePackaging) {
		groupId = theGroupId;
		artifactId = theArtifactId;
		version = theVersion;
		packaging = thePackaging;
	}

	/**
	 * @return the packaging
	 */
	public String getPackaging() {
		return packaging;
	}

	/**
	 * @param packaging the packaging to set
	 */
	public void setPackaging(String packaging) {
		this.packaging = packaging;
	}

	/**
	 * @return the classifier
	 */
	public String getClassifier() {
		return classifier;
	}

	/**
	 * @param classifier the classifier to set
	 */
	public void setClassifier(String classifier) {
		this.classifier = classifier;
	}
	
	/**
	 * @return the repository
	 */
	public String getRepository() {
		return repository;
	}

	/**
	 * @param repository the repository to set
	 */
	public void setRepository(String repository) {
		this.repository = repository;
	}

	/**
	 * @return the groupId
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * @return the artifactId
	 */
	public String getArtifactId() {
		return artifactId;
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}
	
	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public String toString() {
		String ret = "$groupId:$artifactId:$version";
		if (classifier != null) {
			ret += ":$classifier"
		}
		if (packaging != null) {
			ret += ":$packaging"
		}
		return ret;
	}
	
	@Override
	// Aquí no se tiene en cuenta el parámetro repository.
	public boolean equals(Object o) {
		if (o.getClass().getName().equals(this.getClass().getName())) {
			MavenCoordinates o2 = (MavenCoordinates) o;
			return (groupId == o2.groupId 
					&& artifactId == o2.artifactId
					&& version == o2.version
					&& classifier == o2.classifier
					&& packaging == o2.packaging);
		}
		else {
			return false;
		}
	}
}
