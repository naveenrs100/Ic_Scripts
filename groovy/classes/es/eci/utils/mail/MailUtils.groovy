package es.eci.utils.mail

import es.eci.utils.base.Loggable
import java.util.List;

/**
 * Clase de utilidades para Mail
 */
class MailUtils {
	
	// Añade la lista de managers por defecto a la lista de destinatarios
	def static void addDefaultManagersMail(String defaultManagersMail, List destinatarios) {
		if (defaultManagersMail != null) {
			println ("Añadiendo los managers por defecto...")
			def arrayDefaultManagers = defaultManagersMail.split(",")
			arrayDefaultManagers.each { defaultManager ->
				if (!destinatarios.contains(defaultManager)) {
					println ("Añadiendo ${defaultManager}...")
					destinatarios.add("bcc:"+defaultManager)
				}
			}
		}
	}
	
}
