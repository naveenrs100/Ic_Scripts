def prefix = "C:/Users/dcastro.jimenez/Desktop/tmp";
def parentWorkspaceFile = new File(prefix);

def notEmptyTargets = [];
parentWorkspaceFile.eachFileRecurse { File file ->
	if(file.getName() == "pom.xml") {
		def targetFile = new File(file.getParentFile().getCanonicalPath(),"target")
		if(targetFile.exists()) {
			println(targetFile.getCanonicalPath());
			if(targetFile.list().length > 0) {
				targetFile.deleteDir();
				println "añadiendo ${targetFile.getCanonicalFile()}";
				notEmptyTargets.add(targetFile);
			}
		}
	}
}

if(notEmptyTargets.size() > 0) {
	throw new Exception("Hay directorios target no vacíos: ${notEmptyTargets}");
}