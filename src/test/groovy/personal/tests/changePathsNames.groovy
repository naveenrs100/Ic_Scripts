def file = new File("C:/Users/dcastro.jimenez/git/CompanyGradlePlugin");

def startPaths = []
file.eachFileRecurse { File thisFile ->
	if(thisFile.getCanonicalPath().toLowerCase().contains("mapfre")) {
		println(thisFile.getCanonicalPath())
		startPaths.add(thisFile.getCanonicalPath())		
	}		
}

// Damos la vuelta al paths
def paths = [];
startPaths.reverseEach {
	paths.add(it)
}

def newPaths = []; 
paths.each { String path ->
	if(path.toLowerCase().contains("mapfre")) {
		newPaths.add(path.replace("Mapfre", "Company").replace("mapfre","company"));
	}
}

for(int i=0; i < paths.size(); i++) {
	def fileToRename = new File(paths[i]);
	println("Path a renombrar -> " + fileToRename.getCanonicalPath())
	fileToRename.renameTo(newPaths[i]);	
}