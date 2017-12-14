package version.versioner

/*
 * GROOVY SCRIPT
 * Este script busca el fichero artifacts.json en el directorio de ejecución, 
 * y si existe, lo actualiza con el paso correspondiente.
 */

import es.eci.utils.*
import es.eci.utils.versioner.ArtifactsVariableHelper
import groovy.json.*

println "stepUpdateArtifactsJsonFile -> Inicio de ejecución..."

SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
def params = parameterBuilder.getSystemParameters();

def versionerStep = params["versionerStep"];
def action = params["action"];
def releaseMantenimiento = params["releaseMantenimiento"]

def jsonObject;
File currentDir = new File(".")
def artifactFile = new File(currentDir, "artifacts.json")
println "stepUpdateArtifactsJsonFile -> Fichero de artefactos: ${artifactFile}"

JsonSlurper js = new JsonSlurper();
if(artifactFile.exists()) {
	println "stepUpdateArtifactsJsonFile -> Leyendo el fichero artifacts.json..."
	jsonObject = js.parseText(artifactFile.getText());
}

// Indica si la cadena es válida: no nula y no tiene un valor con el placeholder
//	de jenkins
def isValid(String s) {
	return s != null &&
		 s.trim().length() > 0 &&
		 ! s.trim().equals("\${artifactsJson}");
}

if(jsonObject != null) {
	
	ArtifactsVariableHelper helper = new ArtifactsVariableHelper();
	helper.initLogger { println it }
	helper.updateArtifacts(jsonObject, versionerStep, action, releaseMantenimiento);

	// Actualizamos el parámetro artifactsJson si existiese.
	// Si no existiese actualizamos el archivo
	JsonBuilder builder = new JsonBuilder(jsonObject);
	def newArtifactsJson = builder.toString();
	if(isValid(newArtifactsJson) && artifactFile.exists()) {
		println("Modificamos el archivo artifacts.json con el contenido: ${newArtifactsJson}");
		artifactFile.text = newArtifactsJson;
	}
}
else {
	println "stepUpdateArtifactsJson -> No se ha encontrado el fichero"
}

