import groovy.json.JsonSlurper;
import hudson.model.ParametersAction;

def workspaceDir = new File("${parentWorkspace}");
Boolean ok = true;
workspaceDir.eachFile { File file ->
	Map jobParams = [:];
		
	def jsonObject = new JsonSlurper().parseText(file.text);
		
	jobParams.put("groupId",jsonObject.groupId);
	jobParams.put("manualArtifact",jsonObject.manualArtifact);
	jobParams.put("versionType",jsonObject.versionType);
	jobParams.put("repository",jsonObject.repository);
	jobParams.put("remainingVersions",jsonObject.remainingVersions);
		
	
	b = build(jobParams, "DeleteFromNexus");
	
	if(b.getResult()!=SUCCESS) {
		ok = false;
	}
	
}

if(!ok) {
	build.getState().setResult(FAILURE);
}
