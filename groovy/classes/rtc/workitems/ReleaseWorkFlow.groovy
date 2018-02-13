package rtc.workitems

import es.eci.utils.StringUtil;
import es.eci.utils.base.Loggable

/**
 * Esta clase modela la lógica de un workflow en RTC.  Por ejemplo, responde al XML
 * 
 <workflow description="Flujo de trabajo para el elemento ECI Release" 
	   		name="Release ECI Workflow" 
	   		reopenActionId="com.eci.team.workitem.releaseWorkflow.action.a8" 
	   		resolveActionId="com.eci.team.workitem.releaseWorkflow.action.a5" 
	   		startActionId="com.eci.team.workitem.releaseWorkflow.action.a9">
        <action icon="processattachment:/workflow/works.gif" 
        	id="com.eci.team.workitem.releaseWorkflow.action.a16" 
        	name="Desplegar Piloto" 
        	state="com.eci.team.workitem.releaseWorkflow.state.s9"/>
        <action icon="processattachment:/workflow/inprogress.gif" 
        	id="com.eci.team.workitem.releaseWorkflow.action.a7" 
        	name="Iniciar Diseño" 
        	state="com.eci.team.workitem.releaseWorkflow.state.s2"/>
        <action icon="processattachment:/workflow/inprogress.gif" 
        	id="com.eci.team.workitem.releaseWorkflow.action.a15" 
        	name="Reactivación por Adm." 
        	state="com.eci.team.workitem.releaseWorkflow.state.s2"/>
        <action icon="processattachment:/workflow/reopen.gif" 
        	id="com.eci.team.workitem.releaseWorkflow.action.a8" 
        	name="Volver a Diseño" 
        	state="com.eci.team.workitem.releaseWorkflow.state.s2"/>
        <action icon="processattachment:/workflow/reopen.gif" 
        	id="com.eci.team.workitem.releaseWorkflow.action.a14" 
        	name="Volver a INT" 
        	state="com.eci.team.workitem.releaseWorkflow.state.s7"/>
        <action icon="processattachment:/workflow/open.gif" 
        	id="com.eci.team.workitem.releaseWorkflow.action.a9" 
        	name="Crear Release" 
        	state="com.eci.team.workitem.releaseWorkflow.state.s1"/>
        <action icon="processattachment:/workflow/reopen.gif" 
        	id="com.eci.team.workitem.releaseWorkflow.action.a13" 
        	name="Volver a PRE" 
        	state="com.eci.team.workitem.releaseWorkflow.state.s3"/>
        <action icon="processattachment:/workflow/reject.gif" 
        	id="com.eci.team.workitem.releaseWorkflow.action.a2" 
        	name="Cancelar" 
        	state="com.eci.team.workitem.releaseWorkflow.state.s6"/>
        <action icon="processattachment:/workflow/works.gif" 
        	id="com.eci.team.workitem.releaseWorkflow.action.a12" 
        	name="Preparar PRO" 
        	state="com.eci.team.workitem.releaseWorkflow.state.s8"/>
        <action icon="processattachment:/workflow/implement2.gif" 
        	id="com.eci.team.workitem.releaseWorkflow.action.a11" 
        	name="Pasar a INT" 
        	state="com.eci.team.workitem.releaseWorkflow.state.s7"/>
        <action icon="processattachment:/workflow/reopen.gif" 
        	id="com.eci.team.workitem.releaseWorkflow.action.a4" 
        	name="Volver a Nueva" 
        	state="com.eci.team.workitem.releaseWorkflow.state.s1"/>
        <action icon="processattachment:/workflow/close.gif" 
        	id="com.eci.team.workitem.releaseWorkflow.action.a5" 
        	name="Desplegar a Produccion" 
        	state="com.eci.team.workitem.releaseWorkflow.state.s4"/>
        <action icon="processattachment:/workflow/implement2.gif" 
        	id="com.eci.team.workitem.releaseWorkflow.action.a1" 
        	name="Pasar a PRE" 
        	state="com.eci.team.workitem.releaseWorkflow.state.s3"/>
        <action icon="processattachment:/workflow/reopen.gif" 
        	id="com.eci.team.workitem.releaseWorkflow.action.a17" 
        	name="Volver a Listo PRO" 
        	state="com.eci.team.workitem.releaseWorkflow.state.s8"/>
        <action icon="processattachment:/workflow/implement.gif" 
        	id="com.eci.team.workitem.releaseWorkflow.action.a18" 
        	name="Iniciar Desarrollo" 
        	state="com.eci.team.workitem.releaseWorkflow.state.s10"/>
        <action icon="processattachment:/workflow/reopen.gif" 
        	id="com.eci.team.workitem.releaseWorkflow.action.a19" 
        		name="Volver a Desarrollo" 
        		state="com.eci.team.workitem.releaseWorkflow.state.s10"/>
 * 
  
 	El workflow se puede resumir en los pasos:
	
Nueva - com.eci.team.workitem.releaseWorkflow.state.s1
-> Iniciar diseño - com.eci.team.workitem.releaseWorkflow.action.a7
Diseño - com.eci.team.workitem.releaseWorkflow.state.s9
-> Iniciar desarrollo - com.eci.team.workitem.releaseWorkflow.action.a18
Desarrollo - com.eci.team.workitem.releaseWorkflow.state.s10
-> Pasar a PRE - com.eci.team.workitem.releaseWorkflow.action.a1
UAT/Preproducción - com.eci.team.workitem.releaseWorkflow.state.s3
[  OPCIONAL 
-> Pasar a INT - com.eci.team.workitem.releaseWorkflow.action.a11
NFT/Integración - com.eci.team.workitem.releaseWorkflow.state.s7
]
-> Preparar PRO - com.eci.team.workitem.releaseWorkflow.action.a12
Listo PRO - com.eci.team.workitem.releaseWorkflow.state.s8
[ OPCIONAL
-> Desplegar piloto - com.eci.team.workitem.releaseWorkflow.action.a16
Piloto - com.eci.team.workitem.releaseWorkflow.state.s9
]
-> Desplegar a producción - com.eci.team.workitem.releaseWorkflow.action.a5
Producción - com.eci.team.workitem.releaseWorkflow.state.s4 
 */
class ReleaseWorkFlow extends Loggable {
	
	//----------------------------------------------------------
	// Propiedades de la clase
	
	// Transcurso de estados y acciones hacia el estado final
	private static final List<WIState> ACTIONS = [
		// Estado inicial
		new WIState(
			"Nueva",
			null,
			"com.eci.team.workitem.releaseWorkflow.state.s1"),
		new WIState(
			"Diseño",
			"com.eci.team.workitem.releaseWorkflow.action.a7",
			"com.eci.team.workitem.releaseWorkflow.state.s2"),
		new WIState(
			"Desarrollo",
			"com.eci.team.workitem.releaseWorkflow.action.a18",
			"com.eci.team.workitem.releaseWorkflow.state.s10"),
		new WIState(
			"UAT/Preproducción",
			"com.eci.team.workitem.releaseWorkflow.action.a1",
			"com.eci.team.workitem.releaseWorkflow.state.s3"),
		new WIState(
			"NFT/Integración",
			"com.eci.team.workitem.releaseWorkflow.action.a11",
			"com.eci.team.workitem.releaseWorkflow.state.s7",
			Boolean.TRUE),
		new WIState(
			"Listo PRO",
			"com.eci.team.workitem.releaseWorkflow.action.a12",
			"com.eci.team.workitem.releaseWorkflow.state.s8"),
		new WIState(
			"Piloto",
			"com.eci.team.workitem.releaseWorkflow.action.a16",
			"com.eci.team.workitem.releaseWorkflow.state.s9",
			Boolean.TRUE),
		new WIState(
			"Producción",
			"com.eci.team.workitem.releaseWorkflow.action.a5",
			"com.eci.team.workitem.releaseWorkflow.state.s4")
	];
	
	//----------------------------------------------------------
	// Métodos de la clase
	
	// Crea un objeto workflow
	public ReleaseWorkFlow() {
		
	}
	
	/**
	 * Devuelve la lista de estados por los que habría que pasar para alcanzar
	 * el estado final dado un estado actual.
	 * @param currentStateId Id de estado actual
	 * @return Lista de estados, con la acción necesaria para alcanzarlos, para
	 * alcanzar el estado final "Producción"
	 * @throws Exception Si el estado inicial es vacío o bien no corresponde con
	 * los definidos como parte del workflow 
	 */
	public List<WIState> getActionsUntilClosed(String currentStateId) throws Exception {
		List<WIState> ret = [];
		if (StringUtil.isNull(currentStateId)) {
			throw new Exception("Debe informarse el estado actual del WF");
		}
		WIState currentState = ACTIONS.find { it.stateId == currentStateId }
		if (currentState == null) {
			throw new Exception("No se reconoce el estado ${currentStateId} o no se puede progresar desde él");
		}
		for (int i = ACTIONS.indexOf(currentState) + 1; i < ACTIONS.size(); i++) {
			WIState state = ACTIONS[i]
			if (!state.optional) {
				ret << state
			}
		}
		return ret;
	}
	
}
