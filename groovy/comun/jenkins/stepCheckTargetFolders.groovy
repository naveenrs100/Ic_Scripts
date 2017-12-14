import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.TargetFoundException

SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
def params = parameterBuilder.getSystemParameters();

def parentWorkspaceFile = new File(params["parentWorkspace"]);

def notEmptyTargets = [];
parentWorkspaceFile.eachFileRecurse { File file ->
	if(file.getName() == "pom.xml") {
		def targetFile = new File(file.getParentFile().getCanonicalPath(),"target")
		if(targetFile.exists()) {
			println(targetFile.getCanonicalPath());
			if(targetFile.list().length > 0) {		
				targetFile.deleteDir();		
				notEmptyTargets.add(targetFile);
			}
		}
	}
}

if(notEmptyTargets.size() > 0) {
	String directories = "";
	for(String dir : notEmptyTargets) {
		directories = directories + "\t - ${dir}\n";
	}
	throw new TargetFoundException("\n\n------ [ERROR] Hay directorios target no vacíos subidos a RTC o Git. Límpielos antes de ejecutar otra construcción. " + 
									"Son los siguientes:\n ${directories}");
} else {
	println("##### No se han detectado directorios target con contenido en la descarga.")
}