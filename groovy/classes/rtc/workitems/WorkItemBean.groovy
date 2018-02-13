package rtc.workitems

import groovy.transform.PackageScope;

/**
 * Esta clase modela la información de un workitem
 */
class WorkItemBean {

	//--------------------------------------------------------
	// Propiedades de la clase
	
	// Identificador
	private String id;
	// Identificador numérico de tipo
	private String typeId;
	// Descripción localizada del tipo
	private String typeDescription;
	// Identificador numérico del estado
	private String stateId;
	// Descripción localizada del estado
	private String stateDescription;
	// Resumen del workitem
	private String summary;
	
	// Discusión
	private List<CommentBean> discussion;
	
	// Atributos
	private Map<String, String> attributes;
	
	//--------------------------------------------------------
	// Métodos de la clase
		
	/**
	 * Método de factoría estática que crea un bean de work item
	 * a partir del retorno del servicio web.
	 * @param rawXML XML crudo devuelto por RTC
	 * @return Objeto workitem bean con la información asociada.
	 */
	public static WorkItemBean from (String rawXML) {
		WorkItemBean bean = new WorkItemBean();
		def obj = new XmlParser().parseText(rawXML).workItem[0];
		bean.id = obj.id[0].text()
		bean.typeId = obj.type[0].id[0].text()
		bean.typeDescription = obj.type[0].name[0].text()
		bean.stateId = obj.state[0].id[0].text()
		bean.stateDescription = obj.state[0].name[0].text()	
		if (obj.mediumStringExtensions != null &&
			obj.mediumStringExtensions.size() > 0) {
			
			// Recuperar los atributos de tipo MediumString
			obj.mediumStringExtensions.each { extension ->
				bean.addAttribute(
					extension.key[0].text(),
					extension.value[0].text()
					);
			}
		}	
		return bean;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the typeId
	 */
	public String getTypeId() {
		return typeId;
	}

	/**
	 * @return the typeDescription
	 */
	public String getTypeDescription() {
		return typeDescription;
	}

	/**
	 * @return the stateId
	 */
	public String getStateId() {
		return stateId;
	}

	/**
	 * @return the stateDescription
	 */
	public String getStateDescription() {
		return stateDescription;
	}

	/**
	 * @return the summary
	 */
	public String getSummary() {
		return summary;
	}
	
	/**
	 * Lista de comentarios adjuntos al WI.
	 * @return Lista de comentarios.
	 */
	public List<CommentBean> getDiscussion() {
		return discussion;
	}
	
	/**
	 * Setter de la discusión
	 * @param discussion Lista de comentarios
	 */
	@PackageScope
	void setDiscussion(List<CommentBean> discussion) {
		this.discussion = discussion;
	}
	/**
	 * @return the attributes
	 */
	public Map<String, String> getAttributes() {
		return attributes;
	}
	
	/**
	 * Añade un atributo al workitem
	 * @param key Clave de atributo
	 * @param value Valor de atributo
	 */
	private void addAttribute(String key, String value) {
		attributes[key] = value;
	}

	// Se deja privado el constructor
	private WorkItemBean() {
		discussion = [];
		attributes = [:]
	}	
}
