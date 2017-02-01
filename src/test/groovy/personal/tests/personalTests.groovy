def fs = System.getProperty("file.separator")

new File("${rootDir}").eachFileRecurse { File file ->
	if(file.isDirectory()) {
		boolean hasIt = false;
		file.eachFileRecurse {
			if(it.getName == "build.gradle") {
				hasIt = true;
			}
		}
		if(hasIt) {
			println("Including -> ${file.getName()}")
			include "${file.getName()}"
		} 		
	}
}
