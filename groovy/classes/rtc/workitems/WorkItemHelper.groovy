package rtc.workitems

import rtc.RTCClient
import es.eci.utils.base.Loggable
import groovy.xml.QName;

/**
 * Esta clase agrupa la funcionalidad de work items
 */
class WorkItemHelper extends Loggable {

	//----------------------------------------------------
	// Constantes de la clase
	
	/** Tipo de workitem: release */
	public static final String RTC_WI_TYPE_RELEASE = 
		"com.eci.team.apt.workitem.Type.release";
	/** Estado de release: cancelada. */
	public static final String RTC_WI_STATE_RELEASE_CANCELLED =
		"com.eci.team.workitem.releaseWorkflow.state.s6"; 
	/** Estado de release: en diseño. */
	public static final String RTC_WI_STATE_RELEASE_DESIGN = 
		"com.eci.team.workitem.releaseWorkflow.state.s2";
	/** Estado de release: UAT/Preproducción */
	public static final String RTC_WI_STATE_RELEASE_UAT_PRE = 
		"com.eci.team.workitem.releaseWorkflow.state.s3"
	/** Estado de release: listo para producción. */
	public static final String RTC_WI_STATE_RELEASE_READY_PROD = 
		"com.eci.team.workitem.releaseWorkflow.state.s8"
	/** Estado de release: cerrada (en producción) */
	public static final String RTC_WI_STATE_RELEASE_CLOSED = 
		"com.eci.team.workitem.releaseWorkflow.state.s4";
	
	//----------------------------------------------------
	// Propiedades de la clase
	
	// Cliente RTC
	private RTCClient client = null;
	// Referencia a la url de RTC
	private String rtcURL = null;
	
	//----------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Construye un helper con la información necesaria para instanciar
	 * un cliente REST de RTC.
	 * @param rtcURL URL base de RTC
	 * @param rtcUser Id de usuario
	 * @param rtcPwd Password
	 * @param keystoreVersion Versión del keystore
	 * @param nexusURL URL de nexus
	 */
	public WorkItemHelper(
			String rtcURL, 
			String rtcUser, 
			String rtcPwd, 
			String keystoreVersion, 
			String nexusURL) {
		this.rtcURL = rtcURL;
		client = new RTCClient(rtcURL, rtcUser, rtcPwd, keystoreVersion, nexusURL);
	}
		
	/**
	 * Recupera la información relativa a un work item.
	 * @param workItemId Identificador alfanumérico de un workitem	
	 * @return Objeto con la información del workitem
	 */
	public WorkItemBean getWorkItem(Integer workItemId) {
		return getWorkItem(workItemId.toString());	
	}
	
	/**
	 * Recupera la información relativa a un work item.
	 * @param workItemId Identificador alfanumérico de un workitem	
	 * @return Objeto con la información del workitem
	 */
	public WorkItemBean getWorkItem(String workItemId) {
		client.initLogger(this);
		String rawXML = client.get("rpt/repository/workitem", 
			["fields":"workitem/workItem[id=${workItemId.trim()}]/*/*)"])
		if (client.getLastHttpStatus() != 200) {
			throw new Exception("[ERROR] Ha fallado la llamada al servicio REST de RTC -> return state: " +
				client.getLastHttpStatus());
		}
		return WorkItemBean.from(rawXML);
	}		

	/**
	 * Dado un bean de un workitem de RTC, le informa la sucesión de comentarios
	 * (discusión) que haya transcurrido sobre el mismo.
	 * @param bean [Parámetro de entrada/salida] Objeto correspondiente a un WI de RTC.
	 */
	public void getDiscussion(WorkItemBean bean) {
		client.initLogger(this);
		String rawXML = client.get("resource/itemName/com.ibm.team.workitem.WorkItem/${bean.id}")
		// Encontrar la referencia a la URL para obtener la lista de comentarios completa
		def obj = new XmlParser().parseText(rawXML)['rdf:Description'][0];
		def discussed = obj['oslc:discussedBy'][0];
		String commentsURL = "";
		commentsURL = AttributeHelper.getAttribute(discussed, "resource");
		String preparedURL = commentsURL.replaceAll(this.rtcURL, "")
		if (preparedURL.startsWith("/")) {
			preparedURL = preparedURL.replaceFirst("/", "")
		}
		def commentsRawXML = client.get(preparedURL)
		bean.setDiscussion(CommentBean.from(commentsRawXML));
	}
	
	/**
	 * Este método inserta un comentario en un work item de RTC.
	 * @param bean Información del workitem
	 * @param comment Comentario a insertar en el mismo
	 */
	public void addComment(WorkItemBean bean, String comment) {
		addComment(bean.getId(), comment);
	}
	
	/**
	 * Este método inserta un comentario en un work item de RTC.
	 * @param workItemId Identificador de workitem
	 * @param comment Comentario a insertar en el mismo
	 */
	public void addComment(String workItemId, String comment) {
		client.initLogger(this);
		client.post("oslc/workitems/${workItemId}/rtc_cm:comments/oslc:comment",
			["dcterms:description":comment])
	}

	/**
	 * Cierra una release usando la acción resolve del workflow
	 * @param workItemId Id de workitem
	 */
	public void closeRelease(Integer workItemId) {
		closeRelease(workItemId.toString());
	}
	
	/**
	 * Cierra una release usando la acción resolve del workflow
	 * @param workItemId Id de workitem
	 */
	public void closeRelease(String workItemId) {
		client.initLogger(this);
		closeRelease(getWorkItem(workItemId));
	}
		
	/**
	 * Cierra una release usando la acción resolve del workflow
	 * @param bean Objeto workItem
	 */
	public void closeRelease(WorkItemBean bean) {
		client.initLogger(this);
		List<WIState> states = 
			new ReleaseWorkFlow().getActionsUntilClosed(bean.getStateId());
		if (states != null && states.size() > 0) {
			states.each { WIState state ->
				// Enviar la acción pertinente para atravesarlo
				log "Pasando el WI ${bean.id} al estado ${state.description} ..."
				sendAction(bean.getId(), state.getAction());
			}
		} 
	}
	
	/** 
	 * Este método envía a un workitem una acción, que intentará aplicar
	 * según el workflow que le corresponda.
	 * @param workItemId Identificador de workitem
	 * @param action Acción a ejecutar
	 */
	public void sendAction(String workItemId, String action) {
		client.initLogger(this);
		client.put(
			"resource/itemName/com.ibm.team.workitem.WorkItem/${workItemId}",
			["_action":action]);
	}
	
	/** 
	 * Devuelve el valor de un atributo de un workitem.
	 * @param workItemId Identificador de workitem
	 * @param attribute Nombre del atributo
	 * @return Valor del atributo
	 */
	public String getAttribute(Integer workItemId, String attribute) {
		return getAttribute(workItemId.toString(), attribute);	
	}
	
	/** 
	 * Devuelve el valor de un atributo de un workitem.
	 * @param workItemId Identificador de workitem
	 * @param attribute Nombre del atributo
	 * @return Valor del atributo
	 */
	public String getAttribute(String workItemId, String attribute) {
		WorkItemBean bean = getWorkItem(workItemId);
		return bean.getAttributes().get(attribute);
	}
}
