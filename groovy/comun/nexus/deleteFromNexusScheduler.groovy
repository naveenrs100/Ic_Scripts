package nexus;

import es.eci.utils.SystemPropertyBuilder
import groovy.json.JsonOutput;

SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
def params = parameterBuilder.getSystemParameters();

String versionType = params["versionType"];
String groupId = params["groupId"];
String nexusUrl = params["nexusUrl"];
String nexusUser = params["nexusUser"];
String nexusPass = params["nexusPass"];
String repository = params["repository"];
String manualArtifact = params["manualArtifact"].startsWith("\${") ? "" : params["manualArtifact"];
String parentWorkspace = params["parentWorkspace"];
String remainingVersions = params["remainingVersions"];


File scheduleFile;

if(manualArtifact == null || manualArtifact.trim().equals("") || manualArtifact.trim().equals("\${manualArtifact}")) {
	scheduleFile = new File("${parentWorkspace}","${groupId}_${versionType}.txt");
	scheduleFile.createNewFile();
} else {
	scheduleFile = new File("${parentWorkspace}","${groupId}_${manualArtifact}_${versionType}.txt");
	scheduleFile.createNewFile();
}

HashMap<String,String> deleteParametersMap = new HashMap<String,String>();

deleteParametersMap.put("versionType",versionType);
deleteParametersMap.put("groupId",groupId);
deleteParametersMap.put("repository",repository);
deleteParametersMap.put("remainingVersions",remainingVersions);
if(manualArtifact != null && !manualArtifact.trim().equals("")) {
	deleteParametersMap.put("manualArtifact",manualArtifact);
} else {
	deleteParametersMap.put("manualArtifact","");
}

String jsonParameters = JsonOutput.prettyPrint(JsonOutput.toJson(deleteParametersMap));

scheduleFile.text = jsonParameters;



