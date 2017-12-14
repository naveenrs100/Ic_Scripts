package jenkins

import hudson.model.*
import jenkins.model.*
import es.eci.utils.*
import es.eci.utils.pom.ArtifactsFinder
import es.eci.utils.pom.MavenCoordinates

// Parámetros
File folder = new File(build.buildVariableResolver.resolve("parentWorkspace"))

// 1- Buscar coordenadas de los artefactos agrupados por tipo
Map artefactos = ArtifactsFinder.findByPackaging(folder, null)

// 2- Tratar los artefactos y buscar sólo las coordenadas de los ear. Si hay más de un ear lanzar error
// Recorrer el mapa, indexado por área de proyecto
def key = "ear"
if (artefactos[key] == null ||artefactos[key].size() != 1){
	//Error, no puede existir más de un artefacto
	throw new Exception()
}else {
	LinkedList artefacto = artefactos[key]
	MavenCoordinates coords = artefacto.get(0)
	def groupId = coords.getGroupId()
	def artifactId = coords.getArtifactId()
	def version = coords.getVersion()

	ParamsHelper.addParams(build, ["groupId":groupId, "artifactId":artifactId, "version":version])
}