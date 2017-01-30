import es.eci.utils.ComponentVersionHelper;
import es.eci.utils.SystemPropertyBuilder;
import es.eci.utils.ParamsHelper;

import java.util.Map;
import java.util.regex.*;


def build = Thread.currentThread().executable;
def resolver = build.buildVariableResolver;

SystemPropertyBuilder propBuilder = new SystemPropertyBuilder();
def propertiesMap = propBuilder.getSystemParameters();

def stream = resolver.resolve("stream"); println("stream = ${stream}")
def component = resolver.resolve("component"); println("component = ${component}")
def scmToolsHome = build.getEnvironment(null).get("SCMTOOLS_HOME"); println("scmToolsHome = ${scmToolsHome}")
def user = build.getEnvironment(null).get("userRTC"); println("user = ${user}")
def password = resolver.resolve("pwdRTC"); println("password = ${password}")
def repository = build.getEnvironment(null).get("urlRTC"); println("repository = ${repository}")

// Obtenemos el "version" para contemplar el caso en el que éste se haya indicado directamente.
def version = resolver.resolve("version");
if(version == null || version.trim().equals("")) {
	// Si el parámetro "version" no viene informado lo calculamos mediante la última baseline del componente en RTC.
	println("No hay un parámetro \"version\" informado. Lo calculamos mediante la última baseline de RTC...");
	ComponentVersionHelper componentVersionHelper = new ComponentVersionHelper(scmToolsHome);

	List<String> listaBaselines = componentVersionHelper.getBaselines(component, stream, user, password, repository);

	List<String> listaBaselinesSnapshot = [];
	List<String> listaBaselinesFinal = [];

	listaBaselines.each { String baseline ->
		Matcher finalVersionMatcher = Pattern.compile(/[0-9]{1,10}\.[0-9]{1,10}\.[0-9]{1,10}\.[0-9]{1,10}$/).matcher(baseline.trim());
		Matcher snapshotVersionMatcher = Pattern.compile(/[0-9]{1,10}\.[0-9]{1,10}\.[0-9]{1,10}\.[0-9]{1,10}-SNAPSHOT$/).matcher(baseline.trim());
		if(finalVersionMatcher) {
			listaBaselinesFinal.add(baseline);
		}
		if(snapshotVersionMatcher) {
			listaBaselinesSnapshot.add(baseline);
		}
	}

	println("Ultima version SNAPSHOT:");
	def lastSnapshotVersion;
	if(listaBaselinesSnapshot.size() > 0) {
		lastSnapshotVersion = listaBaselinesSnapshot.first();
		println(lastSnapshotVersion);
	} else {
		println("No hay baselines con versión Snapshot")
	}


	println("Ultima version CERRADA:");
	def lastClosedVersion;
	if(listaBaselinesFinal.size() > 0) {
		lastClosedVersion = listaBaselinesFinal.first();
		println(lastClosedVersion);
	} else {
		println("No hay baselines con versión cerrada")
	}

	def versionParam = [:];
	versionParam.put('version',lastClosedVersion);
	ParamsHelper.deleteParams(build, ["version"].toArray(new String[1]));
	ParamsHelper.addParams(build, versionParam);


} else {
	// Si el parámetro "version" ya viene informado.
	println("Hay informada un parámetro \"version\" directamente: \"${version}\".");
	def versionParam = [:];
	versionParam.put('version',version);
	ParamsHelper.deleteParams(build, ["version"].toArray(new String[1]));
	ParamsHelper.addParams(build, versionParam);
}


