package es.eci.utils.pom

import java.util.List;

import components.MavenComponent;
import es.eci.utils.base.Loggable

/**
 * Esta clase implementa una estrategia para agrupar los componentes maven ordenados
 * y con dependencias, en grupos que se puedan lanzar concurrentemente.
 * 
 * Por ejemplo:
 * 
 * ------------------------------------------------------------------
 * Dada una lista ordenada de objetos MavenComponent
 * 
 * C1 -> C2 -> C3
 * 
 * Con dependencias tales que 
 * 
 * MavenComponent.dependsOn(C2, C1) == true
 * MavenComponent.dependsOn(C3, C2) == true
 * 
 * Por lo tanto, no hay forma de agrupar componentes y la estrategia devuelve
 * 3 grupos:
 * 
 * g1: C1
 * g2: C2
 * g3: C3
 * 
 * que deben lanzarse respetando el mismo orden secuencial que nos sugería la lista
 * 
 * C1 -> C2 -> C3
 * 
 * ------------------------------------------------------------------
 * Dada una lista ordenada de objetos MavenComponent
 * 
 * C1 -> C2 -> C3
 * 
 * Con dependencias tales que 
 * 
 * MavenComponent.dependsOn(C2, C1) == true
 * MavenComponent.dependsOn(C3, C1) == true
 * MavenComponent.dependsOn(C3, C2) == false
 * 
 * Esta estrategia debe sugerirnos dos grupos
 * 
 * g1: C1
 * g2: C2, C3
 * 
 * que deben lanzarse de la forma siguiente
 * 
 * C1 -> [ en paralelo: C2, C3 ] 
 * 
 * De forma que podemos acortar tiempo de construcción al poder hacer C2 y C3 de
 * forma concurrente
 * 
 * ------------------------------------------------------------------
 * Dada una lista ordenada de objetos MavenComponent
 * 
 * C1 -> C2 -> C3 -> C4 -> C5
 * 
 * MavenComponent.dependsOn(C2, C1) == false
 * MavenComponent.dependsOn(C3, C2) == true
 * MavenComponent.dependsOn(C4, C2) == true
 * MavenComponent.dependsOn(C4, C3) == false
 * MavenComponent.dependsOn(C5, C3) == true
 * 
 * Esta estrategia debe sugerirnos tres grupos
 * 
 * g1: C1, C2
 * g2: C3, C4
 * g3: C5
 * 
 * que deben lanzarse de la forma siguiente:
 * 
 * [ en paralelo: C1, C2 ] -> [ en paralelo: C3, C4 ] -> C5
 */
class SortGroupsStrategy extends Loggable {

	//----------------------------------------------------------------------
	// Propiedades de la clase
	
	//----------------------------------------------------------------------
	// Métodos de la clase
	
	/** Constructor por defecto. */
	public SortGroupsStrategy() {}
	
	/**
	 * Este método agrupa los componentes de una lista ordenada construida a
	 * través del método RTCBuildFileHelper.buildDependencyGraph.
	 * @param components Lista ordenada de componentes maven, incluyendo las 
	 * dependencias entre sí.
	 * @return Lista de grupos de componentes maven
	 */
	public List<List<MavenComponent>> sortGroups (List<MavenComponent> components) {
		List<List<MavenComponent>> ret = [];
		List<MavenComponent> current = [];
		ret << current;
		for (MavenComponent component: components) {
			if (dependsOn(component, current)) {
				current = [];
				ret << current;
			}
			current << component;
		}
		return ret;
	}
	
	/**
	 * Indica si un determinado componente depende de otro componente en la lista indicada
	 * @param component Componente que consultamos.
	 * @param components Lista de componentes.
	 * @return Cierto si el componente consultado depende de alguno de los componentes en la
	 * lista.
	 */
	private boolean dependsOn(MavenComponent component, List<MavenComponent> components) {
		boolean ret = false;
		if (components != null && components.size() > 0) {
			for (MavenComponent c: components) {
				if (MavenComponent.dependsOn(component, c)) {
					ret = true;
				}
			}
		}
		return ret;
	}
}
