
File dir = new File("C:/Users/dcastro.jimenez/Desktop/android-sdk-linux");
File outputFile = new File("C:/Users/dcastro.jimenez/Desktop/file.txt")

dir.eachFileRecurse { File file ->
	if(!file.isDirectory()
		&& !file.getName().contains(".jar")
		&& !file.getName().contains(".java") 
		&& !file.getName().contains(".html")
		&& !file.getName().contains(".js")
		&& !file.getName().contains(".xml")
		&& !file.getName().contains(".so")
		&& !file.getName().contains(".apk")
	) {
		file.eachLine { String line ->
			if(line.toLowerCase().contains("update") 
				&& !line.contains("as updated from")) {
				println(file.getCanonicalPath());
				outputFile.append(file.getCanonicalPath() + "\n");
			}
		}
	}
}