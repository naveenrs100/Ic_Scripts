package vs

import java.util.List;

/**
 * Esta clase modela un elemento cfg encontrado en el descriptor de un módulo
 * de Visual Studio
 */
class C_VS_Configuration {
	// Propiedades de la clase
	private String ruta;
	private List<C_VS_Platform> platforms;
	
	/**
	 * Constructor vacío
	 */
	public C_VS_Configuration() {
		platforms = new LinkedList<C_VS_Platform>();
	}
	
	/**
	 * Añade una plataforma a la lista de plataformas
	 * @param platform Plataforma de construcción
	 */
	public void addPlatform(C_VS_Platform platform) {
		platforms << platform;
	}
	public String getRuta() {
		return ruta;
	}
	public void setRuta(String ruta) {
		this.ruta = ruta;
	}
	public List<C_VS_Platform> getPlatforms() {
		return platforms;
	}
	public void setPlatforms(List<C_VS_Platform> platforms) {
		this.platforms = platforms;
	}
}
