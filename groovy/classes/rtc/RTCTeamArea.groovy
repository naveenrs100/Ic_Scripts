package rtc

/**
 * Modela un área de equipo de RTC.  Es un modelo muy simplificado en el
 * que no se profundiza en el rol de los usuarios.
 */
class RTCTeamArea {

	//--------------------------------------------------------
	// Propiedades del bean
	
	// Nombre del área de equipo
	private String name;
	// UUID del área de equipo
	private String uuid;
	// Lista de usuarios en el área de equipo
	private List<String> users = [];
	
	//--------------------------------------------------------
	// Métodos del bean
	
	/**
	 * Construye un área de equipo.
	 * @param name Nombre del área de equipo.
	 */
	public RTCTeamArea(String name,String uuid) {
		super();
		this.name = name;
		this.uuid = uuid;
	}


	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	
	/**
	 * @return the uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * @return the users
	 */
	public List<String> getUsers() {
		return new LinkedList<String>(users);
	}
	
	/**
	 * Añade un usuario a la lista de usuarios en el área de equipo.
	 */
	public void addUser(String user) {
		if (!users.contains(user)) {
			users << user
		}
	}
}
