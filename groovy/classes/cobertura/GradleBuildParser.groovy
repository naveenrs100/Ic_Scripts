/**
 * 
 */
package cobertura

/**
 * Esta clase toma las propiedades necesarias de un build.gradle para leer,
 * entre otras cosas, los directorios fuente.
 */
class GradleBuildParser {

	/**
	 * Busca el valor de los sourceSets de prueba en el DSL del build.gradle
	 * @param text Texto del build.gradle
	 * @return Lista de directorios de prueba
	 */
	public static List<String> parseTestSourceSetsGradle(String originalText) {
		
		// Expurgar comentarios
		StringBuilder sb = new StringBuilder()		
		
		BufferedReader reader = new BufferedReader(new StringReader(originalText))
		boolean comentarioMultiLinea = false
		
		boolean continuar = true
		int index = 0
		
		while (continuar) {
			if (index < originalText.length()) {
				// Leer hasta el próximo comentario
				int indiceComentarioLinea = originalText.indexOf("//", index)
				int indiceComentarioMultiLinea = originalText.indexOf("/*", index)
				if (indiceComentarioLinea == -1 && indiceComentarioMultiLinea == -1) {
					// Terminar
					sb.append(originalText.substring(index))
					continuar = false
				}
				if (indiceComentarioLinea != -1 && (indiceComentarioMultiLinea == -1 || indiceComentarioLinea < indiceComentarioMultiLinea)) {
					// Leer hasta el primer comentario de línea y descartar el resto de la línea
					sb.append(originalText.substring(index, indiceComentarioLinea))
					index = indiceComentarioLinea
					// Seguir hasta acabar el fichero o encontrar salto de línea
					boolean pasar = true
					while (pasar) {
						if (index < originalText.length()) {
							if (originalText.charAt(index) != '\n') {
								index++
							}
							else {
								pasar = false
							}
						}
						else {
							pasar = false
						}
					}
				}
				else if (indiceComentarioMultiLinea != -1 && (indiceComentarioLinea == -1 || indiceComentarioMultiLinea < indiceComentarioLinea)) {
					// Leer hasta el primer comentario multilínea y descartar todo hasta encontrar el */ o fin de fichero
					sb.append(originalText.substring(index, indiceComentarioMultiLinea))
					index = indiceComentarioMultiLinea
					// Seguir hasta acabar el comentario o encontrar el final de comentario (*/)
					boolean pasar = true
					while (pasar) {
						if (index < originalText.length() - 1) {
							if (originalText.charAt(index) == '*' && originalText.charAt(index + 1) == '/') {
								pasar = false
								index = index + 2
							}
							else {
								index++
							}
						}
						else {
							pasar = false
						}
					}
				}
			}
			else {
				continuar = false
			}
		}
		
		
		String text = sb.toString()
		List<String> ret = new LinkedList<String>();
		
		// Buscar hasta sourceSets
		index = text.indexOf("sourceSets")
		
		if (index != -1) {
			// Buscar desde el índice
			sb = new StringBuilder();
			sb.append("sourceSets");
			
			boolean leerSourceSet = true
			int contadorLlaves = 0;
			
			index = text.indexOf("{", index)
			// Desde la llave, seguir hasta que el contador de llaves sea 0
			while (index < text.length() && leerSourceSet) {
				char caracter = text.charAt(index++)
				if (caracter == '{') {
					contadorLlaves ++;
				}
				else if (caracter == '}') {
					contadorLlaves --;
				}
				sb.append(caracter)
				leerSourceSet = (contadorLlaves > 0)
			}
			// ConfigSlurper
			ConfigObject obj = new ConfigSlurper().parse(sb.toString());
			def sourceSets = obj.get("sourceSets")
			def dirs = sourceSets?.test?.java?.srcDirs
			if (dirs instanceof java.util.List) {
				ret = dirs 
			}
		}
		
		return ret;
	}
}
