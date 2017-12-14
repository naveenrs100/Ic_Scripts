package ppm;

/**
 * Esta clase implementa la lectura de un fichero de productos de PPM
 * Formato:
 * 
 * <Código>;<Descripción>
 * <Código>;<Descripción>
 * <Código>;<Descripción>
 * ..
 * <Código>;<Descripción>
 * #Línea comentada
 * <Código>;<Descripción>
 * <Código>;<Descripción>
 * <Código>;<Descripción>
 * #Línea comentada
 */
public class PPMProductParser {

	//-----------------------------------------------------
	// Métodos del parser	
	
	/** Construye un parser. */
	public PPMProductParser() {}
	
	/**
	 * Parsea un fichero que contiene una lista de productos en el formato
	 * de PPM.
	 * @param s Contenido del fichero
	 * @return Lista de productos parseados
	 */
	public PPMProductsCollection parse(String s) { 
		PPMProductsCollection products = new PPMProductsCollection();
		s.eachLine { String line ->
			// Excluir líneas comentadas
			if (!line.startsWith("#")) {
				String[] parts = line.split(";");
				products.add(new PPMProduct(parts[0], parts[1]));
			}	
		}
		return products;		
	}
	
	/**
	 * Parsea un fichero que contiene una lista de productos en el formato
	 * de PPM.  Asume que el fichero está en iso-8859-1
	 * @param file Fichero
	 * @return Lista de productos parseados
	 */
	public PPMProductsCollection parse(File file) {
		return parse(file.getText("iso-8859-1"));
	}
}
