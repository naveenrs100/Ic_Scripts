/**
 * 
 */
package es.eci.utils.pom

/**
 * Esta clase modela un nodo en un árbol de poms.
 */
class PomNode {

	//---------------------------------------------
	// Propiedades de la clase
	
	// Fichero
	private File file;
	// Padre
	private PomNode parent;
	// Hijos
	private List<PomNode> children;
	// Propiedades
	private Map<String, String> properties;
	// Coordenadas Maven
	private MavenCoordinates coordinates = null;
	
	//---------------------------------------------
	// Métodos de la clase
	
	/** Construye un nodo a partir de su padre */
	public PomNode(PomNode parent, File file) {
		this.parent = parent;
		this.file = file;
		children = null;
		properties = new HashMap<String, String>();
		// Leer las propiedades
		def pom = new XmlParser().parseText(file.text);
		if (pom.properties != null && pom.properties.size() > 0) {
			pom.properties[0].children().each {
				if ((it.name() instanceof groovy.xml.QName)) {
					properties[it.name().localPart] = it.text();
				}
				else if ((it.name() instanceof java.lang.String)) {
					properties[it.name()] = it.text();
				}
			}
		}
		// Leer las coordenadas
		coordinates = MavenCoordinates.readPom(pom);
	}
	
	/** 
	 * Añade un hijo al nodo
	 * @param child Nodo hijo
	 */
	public void addChild(PomNode child) {
		if (child != null) {
			if (children == null) {
				children = new LinkedList<PomNode>();
			}
			children << child;
		}
	}
	
	/**
	 * Devuelve una copia defensiva de la lista de hijos
	 * @return Lista de hijos
	 */
	public List<PomNode> getChildren() {
		if (children == null) {
			return null
		}
		else {
			return new LinkedList<PomNode>(children);
		}
	}
	
	@Override
	public String toString() {
		return file?.getCanonicalPath()
	}

	/**
	 * @return the file
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @return the parent
	 */
	public PomNode getParent() {
		return parent;
	}

	/**
	 * @return Coordenadas Maven del pom
	 */
	public MavenCoordinates getCoordinates() {
		return coordinates;
	}

	/**
	 * @return Propiedades definidas en el nodo
	 */
	public Map<String, String> getProperties() {
		return properties;
	}
	
	
	
	
}
