package jenkins

String artifacts = System.getenv("artifactsFile")

if (artifacts != null) {
	File f = new File("artifacts.json")
	f.text = artifacts
}