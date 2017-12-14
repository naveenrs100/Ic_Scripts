package ppm

import java.text.Normalizer

import es.eci.utils.StringUtil

class PPMProductsCollection {

	//-------------------------------------------------------
	// Propiedades de la clase
	
	// Lista de productos
	private List<PPMProduct> products;
	
	//-------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Construye una colección vacía
	 */
	public PPMProductsCollection() {
		products = []
	}
	
	/**
	 * Añade un producto a la lista.
	 * @param product Producto reconocido en PPM
	 */
	public void add(PPMProduct product) {
		this.products << product;
	}
	
	/**
	 * Busca un producto por nombre en la colección.
	 * @param productName Nombre de producto a buscar
	 * @return El producto si está en la colección, null
	 * si no lo encuentra
	 */
	public PPMProduct findByName(String productName) {
		final String normalizedProductName = 
			StringUtil.removeAccents(productName);
		return products.find {
			normalizedProductName.equals(
				StringUtil.removeAccents(it.getDescription()))			 
		}
	}
}
