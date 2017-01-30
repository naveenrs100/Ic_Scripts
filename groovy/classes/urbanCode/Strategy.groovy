package urbanCode;

import es.eci.utils.pom.MavenCoordinates;

public interface Strategy {
	
	/**
	 * Este método devuelve una lista de coordenadas maven con las versiones
	 * a resolver de los entregables subidos a Nexus.
	 * @param apiNexus URL de la API de Nexus para su consulta
	 * @param repositoryId Identificador de repositorio de Nexus a consultar
	 * @param ficheroBase Directorio donde se encuentra el código fuente
	 * @param component Nombre del componente en Urban Code
	 * @param v Coordenada Maven con la versión del artefacto leída del version.txt
	 * @return Lista de coordenadas en Nexus de los entregables asociados al código
	 * fuente en el directorio pasado como parámetro
	 */
	public List<MavenCoordinates> resolve(
		String apiNexus, 
		String repositoryId, 
		File ficheroBase, 
		String component, 
		MavenCoordinates v);
}	