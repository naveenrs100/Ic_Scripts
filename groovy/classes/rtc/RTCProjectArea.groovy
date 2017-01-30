package rtc

import java.util.List;

/**
 * Modela un área de proyecto de RTC.
 */
class RTCProjectArea {

	//--------------------------------------------------------
	// Propiedades del bean
	
	// Nombre del área de proyecto
	private String name;
	// UUID del área de proyecto
	private String uuid;
	// Lista de usuarios en el área de proyecto
	private List<String> users = [];
	// Áreas de equipo del área de proyecto
	private List<RTCTeamArea> teamAreas = [];
	
	//--------------------------------------------------------
	// Métodos del bean	
	
	/**
	 * Crea un área de proyecto a partir de su nombre y su UUID.
	 * @param name Nombre de AP
	 * @param uuid UUID de AP
	 */
	public RTCProjectArea(String name,String uuid) {
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
	 * @return the teamAreas
	 */
	public List<RTCTeamArea> getTeamAreas() {
		return new LinkedList<RTCTeamArea>(teamAreas);
	}
	
	/**
	 * Añade un usuario a la lista de usuarios en el área de proyecto.
	 */
	public void addUser(String user) {
		if (!users.contains(user)) {
			users << user
		}
	}
	
	/**
	 * Añade un área de equipo a la lista en el área de proyecto.
	 */
	public void addTeamArea(RTCTeamArea area) {
		if (!teamAreas.contains(area)) {
			teamAreas << area
		}
	}
}
