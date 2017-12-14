package es.eci.utils.pom

import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel

import es.eci.utils.base.Loggable

/**
 * Esta clase permite resolver la URL de descarga y el nombre en formato
 * timestamp de una versión -SNAPSHOT de un artefacto en Nexus
 */
class MavenVersionResolver extends Loggable {

	//------------------------------------------------------------
	// Constantes de la clase
	private static final int BUFFER_SIZE = 10000;

	//------------------------------------------------------------
	// Propiedades de la clase

	// URL del servicio resolver de nexus
	// P. ej. http://nexus.elcorteingles.int/service/local/artifact/maven/resolve
	private String resolver

	//------------------------------------------------------------
	// Métodos de la clase

	/**
	 * Construye un objeto con la información necesaria para atacar a nexus
	 * @param resolver URL del servicio resolver de nexus
	 */
	public MavenVersionResolver(String resolver) {
		this.resolver = resolver;
	}

	/**
	 * Este método llama al servicio resolver para obtener la información necesaria
	 * del artefacto.  Documentado en:
	 * https://repository.sonatype.org/nexus-restlet1x-plugin/default/docs/path__artifact_maven_resolve.html
	 * Parámetros
	 g	Group id of the artifact (Required).	query	
	 a	Artifact id of the artifact (Required).	query	
	 v	Version of the artifact (Required) Supports resolving of "LATEST", "RELEASE" and snapshot versions ("1.0-SNAPSHOT") too.	query	
	 r	Repository that the artifact is contained in (Required).	query	
	 p	Packaging type of the artifact (Optional).	query	
	 c	Classifier of the artifact (Optional).	query	
	 e	Extension of the artifact (Optional).	query	
	 * @return
	 */
	public String resolveVersion(String repositoryId, MavenCoordinates coordinates) {
		String ret = null;
		if (coordinates != null) {
			// Componer la url
			Map<String, String> parameters = [ : ];
			parameters['g'] = coordinates.getGroupId();
			parameters['a'] = coordinates.getArtifactId();
			parameters['v'] = coordinates.getVersion();
			parameters['r'] = repositoryId;
			def appendParameter = { Map<String, String> p, String param, String value ->
				if (value != null) {
					p[param] = value
				}
			}
			appendParameter(parameters, 'p', coordinates.getPackaging());
			appendParameter(parameters, 'c', coordinates.getClassifier());
			StringBuilder sb = new StringBuilder(this.resolver);
			int count = 0;
			for (String param: parameters.keySet()) {
				if (count == 0) {
					sb.append("?");
				}
				else {
					sb.append("&")
				}
				sb.append(URLEncoder.encode(param, "UTF-8"));
				sb.append("=");
				sb.append(URLEncoder.encode(parameters[param], "UTF-8"));
				count++;
			}
			// Lanzar la URL contra nexus
			URL urlNexus = new URL(sb.toString());

			ReadableByteChannel versionReader =
					Channels.newChannel(urlNexus.openConnection().getInputStream());
			ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
			try{
				while(versionReader.read(buffer) > 0){
					//limit is set to current position and position is set to zero
					buffer.flip();
					String xml = ''
					while(buffer.hasRemaining()){
						xml += (char) buffer.get();
					}
					
					def versionParser = new XmlParser().parseText(xml)
					if (versionParser.data.version.size() != 1 ){
						throw new Exception('Se ha obtenido un número de vesiones distinto del esperado')
					} else {
						log('Version recuperada: ' + versionParser.data.version.get(0).value())
						ret = versionParser.data.version.get(0).value()
						ret = ret.substring(1, ret.length()-1)
					}
				}
			}catch(IOException e){
				e.printStackTrace();
			}finally{
				versionReader.close();
			}
		}
		return ret;
	}
}