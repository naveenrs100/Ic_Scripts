package components

/**
 * Bean con la información correspondiente a un conjunto de cambios en RTC.
 */
class RTCChangeSet {  

	/** Id del changeSet */
	private String id;
	private String autor;
	private String email;
	private Date fecha;
	private String idTarea;
	private String comentario;
	
	public RTCChangeSet(String id, String autor, String email, Date fecha, String idTarea, String comentario) {
		this.id = id;
		this.autor = autor;
		this.email = email;
		this.fecha = fecha;
		this.idTarea = idTarea;
		this.comentario = comentario;
	}
	
	public String getId() {
		return id;
	}
	
	public String getAutor() {
		return autor;
	}
	
	public String getEmail() {
		return email;
	}
	
	public Date getFecha() {
		return fecha;
	}
	
	public String getIdTarea() {
		return idTarea;
	}
	
	public String getComentario() {
		return comentario;
	}
	
}