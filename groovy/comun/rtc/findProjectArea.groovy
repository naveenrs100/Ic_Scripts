package rtc

// A partir de una stream, intenta obtener la project area correspondiente a partir 
//	del fichero xml cacheado en el job de refresco periódico.

import java.beans.XMLDecoder;

import es.eci.utils.ParamsHelper
import hudson.model.*
import jenkins.model.*
import rtc.ProjectAreaCacheReader

def stream = build.buildVariableResolver.resolve("stream");
def jenkinsHome	= build.getEnvironment(null).get("JENKINS_HOME");


ProjectAreaCacheReader reader = new ProjectAreaCacheReader(
	new FileInputStream(jenkinsHome + "/workspace/CacheStreamToProjectAreas/areas.xml"));
String ret = reader.getProjectArea(stream);

if (ret != null) {
	ParamsHelper.addParams(build, ["projectArea":ret]);
}