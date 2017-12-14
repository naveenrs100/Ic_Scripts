package urbanCode

import es.eci.utils.NexusHelper
import es.eci.utils.TmpDir
import es.eci.utils.ZipHelper
import es.eci.utils.base.JSONBean
import groovy.json.JsonSlurper

/**
 * 
 * Este bean modela el objeto necesario para informar de una release de UrbanCode.
 * El objeto se define a partir de su serialización en JSON  
 * <pre>
{
  "name": "NombreAPP-version",
  "application": "NombreAPP",
  "description": "descripción de los cambios",
  "versions": [{"componente1": "1.0"}, {"componente2": "1.0"}, {"componente3": "1.1"}]
}</pre>
 * 
 */
class UrbanCodeSnapshot extends JSONBean {

	//------------------------------------------------------------
	// Propiedades del informe
	
	private String name
	private String application
	private String description
	private List<Map<String, String>> versions;
	
	//-------------------------------------------------------------
	// Métodos del informe
	
	/** Construye un informe vacío */
	public UrbanCodeSnapshot() {
		this(null, null, null, new LinkedList<Map<String, String>>())
	}

	/**
	 * Construye un snapshot con todos los valores inicializados
	 * @param name Nombre de la instantánea (p. ej., 00.00.01.00)
	 * @param application Nombre de la aplicación en Urban Code
	 * @param description Descripción de la instantánea
	 * @param versions Tabla con los nombres de los componentes en Urban Code
	 * y las versiones de los mismos a incluir
	 */
	public UrbanCodeSnapshot(String name, String application,
			String description, List<Map<String, String>> versions) {
		super();
		this.name = name;
		this.application = application;
		this.description = description;
		this.versions = versions;
	}
	
	/** 
	 * Construye un informe a partir de un json determinado
	 * @param json Texto del objeto json
	 * @return 
	 */
	public static UrbanCodeSnapshot parseJSON(String json) {
		def result = new JsonSlurper().parseText(json)
		UrbanCodeSnapshot report = new UrbanCodeSnapshot();
		report.name = result.name
		report.application = result.application
		report.description = result.description
		def versions  = result.versions
		versions.each { v ->
			def key = v.keySet().iterator().next()				
			report.addVersion(key, v.get(key))
		}
		return report
	}
	
	/**
	 * Comprime el fichero en un zip y lo sube a nexus en la ruta indicada
	 * @param jsonFile
	 * @param artifactId
	 * @param snapshot
	 * @param nexusURL
	 * @param urbanCodeGroupId
	 */
	public static void zipAndUpload(File jsonFile, String artifactId, String snapshot, String nexusURL, String urbanCodeGroupId,
		String nexusUser, String nexusPass, String gradleBin, String cScriptsStore) {

		println "Subiendo el artifactId ${artifactId}"
		TmpDir.tmp { tmpDir ->
			File tmp = new File(tmpDir.getCanonicalPath() + System.getProperty("file.separator")
				 + "descriptor.json")
			tmp.text = jsonFile.text
			println("El contenido de la ficha de despliegue que se va a subir es: \n ${tmp.text}");
			File zip = ZipHelper.addDirToArchive(tmpDir);
			try {
				println "Subiendo el descriptor.json parcial a Nexus:"
				NexusHelper.uploadTarNexus(nexusUser, nexusPass, gradleBin, cScriptsStore, 
					"fichas_despliegue", urbanCodeGroupId, artifactId, "${snapshot}", 
					nexusURL, "true", zip.getCanonicalPath(), "zip", {println it});
			} catch (Exception e) {
				println("[WARNING]: Ha habido un problmema subiendo el descriptor a Nexus:");
				throw e;
			}
			finally {
				zip.delete()
			}
		}
	}
	
	/**
	 * Añade un componente con su versión a la lista
	 * @param key Componente
	 * @param value Versión del componente
	 */
	public void addVersion(String key, String value) {
		Map<String, String> tmp = new HashMap<String, String>()
		// Se borra de versions el mapa que ya tiene el valor 
		// de artifactId incluido para no duplicarlo.
		def objsToRemove = versions.findAll({			
			it.keySet().contains(key);			
		});
		objsToRemove.each {
			versions.remove(it);
		}				
		tmp.put(key, value);
		versions << tmp 
	}	

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the application
	 */
	public String getApplication() {
		return application;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return the versions
	 */
	public List<Map<String, String>> getVersions() {
		return versions;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param application the application to set
	 */
	public void setApplication(String application) {
		this.application = application;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public String toString() {
		return this.toJSON();
	}
}
