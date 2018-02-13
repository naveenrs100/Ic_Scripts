package es.eci.utils;

import java.nio.ByteBuffer;

import java.util.UUID;

import org.apache.commons.codec.binary.Base64;

public class UtilUUID {
	
	/**
	 * Genera la cadena Base64 propia de los identificadores de área de 
	 * proyecto de RTC a partir de un UUID de área de proyecto interno
	 * de QUVE.
	 * @param uuid Identificador interno de área de proyecto en QUVE
	 * @return Cadena Base64 propia de RTC con el área de proyecto
	 */
	public static String getBase64String(UUID uuid) {
		ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
	    bb.putLong(uuid.getMostSignificantBits());
	    bb.putLong(uuid.getLeastSignificantBits());
	    byte[] uuidArr = bb.array();
		return Base64.encodeBase64URLSafeString(uuidArr);
	}

	/**
	 * Genera un UUID a partir de una cadena en Base64 como las que utiliza
	 * RTC para los identificadores de sus áreas de proyecto.
	 * @param base64String Cadena en Base64 procedente de RTC
	 * @return UUID equivalente al identificador de área de proyecto
	 */
	public static UUID getUUIDFromBase64String(String base64String) {
		return getUUIDFromBase64String(base64String, false);
	}
	
	/**
	 * Genera un UUID a partir de una cadena en Base64 como las que utiliza
	 * RTC para los identificadores de sus áreas de proyecto.
	 * @param base64String Cadena en Base64 procedente de RTC
	 * @param trimfirst Cierto si debe recortar el '_' inicial para poder 
	 * 	procesarla
	 * @return UUID equivalente al identificador de área de proyecto
	 */
	public static UUID getUUIDFromBase64String(String base64String, boolean trimfirst) {
		if (trimfirst)
			base64String = base64String.substring(1, base64String.length());
		byte[] bytes = Base64.decodeBase64(base64String);
		ByteBuffer bb = ByteBuffer.wrap(bytes);
	    UUID uuid = new UUID(bb.getLong(), bb.getLong());
	    return uuid;
	}
	
	/**
	 * Genera un UUID a partir de un entero largo de identificador de proyecto
	 * JIRA.  Este identificador se corresponde con el project.id, no con el
	 * project.key
	 * @param jiraProject Identificador entero largo de proyecto en JIRA
	 * @return UUID equivalente al entero largo
	 */
	public static UUID getUUIDFromJIRAProject(Long jiraProject) {
		return new UUID(0, jiraProject);		
	}
	
	/**
	 * Si el UUID se ajusta al formato esperado, devuelve el entero
	 * largo correspondiente al proyecto JIRA correspondiente.
	 * @param uuid UUID almacenado en QUVE
	 * @return Identificador interno de proyecto en JIRA si cumple con el formato;
	 * 		-1 en otro caso
	 */
	public static Long getJiraProjectFromUUID(UUID uuid) {
		Long ret = -1l;
		if (uuid.getMostSignificantBits() == 0l) {
			ret = uuid.getLeastSignificantBits();
		}
		return ret;
	}
}
