package es.eci.utils.pom

/**
 * Esta clase modela un árbol de poms
 */
class PomTree {

	//-----------------------------------------
	// Propiedades de la clase
	
	// Raíz
	private PomNode root = null;
	
	//-----------------------------------------
	// Métodos de la clase
	
	/**
	 * Construye un árbol a partir de un directorio
	 * @param folder Directorio de inicio
	 */
	public PomTree(File folder) {
		root = builderI(null, folder);
	}
	
	/** 
	 * Inmersión recursiva de la construcción
	 */
	private PomNode builderI(PomNode parent, File folder) {
		PomNode ret = null;
		File pom = getPom(folder)
		if (pom != null) {
			ret = new PomNode(parent, pom)
			List<String> modules = readModules(pom)
			if (modules != null) {
				modules.each { module ->
					if (module != null && module.trim().length() > 0) {
						ret.addChild(builderI(ret, new File(folder, module)))
					}
				}
			}
		}
		return ret;
	}
	
	/**
	 * Busca un pom.xml en un directorio
	 * @param folder Directorio
	 * @return Fichero pom.xml si existe
	 */
	private File getPom(File folder) {
		File ret = null
		if (folder.exists() && folder.isDirectory()) {
			File search = new File(folder.getCanonicalPath() + System.getProperty("file.separator") + "pom.xml")
			if (search.exists() && search.isFile()) {
				ret = search
			}
		}
		return ret
	}
	
	/**
	 * Busca la lista de módulos en un pom
	 * @param pom Fichero pom
	 * @return Lista de módulos
	 */
	private List<String> readModules(File pom) {
		List<String> ret = null;
		try {
			Node xml = new XmlParser().parse(pom)
			if (xml.modules != null && xml.modules.module != null) {
				ret = new LinkedList<String>()
				xml.modules.module.each { module ->
					if (module.text() != null) {
						ret << module.text()
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	/**
	 * Iterador en anchura
	 * @return Iterador en anchura
	 */
	public Iterator<PomNode> widthIterator() {
		return new WidthIterator(root)
	}
	
	/** Iterador en profundidad. */
	public Iterator<PomNode> depthIterator() {
		return new DepthIterator(root)
	}
	
	// Iterador en anchura
	private class WidthIterator implements Iterator<PomNode> {
		// Lista de nodos
		private ArrayList<PomNode> nodes;
		// Índice
		private int currentIndex = 0;
		
		// Constructor desde un elemento
		public WidthIterator(PomNode root) {
			nodes = new ArrayList()
			if (root != null) {
				nodes << root
				childrenI(root, nodes)
			}
		}
		
		// Inmersión recursiva (anchura)
		private void childrenI(PomNode root, List<PomNode> nodes) {
			if (root != null) {
				if (root.getChildren() != null) {
					root.getChildren().each { child ->
						if (child != null) {
							nodes << child
						}
					}
					root.getChildren().each { child ->
						childrenI(child, nodes)						
					}
				}
			}
		}
		
		@Override
		public boolean hasNext() {
			return currentIndex < (nodes.size())
		}
		@Override
		public PomNode next() {
			return nodes.get(currentIndex ++)
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException()			
		}
	}
	
	// Iterador en profundidad
	private class DepthIterator implements Iterator<PomNode> {
		// Lista de nodos
		private ArrayList<PomNode> nodes;
		// Índice
		private int currentIndex = 0;
		
		// Constructor desde un elemento
		public DepthIterator(PomNode root) {
			nodes = new ArrayList<PomNode>()
			childrenI(root, nodes)
		}
		
		// Inmersión recursiva (profundidad)
		private void childrenI(PomNode root, List<PomNode> nodes) {
			if (root != null) {
				nodes << root
				if (root.getChildren() != null) {
					root.getChildren().each { child ->
						if (child != null) {
							childrenI(child, nodes)
						}
					}
				}
			}
		}
		
		@Override
		public boolean hasNext() {
			return currentIndex < (nodes.size())
		}
		@Override
		public PomNode next() {
			return nodes.get(currentIndex ++)
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException()			
		}
	}
}
