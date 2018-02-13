package rtc.workitems

import es.eci.utils.StringUtil
import groovy.xml.QName

/**
 * Métodos de utilidad para leer un atributo de un nodo XML
 */
class AttributeHelper {

	/**
	 * Recupera el valor de un atributo de un nodo XML
	 * @param node Nodo
	 * @param attributeName Nombre del atributo
	 * @return Valor del atributo, si está presente
	 */
	public static String getAttribute(def node, String attributeName) {
		String ret = null;
		if (node != null && StringUtil.notNull(attributeName)) {
			node.attributes().keySet().each { QName key ->
				if (attributeName.equals(key.localPart)) {
					ret = node.attributes()[key];
				}
			}
		}
		return ret;
	}
}
