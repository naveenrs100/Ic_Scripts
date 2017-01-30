package ldap

class LDAPRecord {

	//----------------------------------------------
	// Propiedades del registro
	
	// Atributos del registro
	private Map<String, List<Object>> attributes;
	
	//----------------------------------------------
	// Métodos del registro
	
	/** Construye un registro vacío */
	public LDAPRecord() {
		attributes = new HashMap<String, List<Object>>();		
	}
	
	/**
	 * Añade un valor a un atributo.
	 * @param attribute Nombre de atributo.
	 * @param value Valor a añadir.
	 */
	public void populate(String attribute, Object value) {
		List<Object> values = attributes.get(attribute);
		if (values == null) {
			values = [];
			attributes.put(attribute, values);
		}
		if (value instanceof List) {
			value.each { values << it }
		}
		else {
			values << value;
		}
	}
	
	/**
	 * Devuelve la lista de valores asociados a un atributo.
	 * @param attribute Atributo de LDAP.
	 * @return Valores en LDAP asociados al atributo.
	 */
	public List<Object> getValues(String attribute) {
		return attributes.get(attribute);
	}
	
	// Algunos atributos merecen tratamiento especial
	
	/**
	 * Código de empleado ECI.
	 * @return El código del empleado.
	 */
	public String getUserId() {
		return getString("sAMAccountName");
	}
	
	/**
	 * Dirección de correo ECI.
	 * @return El correo del empleado.
	 */
	public String getMail() {
		return getString("mail");
	}
	
	/**
	 * Nombre completo del empleado.
	 * @return Nombre completo
	 */
	public String getDisplayName() {
		return getString("displayName");
	}
	
	// Devuelve un valor como cadena
	private String getString(String att) {
		String ret = null;
		List<Object> values = attributes.get(att)
		if (values != null && values.size() > 0) {
			ret = values[0].toString();
		}
		return ret;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("==========================================");
		sb.append("\n");
		sb.append("Empleado: ");
		sb.append(getDisplayName());
		sb.append(" [");
		sb.append(getUserId());
		sb.append("]\n");
		sb.append("mail: ");
		sb.append(getMail());
		sb.append("\n");
		sb.append("Información completa: ");
		sb.append("\n");
		for (String key: attributes.keySet()) {
			sb.append("  ${key}:");
			sb.append("\n");
			List<Object> values = attributes.get(key);
			for (Object value: values) {
				sb.append("    ${value.toString()}");
				sb.append("\n");
			}
		}
		return sb.toString();
	}
}
