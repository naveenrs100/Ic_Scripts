package urbanCode

import es.eci.utils.base.JSONBean;

/**
 * Esta clase modela la información para crear una versión de componente en Urban
 * Code.
 */
class UrbanCodeComponentVersion extends JSONBean {

	//-------------------------------------------------------------------
	// Propiedades de la clase
	
	// Componente urban code
	private String component;
	// Versión del componente (1.2.3.4, 2.4)
	private String name;
	// Descripción de la nueva versión
	private String description;
	// incremental/full
	private String type;
	
	//-------------------------------------------------------------------
	// Métodos de la clase
	
	
	/**
	 * Crea una versión de componente informada
	 * @param component Componente urban code
	 * @param name Versión del componente (1.2.3.4, 2.4)
	 * @param description Descripción de la nueva versión
	 * @param type incremental/full
	 */
	public UrbanCodeComponentVersion(String component, String name,
			String description, String type) {
		super();
		this.component = component;
		this.name = name;
		this.description = description;
		this.type = type;
	}

	/**
	 * @return the component
	 */
	public String getComponent() {
		return component;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
			
	
}
