package urbanCode;

import java.util.List;
import java.util.Map;

import es.eci.utils.LogUtils
import es.eci.utils.pom.ArtifactsFinder;
import es.eci.utils.pom.MavenCoordinates
import es.eci.utils.pom.MavenVersionResolver

public class StrategyRegistroUnico implements Strategy{

	private static final ARTIFACT_ID = "platform";
	
	@Override
	public List<MavenCoordinates> resolve(String apiNexus, String repositoryId, File ficheroBase, String component, MavenCoordinates v) {
		
		String version = v.getVersion(); 
		//   1- Si la versión es SNAPSHOT se obtiene el timestamp de NEXUS
		if (version.contains("SNAPSHOT")) {
			version = this.obtenerCoordenadas(apiNexus, repositoryId, component, v)
		}
		//   4- Se componen las coordenadas para el json con el nombre del componente de urbanCode y la versión
		List result = new ArrayList<MavenCoordinates>();
		result.add(new MavenCoordinates(v.getGroupId(), component, version));
		
		return result;
	}

	private MavenCoordinates obtenerCoordenadas(String apiNexus, String repositoryId, String component, MavenCoordinates coordinates){
		//Resolvemos la version con timestamp
		MavenVersionResolver vRvr = new MavenVersionResolver(apiNexus);
		MavenCoordinates c = new MavenCoordinates(coordinates.getGroupId(), ARTIFACT_ID, coordinates.getVersion());
		def version = vRvr.resolveVersion(repositoryId, c);
		// Se sustituye el nombre del artefacto con el componente de Urban Code
		return new MavenCoordinates(c.getGroupId(), component, version);
	}
}