package components

/**
 * Bean con la información correspondiente a un componente RTC
 */
class RTCComponent {

	private String id;
	private String nombre;
	private boolean added = false;
	/** Conjunto de cambios devuelto por la llamada a scm compare */
	private List<RTCChangeSet> cambios;
	
	public RTCComponent(String id, String nombre, boolean added = false) {
		this.id = id;
		this.nombre = nombre;
		this.cambios = [];
		this.added = added;
	}
	
	public String getId() {
		return id;
	}
	
	public String getNombre() {
		return nombre;
	}
	
	public List<RTCChangeSet> getCambios() {
		return cambios;
	}
	
	public void anyadirCambio(RTCChangeSet change) {
		cambios << change
	}
	
	public boolean isAdded() {
		return added;
	}
}