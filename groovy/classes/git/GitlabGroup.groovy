package git;

/**
 * Esta clase modela un grupo de gitlab
 */
public class GitlabGroup {

	//--------------------------------------------
	// Propiedades del grupo
	
	// Nombre del grupo
	private String name;
	// Id interno del grupo
	private Integer id;
	// Identificador de producto.  Esta información
	//	obedece a la necesidad interna de asociar un 
	//	grupo a un producto, y no existe en gitlab.
	//	En nuestro entorno, lo inferimos de un campo
	//	que se mantiene en el job de jenkins (productId)
	private String productId;
	
	//--------------------------------------------
	// Métodos del grupo
		
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}
	/**
	 * @param productId the productId to set
	 */
	public void setProductId(String productId) {
		this.productId = productId;
	}
	
	
}
