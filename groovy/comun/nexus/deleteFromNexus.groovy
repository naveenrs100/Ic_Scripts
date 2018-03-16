package nexus;

@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')
@Grab(group='org.apache.xmlbeans', module='xmlbeans', version='2.6.0')

import groovy.json.JsonOutput;
import java.util.regex.Pattern
import org.apache.xmlbeans.impl.schema.PathResourceLoader;
import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.versioner.VersionDigits;
import static groovyx.net.http.Method.DELETE;
import groovyx.net.http.HTTPBuilder


SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
def params = parameterBuilder.getSystemParameters();

String versionType = params["versionType"];
String groupId = params["groupId"];
String nexusUrl = params["nexusUrl"];
String nexusUser = params["nexusUser"];
String nexusPass = params["nexusPass"];
String repository = params["repository"];
String manualArtifact = params["manualArtifact"].startsWith("\${") ? "" : params["manualArtifact"];
Boolean dry_run = Boolean.valueOf(params["dry_run"]);
int remainingVersions = Integer.valueOf(params["remainingVersions"]);


if(groupId != null && !groupId.trim().equals("")) {

	Pattern releasePattern = ~/.*(\d+\.)+0$/
	Pattern fixPattern = ~/.*(\d+\.)+[1-9](\d*)$/
	Pattern hotfixPattern = ~/.*(\d+\.)+[0-9]+-(\d*)$/

	Pattern versionPattern;

	if(versionType.toLowerCase().equals("release")) {
		versionPattern = releasePattern;
	}
	else if(versionType.toLowerCase().equals("fix")) {
		versionPattern = fixPattern;
	}
	else if(versionType.toLowerCase().equals("hotfix")) {
		versionPattern = hotfixPattern;
	}

	def nexusServiceUrl = nexusUrl.split("/content/repositories")[0]

	def xml = "${nexusServiceUrl}/service/local/lucene/search?g=${groupId}".toURL().text

	def root = new XmlParser().parseText(xml)

	def artifacts = []
	def rcVersions = []

	root.data.artifact.each {
		def artifactId = it.artifactId.text();
		if(!artifacts.contains(artifactId)) {
			artifacts.add(artifactId);
		}
	}

	def artifactsVersionsMap = [:];
	for(artifact in artifacts) {
		def artifactSectionAll = root.data.artifact.findAll { it.artifactId.text().equals(artifact) }

		ArrayList<String> versions = new ArrayList<String>();
		artifactSectionAll.each { artifactSection ->
			if(versionPattern.matcher(artifactSection.version.text())) {
				versions.add(artifactSection.version.text());
			}
		}

		def versionDigitsList = [];
		versions.each {
			versionDigitsList.add(new VersionDigits(it));
		}

		def sortedVersionObjects = versionDigitsList.sort { it };
		println("Versiones de \"${artifact}\" ->")
		def orderedVersionList = []
		sortedVersionObjects.each {
			orderedVersionList.add(it.getVersion());
		}
		println orderedVersionList;
		def filteredVersionsList = filterVersions(remainingVersions, orderedVersionList);
		println filteredVersionsList;
		artifactsVersionsMap.put(artifact, filteredVersionsList);

	}

	def jsonVersionsToDelete = JsonOutput.toJson(artifactsVersionsMap);
	println("\nVersiones a borrar:")
	println JsonOutput.prettyPrint(jsonVersionsToDelete);

	if(!dry_run) {
		artifactsVersionsMap.keySet().each { String key ->
			if(manualArtifact == null || manualArtifact.trim().equals("")) {
				String artifact = key;
				ArrayList versions = artifactsVersionsMap.get(key);
				println("Se va a borrar para el artefacto: \"${key}\"");
				deleteArtifactFromNexus(groupId, artifact, versions, nexusUrl, nexusUser, nexusPass, repository);
			}
			else if(manualArtifact != null && !manualArtifact.trim().equals("")) {
				if(key.equals(manualArtifact)) {
					String artifact = key;
					ArrayList versions = artifactsVersionsMap.get(key);
					println("Se va a borrar para el artefacto: \"${key}\"");
					deleteArtifactFromNexus(groupId, artifact, versions, nexusUrl, nexusUser, nexusPass, repository);
				}
			}
		}
	}

}

/**
 * Llamada a la API de Nexus para borrar las rutas correspondientes a 
 * las versiones que sobran de cada artefacto del groupId pasado.
 * @param groupId
 * @param artifact
 * @param versions
 * @param nexusUrl
 * @param nexusUser
 * @param nexusPass
 * @param repository
 * @return
 */
public deleteArtifactFromNexus(String groupId, String artifact, ArrayList versions, String nexusUrl, String nexusUser, String nexusPass, String repository) {
	versions.each { String version ->
		def relativePathToDelete = groupId.replaceAll("\\.","/") + "/" + artifact + "/" + version;
		def pathToDelete = "${nexusUrl}/${repository}/${relativePathToDelete}"

		println "Vamos a borrar: \"${pathToDelete}\"";

		def httpBuilder = new HTTPBuilder(pathToDelete);
		httpBuilder.auth.basic "${nexusUser}", "${nexusPass}";

		httpBuilder.request(DELETE) { req ->
			response.success = { resp ->
				assert (resp.status == 200 || resp.status == 202 || resp.status == 204)
				println("request was successful: Deleting " + pathToDelete)
			}

			response.failure = { resp ->
				assert (resp.status != 200 && resp.status != 202 && resp.status != 204)
				println("request failed. Status: " + resp.status)
			}
		}
	}

}

/**
 * Filtra las versiones devolviendo sólo las que hay que borrar.
 * @param remainingVersions
 * @param versionList
 * @return
 */
public ArrayList filterVersions(int remainingVersions, List versionList) {
	int endIndex = versionList.size() - remainingVersions;
	ArrayList<String> ret = new ArrayList<String>();
	if(endIndex >= 0) {
		ret = versionList.subList(0, endIndex);
	}

	return ret;
}
