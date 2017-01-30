/*
 * SYSTEM GROOVY SCRIPT
 * Este script busca la variable artifactsJson en el entorno, y si está definida,
 * la actualiza con el paso correspondiente.
 */

import es.eci.utils.*
import es.eci.utils.versioner.ArtifactsVariableHelper
import groovy.json.*

println "stepUpdateArtifactsJson -> Inicio de ejecución..."

def build = Thread.currentThread().executable;
def resolver = build.buildVariableResolver;

def artifactsJson = resolver.resolve("artifactsJson");
println "stepUpdateArtifactsJson -> artifactsJson : $artifactsJson"
def versionerStep = resolver.resolve("versionerStep");
def increaseIndex = resolver.resolve("increaseIndex");

def jsonObject;

JsonSlurper js = new JsonSlurper();
if(artifactsJson != null && !artifactsJson.trim().equals("") && !artifactsJson.trim().equals("\${artifactsJson}")) {
	println "stepUpdateArtifactsJson -> Leyendo el parámetro artifactsJson..."
	jsonObject = js.parseText(artifactsJson);
}

if(jsonObject != null) {
	
	ArtifactsVariableHelper helper = new ArtifactsVariableHelper();
	helper.initLogger { println it }
	helper.updateArtifacts(jsonObject, versionerStep, increaseIndex);

	// Actualizamos el parámetro artifactsJson si existiese.
	// Si no existiese actualizamos el archivo
	JsonBuilder builder = new JsonBuilder(jsonObject);
	def newArtifactsJson = builder.toString();
	if(artifactsJson != null && !artifactsJson.trim().equals("") && !artifactsJson.trim().equals("\${artifactsJson}")) {		
		def params = [:];
		params.put("artifactsJson",newArtifactsJson);

		def parent = GlobalVars.getParentBuild(build);

		if (parent != null) {
			ParamsHelper.deleteParams(parent, "artifactsJson")
			ParamsHelper.addParams(parent, params);
		}
	}
}
else {
	println "stepUpdateArtifactsJson -> No se ha encontrado la variable"
}

