package components

import es.eci.utils.pom.MavenCoordinates

/**
 * Los procesos de IC se apoyan en la clase MavenComponent para componer
 * el grafo de dependencias.  Cada componente maven tiene varios artefactos,
 * y puede depender de otros componentes.
 */
class MavenComponent implements Comparable<MavenComponent>{

	//--------------------------------------------------------------
	// Propiedades de la clase
	
	// Un componente maven viene definido por su nombre
	private String name;
	// Un componente maven contiene varios artefactos con distintas coordenadas
	private List<MavenCoordinates> coordinates;
	// Un componente maven define dependencias de otros componentes
	private List<MavenComponent> dependencies;
	
	//--------------------------------------------------------------
	// Métodos de la clase
	
	@Override
	public int compareTo(MavenComponent comp) {
		// De este método depende la creación del grafo de dependencias
		// Si no hay relación de dependencia entre componentes, debe devolver cero
		// Si this depende directa o transitivamente de comp, debe devolver > 0
		// Si comp depende directa o transitivamente de this, debe devolver < 0
		int ret = 0;
		if (!this.name.equals(comp.name)) {
			if (dependsOn(this, comp)) {
				ret = 1;
			}
			else if (dependsOn(comp, this)) {
				ret = -1;
			}
		}
		return ret;
	}

	@Override
	public boolean equals(Object o) {
		return this.name.equals(o.name);
	}
	/**
	 * Devuelve el nombre del componente.
	 * @return name
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Construye un componente maven a partir de sus coordenadas.
	 * @param name Nombre del componente
	 */
	public MavenComponent(String name) {
		this.name = name;
		this.coordinates = [];
		this.dependencies = []
	}
	
	/**
	 * Añade un artefacto maven al componente.
	 * @param coords Coordenadas del artefacto
	 */
	public void addArtifact(MavenCoordinates coords) {
		if (!coordinates.contains(coords)) {
			this.coordinates << coords;
		}
	}
	
	/** @return Devuelve las coordenadas del objeto */
	public List<MavenCoordinates> getCoordinates() {
		return this.coordinates;
	}
	
	/**
	 * Añade una dependencia al componente.  
	 * @param comp Componente del que depende this
	 * @throws Exception si se le intenta hacer depender de sí mismo.
	 * @throws Exception si se le intenta inducir una dependencia circular
	 * con uno o más componentes
	 */
	public void addDependency(MavenComponent comp) {
		if (comp.equals(this)) {
			throw new Exception("Un componente no puede depender de sí mismo");
		}
		if (checkCircularDependencies(comp)) {
			throw new Exception("Se ha intentado crear una dependencia circular");
		}
		if (!this.dependencies.contains(comp)) {
			this.dependencies << comp;
		}
	} 
	
	// Este método nos indica si comp depende de this, o bien alguna de las dependencias
	//	transitivas de comp depende de this
	private boolean checkCircularDependencies(MavenComponent comp) {
		return dependsOn(comp, this);
	}
	
	/**
	 * Indica si un artefacto pertenece al componente.
	 * @param coords Coordenadas maven del artefacto
	 * @return Cierto si forma parte del componente, falso en otro caso
	 */
	public boolean contains(MavenCoordinates coords) {
		return this.coordinates.contains(coords);
	}
	
	/**
	 * Devuelve la lista resuelta (transitividad incluida) de componentes maven
	 * de los que depende un componente dado.
	 * @return Lista de componentes de los que depende este componente
	 */
	public List<MavenComponent> getDependencies() {
		List<MavenComponent> deps = []
		
		this.dependencies?.each { MavenComponent comp ->
			if (!deps.contains(comp)) {
				deps << comp;
				deps.addAll(comp.getDependencies());
			}
		}
		
		return deps;
	}
	
	/**
	 * Este método indica si dependency se encuentra entre las dependencias de comp
	 * @param comp Componente maven
	 * @param dependency Dependencia que queremos comprobar
	 * @return Cierto si comp depende de dependency
	 */
	public static boolean dependsOn(MavenComponent comp, MavenComponent dependency) {
		if (comp.dependencies == null || comp.dependencies.size() == 0) {
			return false;
		}
		else if (comp.dependencies.contains(dependency)) {
			return true;
		}
		else {
			boolean ret = false;			
			comp.dependencies.each { MavenComponent dep ->
				ret |= dependsOn(dep, dependency);
			}
			return ret;
		}
	}
	
	@Override
	public String toString() {
		return name;
	}
}
