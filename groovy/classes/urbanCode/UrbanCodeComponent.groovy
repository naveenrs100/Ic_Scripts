package urbanCode

import es.eci.utils.base.JSONBean;

/**
 * Este bean modela la información para crear una versión de componente en Urban
 * Code.
 */
class UrbanCodeComponent extends JSONBean {

	//-------------------------------------------------------------------
	// Propiedades de la clase
	
	// Componente urban code
	private String component;
	// Propiedades del componente: [clave1:valor1,clave2:valor2,...]
	private Map<String, String> properties = [:];
	
	//-------------------------------------------------------------------
	// Métodos de la clase
	
	
	/**
	 * Crea una versión de componente informada sin propiedades
	 * @param component Componente urban code
	 */
	public UrbanCodeComponent(String component) {
		this(component, [:])
	}
	
	/**
	 * Crea una versión de componente informada
	 * @param component Componente urban code
	 * @param properties Propiedades del componente: [clave1:valor1,clave2:valor2,...]
	 */
	public UrbanCodeComponent(String component, Map<String, String> properties) {
		this.component = component;
		this.properties = properties;
	}

	/**
	 * @return the component
	 */
	public String getComponent() {
		return component;
	}

	/**
	 * @return the properties
	 */
	public Map<String, String> getProperties() {
		return properties;
	}
	
}
