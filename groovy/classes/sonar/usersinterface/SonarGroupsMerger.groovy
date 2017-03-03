/**
 * 
 */
package sonar.usersinterface

import es.eci.utils.base.Loggable


/**
 * Esta clase debe mezclar la información obtenida de otra fuente (RTC, git, etc)
 * con la disponible en Sonar, en particular usuarios y grupos.
 */
public class SonarGroupsMerger extends Loggable {

	//--------------------------------------------------------------
	// Propiedades de la clase
	
	// Información cacheada de la instancia de sonar
	private SonarPermissionsService info;
	
	//--------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Construye un merger con la información actualizada de Sonar.
	 * @param info Caché de información de usuarios, grupos y permisos de
	 * 	la instancia de Sonar.
	 */
	public SonarGroupsMerger(SonarPermissionsService info) {
		this.info = info;
	} 
	
	/**
	 * Este método busca el grupo indicado en la instancia de Sonar, lo crea
	 * nuevo si es necesario, y actualiza sobre el mismo la lista de usuarios.
	 * @param groupName Nombre de grupo.
	 * @param eciCodes Lista de usuarios resultante.
	 * @param description Opcional.  Descripción del grupo.
	 * @return Grupo mezclado
	 */
	public SonarGroup merge(
			String groupName, 
			List<String> eciCodes, 
			String description = null) {
		SonarGroup group = info.group(groupName + " - users", description);
		List<String> existingUsers = group.getUsers();
		// Eliminar del grupo los que haya que eliminar
		existingUsers.each { String existingUser ->
			if (!eciCodes.contains(existingUser)) {
				group.removeUser(existingUser);
			}
		}
		// Añadir al grupo los que haya que añadir
		eciCodes.each { String newUser ->
			if (!existingUsers.contains(newUser)) {
				group.addUser(newUser);
			}
		}
		return group;
	}
}
