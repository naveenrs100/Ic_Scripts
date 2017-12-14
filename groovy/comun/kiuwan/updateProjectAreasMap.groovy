/**
 * Este script se invoca como Groovy Script simple.
 * 
 * Escribe en el directorio de ejecución los ficheros:
 * 
 * groups - relaciona grupos/streams con el nombre del área de proyecto.
 * Este fichero se usará en cada análisis de Kiuwan para relacionar el
 * análisis con un área de proyecto.
 * 
 * projectareas - lista exhaustivamente todas las áreas de proyecto dadas
 * de alta en QUVE, con su lista de streams/grupos y de componentes/repositorios.
 * Este fichero se usará para el lanzamiento de una línea base completa en
 * Kiuwan.
 * 
 * Parámetros
 * 
 * quveurl - URL base de Jenkins
 * jenkinsHome - Directorio base de Jenkins
 * parentWorkspace - Directorio de ejecución sobre el que se genera el mapa
 * urlGitlab - URL base de gitlab
 * privateToken - Token privado de gitlab
 * keystoreVersion - Versión de keystore a utilizar
 * urlNexus - URL Base de nexus
 * parentWorkspace - directorio de ejecución
 */

package kiuwan

@GrabResolver (name='nexus', root='http://nexus.elcorteingles.int/content/groups/public/')
@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.3.6')

import es.eci.utils.ParameterValidator
import es.eci.utils.Stopwatch
import es.eci.utils.StringUtil
import es.eci.utils.SystemPropertyBuilder
import git.GitlabClient
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class Group {	
	String name;
	List<String> components;
	String scm;
}

// Lee el token de sesión de QUVE almacenado en el fichero 
//	/jenkins/portalSessionKey
def getToken(def jenkinsHome){
	def sessionKeyFile = new File(jenkinsHome, "portalSessionKey")
	def sessionKey = "";
	sessionKeyFile.eachLine { line ->
		sessionKey = line
		return
	}
	def token = "{\"sessionKey\":\"${sessionKey}\"}"
	return token;
}

/**
 * Este método implementa la comunicación con QUVE.
 * @param baseurl URL base de QUVE.
 * @param path Servicio de QUVE al que atacamos
 * @param contentType Tipo de contenido a informar.
 * @param jenkinsHome Directorio raíz de jenkins
 */
def sendHttp(
		String baseurl,
		String path,
		def jenkinsHome){
	String ret = null;
	long millis = Stopwatch.watch {
		
		String contentType = "application/json"
		def url = "${baseurl}/${path}?quvetoken=" + getToken(jenkinsHome);
		def encoding = "UTF-8"

		// Envío del comando
		HttpURLConnection con = new URL(url).openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("Content-Type", contentType);
		// Resultado
		int responseCode = con.getResponseCode();
		System.out.println("Response Code : " + responseCode);

		BufferedReader inReader = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuilder response = new StringBuilder();

		while ((inputLine = inReader.readLine()) != null) {
			response.append(inputLine);
		}
		inReader.close();
		
		//print result
		ret = response.toString();
	}

	println "sendHttp: $millis ms."
	return ret
}

// Calcula el nombre de la stream a partir de las distintas streams
//	definidas
def getStream(streams) {
	def streamName = null;
	List names = []
	streams.each { def stream ->
		def tmp = StringUtil.trimStreamName(stream.name);
		if (!names.contains(tmp)) {
			names << tmp
		}
	}
	if (names.size() == 1) {
		streamName = names[0]
	}	
	return streamName;
}

// Actualiza el grupo en la lista con el nombre indicado y la 
//	lista de los componentes mezclados
def updateGroup(GitlabClient client, List<Group> groups, String streamName, streams) {
	Group g = groups.find { it.name == streamName }
	if (g == null) {
		g = new Group();
		g.name = streamName
		g.components = []
		// Si aparece en git, cambiar el scm
		if (client.get("groups", ['search':streamName]) != '[]') {
			g.scm = 'gitlab'
		}
		else {
			g.scm = 'RTC'
		}
		groups << g
	}
	streams.each { def stream ->
		stream.components.each { def component ->
			String componentName = component.name
			if (!g.components.contains(componentName)) {
				g.components << componentName
			}
		}
	}
}

// Obtiene el nombre normalizado del área de proyecto
def getProjectArea(projectArea) {
	return StringUtil.normalizeProjectArea(projectArea.name)
}

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

def params = propertyBuilder.getSystemParameters()
def quveurl  = params.get("quveurl");
def jenkinsHome = params.get("jenkinsHome");
def urlGitlab = params.get("urlGitlab");
def privateToken  = params.get("privateToken");
def urlNexus = params.get("urlNexus");
def keystoreVersion  = params.get("keystoreVersion");
String excludedAreas = params.get("exclusiones")

List<String> exclusions = []
excludedAreas.eachLine {
	exclusions << it
}

File parentWorkspace = new File(params.get("parentWorkspace").toString())

GitlabClient client = new GitlabClient(urlGitlab, privateToken, keystoreVersion, urlNexus);

ParameterValidator.builder().
	add("quveurl", quveurl).
	add("jenkinsHome", jenkinsHome).build().validate();

/*def quveurl = build.getEnvironment(null).get("QUVE_URL")
def jenkinsHome = build.getEnvironment(null).get("JENKINS_HOME")*/
println "Llamando a QUVE..."
String response =  sendHttp(quveurl, 'admin/products', jenkinsHome);
def obj = new JsonSlurper().parseText(response)
// Recorrer el array de elementos
Map<String, String> groupsToProjectAreas = [:]
Map<String, List<Group>> projectAreasToGroups = [:] 

// Exclusiones
obj.elements.each { def element ->
	// Cada element es un producto
	String stream = getStream(element.streams);
	String projectArea = getProjectArea(element.projectArea);
	if (!excludedAreas.contains(projectArea)) {
		if (stream == null) {
			println "ERROR: No se puede determinar la corriente para ${element.name}"
		}
		else {
			if (groupsToProjectAreas.containsKey(stream)) {
				// informar del error
				println "ERROR: Elemento repetido: " + stream
			}
			else {
				groupsToProjectAreas[stream] = projectArea
				List<Group> groups = []
				if (projectAreasToGroups.containsKey(projectArea)) {
					groups = projectAreasToGroups[projectArea]
				}
				updateGroup(client, groups, stream, element.streams)
				projectAreasToGroups[projectArea] = groups
			}
		}
	}
	else {
		println "$projectArea se excluye"
	}
}
// Fichero generado: relación de grupo a área de proyecto
// Útil en el lanzamiento de análisis desde los jobs de jenkins
File outputGroups = new File(parentWorkspace, "groups")
outputGroups.text = JsonOutput.prettyPrint(JsonOutput.toJson(groupsToProjectAreas))
// Fichero generado: relación de productos, subsistemas y componentes
// Útil en el lanzamiento de líneas base masivas
File outputPA = new File(parentWorkspace, "projectareas")
outputPA.text = JsonOutput.prettyPrint(JsonOutput.toJson(projectAreasToGroups))

