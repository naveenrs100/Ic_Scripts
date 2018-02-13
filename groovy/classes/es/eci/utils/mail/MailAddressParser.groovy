package es.eci.utils.mail

import java.util.List

/**
 * Esta clase facilita el parseo de direcciones de correo desde
 * el parámetro managersMail del job de jenkins  
 */
class MailAddressParser {

	// Descompone una lista de destinatarios separada por comas en una lista de
	//	cadenas de caracteres
	public static List<String> parseReceivers(String managersMail) {
		List<String> ret = new LinkedList<String>()
		def managers = managersMail.split(/[,*\s*]/)
		managers.each { if (it != null && it.trim().size() > 0) { ret << it } }
		return ret;
	}
}
