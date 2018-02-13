/**
 * 
 */
package es.eci.utils.pom

import java.util.regex.Pattern

import es.eci.utils.VersionUtils

/**
 * Localiza un artefacto por sus coordenadas en un juego de poms
 */
class ArtifactsFinder {

	/**
	 * Busca uno o varios artefactos con un determinado packaging y, opcionalmente,
	 * que cumplan un determinado patrón de nombre.
	 * @param folder Directorio base de pom.xml
	 * @param regex [Opcional] Expresión regular que debe cumplir el nombre del artefacto
	 * @return Tabla de listas de coordenadas, por tipo de empaquetado
	 */
	public static Map<String, List<MavenCoordinates>> findByPackaging(File folder, String regex = null) {
		Map<String, List<MavenCoordinates>> ret = new HashMap<String, List<MavenCoordinates>>();
		PomTree tree = new PomTree(folder)
		for (Iterator<PomNode> it = tree.widthIterator(); it.hasNext() ; ) {
			PomNode pom = it.next();
			Node xml = new XmlParser().parse(pom.getFile())
			VersionUtils utils = new VersionUtils()
			String artifactId = solve(pom, xml.artifactId.text());
			String groupCandidate = null
			
			if (xml.groupId != null && xml.groupId.text() != null && xml.groupId.text() != "") {
				groupCandidate = xml.groupId.text()
			}
			else if (xml.parent != null && xml.parent.groupId.text() != null) {
				groupCandidate = xml.parent.groupId.text()
			}
			String groupId = solve(pom, groupCandidate);
			String versionCandidate = null
			if (xml.version.text() != null && xml.version.text() != "") {
				versionCandidate = xml.version.text()
			}
			else if (xml.parent.version != null) {
				versionCandidate = xml.parent.version.text()
			}
			String version = solve(pom, versionCandidate);
			String pomPackaging = xml.packaging.text()
			
			String classifier=null
			// En caso de utilizar assembly-plugin hay que parsear el descriptor para obtener tipo y classifier
			String assemblyCandidate = null
			if (xml.build.plugins.plugin.artifactId.text() == "maven-assembly-plugin") {
				assemblyCandidate = xml.build.plugins.plugin.configuration.descriptors.descriptor.text()
				if (assemblyCandidate != null && assemblyCandidate != ""){
					File descriptor = new File(pom.getFile().toString().replaceAll("pom.xml","") + assemblyCandidate)
					def assembly = new XmlSlurper().parseText(descriptor.getText())
					classifier = assembly.id;
					pomPackaging = assembly.formats.format;
					
				}
			}
			
			List<MavenCoordinates> coords = null;
			if (ret.containsKey(pomPackaging)) {
				coords = ret.get(pomPackaging)
			}
			else {
				coords = new LinkedList<MavenCoordinates>()
				ret.put(pomPackaging, coords)
			}
			// ¿Se debe filtrar el artefacto por expresión regular?
			if (regex == null || artifactId =~ regex) { 
				MavenCoordinates c = new MavenCoordinates(groupId, artifactId, version);
				if (classifier != null)
					c.setClassifier(classifier)
				coords << c;
			}
						
		}
		return ret
	}
	
	/** 
	 * Resuelve recursivamente una propiedad en un pom y, si no, subiendo la jerarquía
	 * @param pom Fichero pom
	 * @param property Valor de una propiedad
	 */
	private static String solve(PomNode pom, String property) {
		VersionUtils utils = new VersionUtils();
		PomNode actual = pom
		Node xml = new XmlParser().parse(pom.getFile())
		String ret = utils.solve(xml, property)
		while (ret == ''  && actual.getParent() != null) {
			 actual = actual.getParent()
			 xml = new XmlParser().parse(actual.getFile())
			 ret = utils.solve(xml, property) 
		}
		return ret;
	}
}
