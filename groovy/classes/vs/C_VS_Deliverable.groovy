package vs

import java.util.List;


/**
 * Esta clase representa un entregable definido en los ficheros de configuración
 * de componente.
 */
class C_VS_Deliverable {

	// Propiedades de la clase
	private String type;
	private String groupId;
	private String id;
	private String ruta;
	private String ide;
	private Boolean debug;
	private String version;
	private List<C_VS_Platform> platforms;
	private String fichero;
	
	// Dlls para empaquetado (opcional)
	private List<C_VS_DLL> dlls;
	
	/**
	 * Constructor vacío
	 */
	public C_VS_Deliverable() {
		platforms = new LinkedList<C_VS_Platform>();
		dlls = new LinkedList<C_VS_DLL>()
	}
	
	/**
	 * Añade una plataforma a la lista de plataformas
	 * @param platform Plataforma de construcción
	 */
	public void addPlatform(C_VS_Platform platform) {
		platforms << platform;
	}
	
	/**
	 * Añade una dll a la lista de dlls
	 * @param dll Referencia a una biblioteca
	 */	
	public void addDll(C_VS_DLL dll) {
		dlls << dll
	}
	
	public String getGroupId() {
		return groupId;
	}
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getRuta() {
		return ruta;
	}
	public void setRuta(String ruta) {
		this.ruta = ruta;
	}
	public String getIde() {
		return ide;
	}
	public void setIde(String ide) {
		this.ide = ide;
	}
	public Boolean getDebug() {
		return debug;
	}
	public void setDebug(Boolean debug) {
		this.debug = debug;
	}
	public List<C_VS_Platform> getPlatforms() {
		return platforms;
	}
	public void setPlatforms(List<C_VS_Platform> platforms) {
		this.platforms = platforms;
	}
	public List<C_VS_DLL> getDlls() {
		return dlls;
	}
	public void setDlls(List<C_VS_DLL> dlls) {
		this.dlls = dlls;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getFichero() {
		return fichero;
	}
	public void setFichero(String fichero) {
		this.fichero = fichero;
	}
	
}
