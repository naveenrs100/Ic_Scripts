package git

import es.eci.utils.ParamsHelper;

def parentWorkspaceDir = new File("${build.workspace}");
def commitsIds = "";
parentWorkspaceDir.eachFile { File file ->
  if(file.getName().endsWith("_lastCommit.txt")) {
	def propName = file.getName().split("_lastCommit.txt")[0]
	def commitId = file.text;
	commitsIds = commitsIds + "${propName}:${commitId},"
  }
}

if(!commitsIds.trim().equals("")) {
	commitsIds = commitsIds.substring(0, commitsIds.length() - 1);
}

ParamsHelper.addParams(build, ["commitsId":commitsIds]);