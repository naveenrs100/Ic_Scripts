package ppm

/**
 * Esta clase modela un producto en PPM
 */
public class PPMProduct {
	
	//-----------------------------------------------------
	// Propiedades del producto
	
	// Código
	private String code;
	// Descripción
	private String description;	
	
	//-----------------------------------------------------
	// Métodos del producto
	
	/**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Construye un producto con código y descripción.
	 * @param code Código de producto
	 * @param description Descripción
	 */
	public PPMProduct(String code, String description) {
		this.code = code;
		this.description = description;
	}
	
}