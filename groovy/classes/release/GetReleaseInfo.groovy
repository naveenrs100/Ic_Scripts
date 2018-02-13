package release

import rtc.workitems.WorkItemBean
import rtc.workitems.WorkItemHelper
import es.eci.utils.StringUtil
import es.eci.utils.base.Loggable
import groovy.json.JsonOutput;

/**
 * Esta clase recoge la información necesaria del workitem o issue de release,
 * dejando en el fichero release.json los parámetros
 * 
 * + releaseId
 * + remedyRequest
 * 
 * Que harán falta más tarde para actualizar la información de release
 * en el gestor de actividad (RTC o JIRA)
 * 
 * Deja además un comentario relativo a la construcción en el WorkItem
 */
class GetReleaseInfo extends Loggable {
	
	//-------------------------------------------------------
	// Constantes de la clase
	
	/** Fichero de datos. */
	private static final String RELEASE_INFO_FILE_NAME = "release.json";
	
	//-------------------------------------------------------
	// Propiedades de la clase
	
	// Directorio
	private String parentWorkspace = null;
	// Release id
	private String releaseId = null;
	// Acción realizada
	private String action;
	// Resultado de la construcción
	private String result;
	// Conexión a RTC
	private String rtcUrl;
	private String rtcUser;
	private String rtcPass;
	private String rtcKeystore;
	private String nexusUrl;
	// ¿Está activada la gestión de releases?
	private String releaseManagement;
	
	//-------------------------------------------------------
	// Métodos de la clase
	
	// Recupera la información de release y la almacena en un fichero
	public void storeReleaseInfoIntoFile() {
		if (StringUtil.isNull(releaseId)) {
			log "[WARNING] No se ha informado releaseId"
		}
		else {			
			if (Boolean.valueOf(releaseManagement)) {
				// Consultar la información de la release
				// De momento la consulta solo en RTC
				// TODO: esta clase debe tener suficiente lógica para distinguir
				//	si la información de release está en RTC o en JIRA
				WorkItemHelper helper = 
					new WorkItemHelper(
						rtcUrl,
						rtcUser,
						rtcPass,
						rtcKeystore,
						nexusUrl
					);
				helper.initLogger(this);
				
				File f = new File(parentWorkspace, RELEASE_INFO_FILE_NAME)
				f.createNewFile()
				
				Map<String, String> data = [:]
				WorkItemBean bean = helper.getWorkItem(releaseId);
				data['releaseId'] = releaseId;
				data['remedyRequest'] = bean.getAttributes().get("CRQ_Remedy");
				
				f.text = JsonOutput.toJson(data)
				
				// Actualizar el workitem
				helper.addComment(releaseId, 
					"Proceso de IC: $action ejecutado con resultado $result")
			}
			else {
				log "[WARNING] No se recoge la información de release, ni se actualizan los comentarios, al estar desactivada la gestión de release"
			}
		}
	}
	
	/**
	 * Este método valida que el id de release pasado corresponde a un
	 * elemento de release válido en JIRA o RTC, que no está en estado
	 * cerrado ni cancelado.
	 */
	public void validateRelease() {
		if (StringUtil.isNull(releaseId)) {
			log "[WARNING] No se ha informado release, no se realizan acciones de gestión de release"
		}
		else {
			if (Boolean.valueOf(releaseManagement)) {
				// TODO: esta clase debe tener suficiente lógica para distinguir
				//	si la información de release está en RTC o en JIRA
				WorkItemHelper helper = 
					new WorkItemHelper(
						rtcUrl,
						rtcUser,
						rtcPass,
						rtcKeystore,
						nexusUrl
					);
				helper.initLogger(this);
				WorkItemBean bean = helper.getWorkItem(releaseId);
				if (bean == null) {
					throw new Exception("No se puede encontrar la release ${releaseId}; no existe o no es visible")
				}
				if (!WorkItemHelper.RTC_WI_TYPE_RELEASE.equals(bean.getTypeId())) {
					throw new Exception("El elemento ${releaseId} no es del tipo release")
				} 
				// Validar el estado
				if ([WorkItemHelper.RTC_WI_STATE_RELEASE_CANCELLED,
					WorkItemHelper.RTC_WI_STATE_RELEASE_CLOSED].contains(bean.getStateId())) {
					throw new Exception("No se puede realizar el proceso al estar la release ${releaseId} en estado ${bean.stateDescription}")
				}
				// Se considera que la release está en estado válido
				log "La release ${releaseId} en estado ${bean.stateDescription} permite continuar el proceso de IC"
			}
			else {
				log "[WARNING] No se valida la release al estar desactivada la gestión de release"
			}
		}
	}
	
	/**
	 * Cierra la release en RTC o JIRA
	 */
	public void closeRelease() {		
		if (Boolean.valueOf(releaseManagement)) {
			// TODO: esta clase debe tener suficiente lógica para distinguir
			//	si la información de release está en RTC o en JIRA
			WorkItemHelper helper = 
				new WorkItemHelper(
					rtcUrl,
					rtcUser,
					rtcPass,
					rtcKeystore,
					nexusUrl
				);
			helper.initLogger(this);
			helper.closeRelease(releaseId);
		}
			else {
				log "[WARNING] No se cierra la release al estar desactivada la gestión de release"
			}
	}

	/**
	 * @return the parentWorkspace
	 */
	public String getParentWorkspace() {
		return parentWorkspace;
	}

	/**
	 * @param parentWorkspace the parentWorkspace to set
	 */
	public void setParentWorkspace(String parentWorkspace) {
		this.parentWorkspace = parentWorkspace;
	}

	/**
	 * @return the releaseId
	 */
	public String getReleaseId() {
		return releaseId;
	}

	/**
	 * @param releaseId the releaseId to set
	 */
	public void setReleaseId(String releaseId) {
		this.releaseId = releaseId;
	}
	
	/**
	 * @return the releaseId
	 */
	public String getWorkItem() {
		return releaseId;
	}

	/**
	 * @param releaseId the releaseId to set
	 */
	public void setWorkItem(String workItem) {
		this.releaseId = workItem;
	}

	/**
	 * @return the action
	 */
	public String getAction() {
		return action;
	}

	/**
	 * @param action the action to set
	 */
	public void setAction(String action) {
		this.action = action;
	}

	/**
	 * @return the result
	 */
	public String getResult() {
		return result;
	}

	/**
	 * @param result the result to set
	 */
	public void setResult(String result) {
		this.result = result;
	}

	/**
	 * @return the rtcUrl
	 */
	public String getRtcUrl() {
		return rtcUrl;
	}

	/**
	 * @param rtcUrl the rtcUrl to set
	 */
	public void setRtcUrl(String rtcUrl) {
		this.rtcUrl = rtcUrl;
	}

	/**
	 * @return the rtcUser
	 */
	public String getRtcUser() {
		return rtcUser;
	}

	/**
	 * @param rtcUser the rtcUser to set
	 */
	public void setRtcUser(String rtcUser) {
		this.rtcUser = rtcUser;
	}

	/**
	 * @return the rtcPass
	 */
	public String getRtcPass() {
		return rtcPass;
	}

	/**
	 * @param rtcPass the rtcPass to set
	 */
	public void setRtcPass(String rtcPass) {
		this.rtcPass = rtcPass;
	}

	/**
	 * @return the rtcKeystore
	 */
	public String getRtcKeystore() {
		return rtcKeystore;
	}

	/**
	 * @param rtcKeystore the rtcKeystore to set
	 */
	public void setRtcKeystore(String rtcKeystore) {
		this.rtcKeystore = rtcKeystore;
	}

	/**
	 * @return the urlNexus
	 */
	public String getNexusUrl() {
		return nexusUrl;
	}

	/**
	 * @param urlNexus the urlNexus to set
	 */
	public void setNexusUrl(String urlNexus) {
		this.nexusUrl = urlNexus;
	}

	/**
	 * @return the releaseManagement
	 */
	public String getReleaseManagement() {
		return releaseManagement;
	}

	/**
	 * @param releaseManagement the releaseManagement to set
	 */
	public void setReleaseManagement(String releaseManagement) {
		this.releaseManagement = releaseManagement;
	}
	
	
	
}
