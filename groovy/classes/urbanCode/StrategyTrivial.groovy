package urbanCode;

import java.util.List;
import java.util.Map;

import es.eci.utils.LogUtils
import es.eci.utils.pom.ArtifactsFinder;
import es.eci.utils.pom.MavenCoordinates
import es.eci.utils.pom.MavenVersionResolver

/**
 * Esta estrategia urban code es la más trivial posible: se limita a tomar
 * el valor del parámetro componente y el contenido del fichero version.txt
 * para generar en Nexus el descriptor.json.  No contempla la resolución de 
 * versiones -SNAPSHOT (es totalmente trivial).
 */
public class StrategyTrivial implements Strategy{

	
	public StrategyTrivial(File f){
	}

	@Override
	public List<MavenCoordinates> resolve(
			String apiNexus, 
			String repositoryId, 
			File ficheroBase, 
			String component, 
			MavenCoordinates v) {
		List<MavenCoordinates> ret = new LinkedList<MavenCoordinates>();
		MavenCoordinates coord = new MavenCoordinates(v.getGroupId(), component, v.getVersion());
		ret.add(coord);
		return ret;
	}
			
}