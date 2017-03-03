package vs


/**
 * Elemento en el XML de configuración con una referencia a DLL
 */
class C_VS_DLL {

	// Referencia a un componente de biblioteca
	private String dll;
	
	public C_VS_DLL() {
		
	}
	
	public String getDll() {
		return dll;
	}
	public void setDll(String dll) {
		this.dll = dll;
	}
}
