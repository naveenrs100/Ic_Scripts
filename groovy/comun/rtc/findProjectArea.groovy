// A partir de una stream, intenta obtener la project area correspondiente a partir 
//	del fichero xml cacheado en el job de refresco periódico.

import hudson.model.*
import jenkins.model.*
import java.beans.XMLDecoder;

def build = Thread.currentThread().executable;
def resolver = build.buildVariableResolver;
def stream = resolver.resolve("stream");
def jenkinsHome	= build.getEnvironment(null).get("JENKINS_HOME");

XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(jenkinsHome + "/workspace/UpdateProjectAreas/areas.xml")));
Map<String, List<String>> map = (Map<String, List<String>>) decoder.readObject();
decoder.close();

// Recorrer el mapa, indexado por área de proyecto
boolean encontrado = false;
Iterator<String> it = map.keySet().iterator();

String ret = null;

while (ret == null && it.hasNext()) {
	String area = it.next();
	List<String> streams = map.get(area);
	if (streams.contains(stream)) {
		ret = area;
	}
}

if (ret != null) {
	def paramsIn = build?.actions.find{ it instanceof ParametersAction }?.parameters
	def index = build?.actions.findIndexOf{ it instanceof ParametersAction }
	def params = [];
	if (paramsIn!=null){
		//No se borra nada para compatibilidad hacia atrás.
		params.addAll(paramsIn);
		//Borra de la lista los paramaterAction
		build?.actions.remove(index);
	}
	params.add(new StringParameterValue("projectArea", ret));
	build?.actions.add(new ParametersAction(params));
}