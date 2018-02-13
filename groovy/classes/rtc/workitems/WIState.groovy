package rtc.workitems

/**
 * Esta clase modela un paso del Workflow de un workitem
 */
class WIState {

	//-----------------------------------------------------
	// Propiedades del estado
	
	// Descripción del estado
	private String description;
	// Identificador interno de la acción necesaria para alcanzar el estado
	private String action;
	// Identificador interno del estado que representa
	private String stateId;
	// ¿El paso es opcional?
	private Boolean optional = Boolean.FALSE;
	
	//-----------------------------------------------------
	// Métodos del estado
	
	/**
	 * Construye un estado del WF 
	 * @param action Identificador interno de la acción necesaria para alcanzar
	 * el estado
	 * @param stateId Identificador interno del estado que representa
	 * @param optional ¿El paso es opcional?
	 */
	public WIState(
			String description,
			String action,
			String stateId, 
			Boolean optional = Boolean.FALSE) {
		this.description = description;
		this.action = action;
		this.stateId = stateId;
		this.optional = optional;
	}

	@Override
	public String toString() {
		return "$description [${stateId}]"
	}
			
	/**
	 * @return the action
	 */
	public String getAction() {
		return action;
	}

	/**
	 * @return the stateId
	 */
	public String getStateId() {
		return stateId;
	}

	/**
	 * @return the optional
	 */
	public Boolean getOptional() {
		return optional;
	}	
}
