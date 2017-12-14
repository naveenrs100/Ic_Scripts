package version.versioner

/*
 * SYSTEM GROOVY SCRIPT
 * Este script busca la variable artifactsJson en el entorno, y si está definida,
 * la actualiza con el paso correspondiente.
 */

import es.eci.utils.*
import es.eci.utils.versioner.ArtifactsVariableHelper
import groovy.json.*

println "stepUpdateArtifactsJson -> Inicio de ejecución..."

def artifactsJson = build.buildVariableResolver.resolve("artifactsJson");
println "stepUpdateArtifactsJson -> artifactsJson : $artifactsJson"
def versionerStep = build.buildVariableResolver.resolve("versionerStep");
def action = build.buildVariableResolver.resolve("action");
def releaseMantenimiento = build.buildVariableResolver.resolve("releaseMantenimiento");

def jsonObject;

JsonSlurper js = new JsonSlurper();
if(artifactsJson != null && !artifactsJson.trim().equals("") && !artifactsJson.trim().equals("\${artifactsJson}")) {
	println "stepUpdateArtifactsJson -> Leyendo el parámetro artifactsJson..."
	jsonObject = js.parseText(artifactsJson);
}

if(jsonObject != null) {
	
	ArtifactsVariableHelper helper = new ArtifactsVariableHelper();
	helper.initLogger { println it }
	helper.updateArtifacts(jsonObject, versionerStep, action, releaseMantenimiento);

	// Actualizamos el parámetro artifactsJson si existiese.
	// Si no existiese actualizamos el archivo
	JsonBuilder builder = new JsonBuilder(jsonObject);
	def newArtifactsJson = builder.toString();
	if(artifactsJson != null && !artifactsJson.trim().equals("") && !artifactsJson.trim().equals("\${artifactsJson}")) {		
		def params = [:];
		params.put("artifactsJson",newArtifactsJson);

		def parent = new GlobalVars().getParentBuild(build);

		if (parent != null) {
			ParamsHelper.deleteParams(parent, "artifactsJson")
			ParamsHelper.addParams(parent, params);
		}
	}
}
else {
	println "stepUpdateArtifactsJson -> No se ha encontrado la variable"
}

