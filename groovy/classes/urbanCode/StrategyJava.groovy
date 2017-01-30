package urbanCode;

import java.util.List;
import java.util.Map;

import es.eci.utils.LogUtils
import es.eci.utils.pom.ArtifactsFinder;
import es.eci.utils.pom.MavenCoordinates
import es.eci.utils.pom.MavenVersionResolver

public class StrategyJava implements Strategy{

	private List type;
	private static final String EAR = "ear";
	private static final String ZIP = "zip";

	public StrategyJava(File f){
		List tipos = [EAR, ZIP];
		this.setType(tipos);
	}

	@Override
	public List<MavenCoordinates> resolve(String apiNexus, String repositoryId, File ficheroBase, String component, MavenCoordinates v) {
		// Llamamos al artifactsFinder para recuperar las coordenadas
		String version = v.getVersion();
		Map<String, List<MavenCoordinates>> artefactos = ArtifactsFinder.findByPackaging(ficheroBase)
		List<MavenCoordinates> result = null;
		List arts = new ArrayList();
		List artsZip = new ArrayList();

		// Obtener los artefactos definidos en la estrategía (EAR, ZIP, ...)
		if (artefactos != null && artefactos != ""){
			this.type.each {t ->
				// Si el artefacto es de tipo EAR solo permitir una coincidencia
				if (t.equals(EAR)){
					if (artefactos.get(t) != null && artefactos.get(t).size() == 1){
						arts.add(artefactos.get(t).get(0));
					}
				}
				// Si es de tipo ZIP añadir todos
				else if (t.equals(ZIP)){
					artefactos.get(t).each{a ->
						artsZip.add(a)
					}
				}
			}
		}

		if (artsZip != null && artsZip.size() >0){
			artsZip.each { art ->
				System.out.println("**** CLASSIFIER: "  + art.getClassifier())
				if (result == null)
					result = new ArrayList<MavenCoordinates>()
				if (version.contains("SNAPSHOT")){
					version = this.obtenerCoordenadas(apiNexus, repositoryId, component + "-" + art.getClassifier(), art, ZIP).getVersion();
				}
				result.add(new MavenCoordinates(art.getGroupId(), component + "-" + art.getClassifier(), version));
			}
		}
		if (arts != null && arts.size() >0){
			arts.each { art ->
				if (result == null)
					result = new ArrayList<MavenCoordinates>();
				if (version.contains("SNAPSHOT")){
					version = this.obtenerCoordenadas(apiNexus, repositoryId, component, art, EAR).getVersion();
				}
				result.add(new MavenCoordinates(art.getGroupId(), component, version));
			}
		}

		if (result == null)
			System.out.println("Número de artefactos encontrado no apropiado");
		return result;
	}

	private MavenCoordinates obtenerCoordenadas(String apiNexus, String repositoryId, String component, MavenCoordinates coordinates, String type){
		//Resolvemos la version con timestamp
		MavenVersionResolver vRvr = new MavenVersionResolver(apiNexus);
		coordinates.setPackaging(type);
		def version = vRvr.resolveVersion(repositoryId, coordinates);
		// Se sustituye el nombre del artefacto con el componente de Urban Code
		return new MavenCoordinates(coordinates.getGroupId(), component, version);
	}

	private setType(List type){
		this.type = type;
	}

	private List getType(){
		return this.type;
	}

}