package es.eci.utils.base

import groovy.json.JsonBuilder

/**
 * Clase base para beans que deban implementar serialización json
 */
class JSONBean {
	
	//----------------------------------------------------------------
	// Métodos de la clase
	
	// Expresa el objeto como un Map
	private Map asMap() {
		this.class.declaredFields.findAll { !it.synthetic }.collectEntries {
			[ (it.name):this."$it.name" ]
		}
	}

	// Elimina los nulos del map
	def denull(obj) {
		if(obj instanceof Map) {
			obj.collectEntries {k, v ->
				if(v) [(k): denull(v)] else [:]
			}
		} else if(obj instanceof List) {
			obj.collect { denull(it) }.findAll { it != null }
		} else {
			obj
		}
	}
	
	/**
	 * @return Devuelve la serialización JSON del objeto, eliminando los 
	 * valores nulos
	 */
	public String toJSON() {		
		return new JsonBuilder(denull(this.asMap())).toString();
	}
}
