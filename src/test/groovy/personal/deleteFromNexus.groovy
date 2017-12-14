import groovy.json.JsonOutput;
import groovyx.net.http.HTTPBuilder
import java.util.regex.Pattern
import org.apache.xmlbeans.impl.schema.PathResourceLoader;
import es.eci.utils.versioner.VersionDigits;
import static groovyx.net.http.Method.DELETE;

String groupId = "es.eci.release.prueba";
String nexusUrl = "http://nexus.elcorteingles.pre/content/repositories";
String repository = "eci";
String manualArtifact = "";
int remainingVersions = 2;

if(groupId != null && !groupId.trim().equals("")) {

	Pattern releasePattern = ~/.*(\d+\.)+0$/
	Pattern fixPattern = ~/.*(\d+\.)+[1-9](\d*)$/
	Pattern hotfixPattern = ~/.*(\d+\.)+[0-9]-(\d*)$/

	Pattern versionPattern = hotfixPattern;

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
		println("Versiones de \"${artifact}\":")
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


	artifactsVersionsMap.keySet().each { String key ->
		if(manualArtifact == null || manualArtifact.trim().equals("")) {
			String artifact = key;
			ArrayList versions = artifactsVersionsMap.get(key);
			println("Borrando para el artefacto: \"${key}\"");
			deleteArtifactFromNexus(groupId, artifact, versions, nexusUrl, repository);			
		}
		else if(manualArtifact != null && !manualArtifact.trim().equals("")) {
			if(key.equals(manualArtifact)) {
				String artifact = key;
				ArrayList versions = artifactsVersionsMap.get(key);
				println("Borrando para el artefacto: \"${key}\"");
				deleteArtifactFromNexus(groupId, artifact, versions, nexusUrl, repository);
			}
		}
	}


	/***************************************************************/
	//		if(!artifacts.containsKey(artifactId)) {
	//			artifacts.put(artifactId,group)
	//		}
	//
	//		if(rcPattern.matcher(version)) {
	//			if(!rcVersions.contains(version)) {
	//				rcVersions.add(version)
	//			}
	//		}
	//	}
	//	println("Artifacts")
	//	for(a in artifacts){
	//		println(a)
	//	}
	//
	//	def versions = []
	//	for(rcVersion in rcVersions) {
	//		//def finalVersion = rcVersion.split("-").first()
	//		if(!versions.contains(rcVersion)) {
	//			versions.add(rcVersion)
	//		}
	//	}
	//
	//
	//	for(version in versions) {
	//		def tmpRCs = []
	//		for(rcVersion in rcVersions) {
	//			if(rcVersion.startsWith(version)) {
	//				def key = rcVersion.split("-RC")[1]
	//				def value = rcVersion
	//				tmpRCs.add(key.toInteger())
	//			}
	//		}
	//
	//		tmpRCs.sort{ -it }
	//
	//		println("RCs existentes: ")
	//		for(a in tmpRCs) {
	//			println(version + "-RC" + a)
	//		}
	//
	//		if(tmpRCs.size() - remainingRC.toInteger() > 0) {
	//			def cnt = 0
	//			//for(int i = 0; i < tmpRCs.size() - remainingRC.toInteger(); i++) {
	//			for(tmpRC in tmpRCs) {
	//				cnt++
	//				if(cnt > remainingRC.toInteger()){
	//					// Iterate over artifacts[] and tmpRC[] to delete versions.
	//					for(artifact in artifacts) {
	//						def artifactGroup = artifact.getValue()
	//						def artifactName = artifact.getKey()
	//						def relativePathToDelete = artifactGroup.replaceAll("\\.","/") + "/" + artifactName + "/" + version + "-RC" + tmpRC.toString()
	//						def pathToDelete = "${nexusUrl}/releases/${relativePathToDelete}"
	//
	//						println("pathToDelete = " + pathToDelete)
	//
	//						def httpBuilder = new HTTPBuilder(pathToDelete)
	//						httpBuilder.auth.basic 'admin', 'N3ur0na'
	//
	//						httpBuilder.request(DELETE) { req ->
	//							response.success = { resp ->
	//								assert (resp.status == 200 || resp.status == 202 || resp.status == 204)
	//								println("request was successful: Deleting " + pathToDelete)
	//							}
	//
	//							response.failure = { resp ->
	//								assert (resp.status != 200 && resp.status != 202 && resp.status != 204)
	//								println("request failed. Status: " + resp.status)
	//							}
	//						}
	//					}
	//				}
	//			}
	//		}
	//	}
	//} else { // Erase from every com.mapfre.direct groupId from the Nexus repository.
	//	println("\nERROR: Please, indicate a groupId.")
	//
}

public deleteArtifactFromNexus(String groupId, String artifact, ArrayList versions, String nexusUrl, String repository) {
	versions.each { String version ->
		def relativePathToDelete = groupId.replaceAll("\\.","/") + "/" + artifact + "/" + version;
		def pathToDelete = "${nexusUrl}/${repository}/${relativePathToDelete}"

		println "Vamos a borrar: \"${pathToDelete}\"";

		def httpBuilder = new HTTPBuilder(pathToDelete);
		httpBuilder.auth.basic 'U_ICNEXUS', 'zru3y4Yf';

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

public ArrayList filterVersions(int remainingVersions, List versionList) {

	int endIndex = versionList.size() - remainingVersions;

	ArrayList<String> ret = new ArrayList<String>();
	if(endIndex >= 0) {
		ret = versionList.subList(0, endIndex);
	}

	return ret;
}
