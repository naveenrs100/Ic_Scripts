package urbanCode

import es.eci.utils.base.Loggable
import es.eci.utils.pom.MavenCoordinates

/**
 * Este servicio obtiene la información relevante de un componente
 * en Urban Code, en formato de coordenadas Maven. 
 * <br/>
 * Notar que es <b>imprescindible</b> contar con una instalación local del cliente.  Se puede provisionar
 * desde:<br/>
 * <a href="http://nexus.elcorteingles.int/service/local/repositories/GC/content/ibm/urbanCode/udclient/6.1.0/udclient-6.1.0.zip">Cliente udclient en Nexus</a>
 * <br/> 
 * 
 */
class UrbanCodeComponentInfoService extends Loggable {

	//------------------------------------------------------------
	// Propiedades de la clase	
	
	// Ejecutor Urban Code
	private UrbanCodeExecutor executor = null;
	
	
	//------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Crea una instancia del servicio.
	 * @param exec Ejecutor configurado para conversar con Urban Code
	 */	
	public UrbanCodeComponentInfoService(
			UrbanCodeExecutor exec) {
		this.executor = exec;
	}
		
	/**
	 * Compone unas coordenadas maven a partir de la información en Urban Code para un
	 * determinado componente	
	 * @param component Nombre del componente Urban Code
	 * @return Coordenadas Maven asociadas al componente (sin versión)
	 */
	public MavenCoordinates getCoordinates(String component) {
		executor.initLogger(this);
		def componentInfo = executor.getComponentInformation(component);
		// Razonar sobre la información del componente
		String groupId = "";
		String artifactId = "";
		String version = "";
		String packaging = "jar";
		String classifier = "";
		String repository = "public";
		// Recorrer las propiedades del objeto json
		componentInfo.properties.each { def property ->
			if (property.name.equals("MavenComponentProperties/artifactId")) {
				artifactId = property.value;
			}
			else if (property.name.equals("MavenComponentProperties/extension")) {
				String extension = property.value;
				extension = extension.replaceAll("\\.", "");
				packaging = extension;
			}
			else if (property.name.equals("MavenComponentProperties/groupId")) {
				groupId = property.value;
			}
			else if (property.name.equals("MavenComponentProperties/qualifier")) {
				classifier = property.value;
				if (classifier.indexOf("-") == 0) {
					classifier = classifier.replaceFirst('-', '');
				}
			}
			else if (property.name.equals("MavenComponentProperties/repoUrl")) {
				if (property.value.contains("private"))
					repository = "private-all";
			}
			else if (property.name.equals("MavenComponentProperties/repoUrl")) {
				if (property.value.contains("fichas_dcos"))
					repository = "fichas_dcos";
			}
		}
		// Recorrer las propiedades de templateSourceProperties, en ocasiones viene informado aquí el packaging
		componentInfo.templateSourceProperties.each { def property ->
			if (property.name.equals("extension")) { 
				String extension = property.value;
				extension = extension.replaceAll("\\.", "");
				packaging = extension;
			}
		}
		// Componer el objeto con las coordenadas
		MavenCoordinates coords = new MavenCoordinates(groupId, artifactId, version);
		coords.setPackaging(packaging);
		coords.setClassifier(classifier);
		coords.setRepository(repository);
		
		return coords; 
	}
}
