package omnistore

import es.eci.utils.ComponentVersionHelper;
import es.eci.utils.SystemPropertyBuilder;
import es.eci.utils.ParamsHelper;
import java.util.Map;
import java.util.regex.*;

SystemPropertyBuilder propBuilder = new SystemPropertyBuilder();
def propertiesMap = propBuilder.getSystemParameters();

def stream = build.buildVariableResolver.resolve("stream"); println("stream = ${stream}")
def component = build.buildVariableResolver.resolve("component"); println("component = ${component}")
def scmToolsHome = build.getEnvironment(null).get("SCMTOOLS_HOME"); println("scmToolsHome = ${scmToolsHome}")
def user = build.getEnvironment(null).get("userRTC"); println("user = ${user}")
def password = build.buildVariableResolver.resolve("pwdRTC"); println("password = ${password}")
def repository = build.getEnvironment(null).get("urlRTC"); println("repository = ${repository}")

// Obtenemos el parámetro "version" para contemplar el caso en el que éste se haya indicado directamente.
def version = build.buildVariableResolver.resolve("version");
if(version == 'DESARROLLO' || version == 'RELEASE') {
	println("El parámetro \"version\" viene informado como \"DESARROLLO\" o \"RELEASE\". Lo calculamos mediante la última baseline de RTC...");
	ComponentVersionHelper componentVersionHelper = new ComponentVersionHelper(scmToolsHome);
	componentVersionHelper.initLogger { println it }

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

	def isNull = { String s ->
		return s == null || s.trim().length() == 0;
	}
	
	def versionParam = [:];
	if (version == 'RELEASE') {
		if (isNull(lastClosedVersion)) {
			throw new NullPointerException("Debe existir alguna línea base cerrada en la corriente de RELEASE")
		}
		versionParam.put('version',lastClosedVersion);
	}
	else {
		if (isNull(lastSnapshotVersion)) {
			throw new NullPointerException("Debe existir alguna línea base abierta en la corriente de DESARROLLO")
		}
		versionParam.put('version',lastSnapshotVersion);
	}
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


