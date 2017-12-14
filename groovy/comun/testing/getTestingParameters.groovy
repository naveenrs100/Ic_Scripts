package testing

import es.eci.utils.ParamsHelper;
import es.eci.utils.StringUtil;
import hudson.model.ParametersAction
import java.text.SimpleDateFormat;

// get parameters
def parameters = build?.actions.find{ it instanceof ParametersAction }?.parameters
parameters.each {
   println "parameter ${it.name}:"
   println it.value
}

ParamsHelper pHelper = new ParamsHelper();
def testParams = "";
String application = "";
String instantanea = "";
String clariveId = "";
String dockerVolume = "";

Date ahora = new Date()
SimpleDateFormat formato = new SimpleDateFormat("yyyyMMddHHmmss");
println "Timestamp generado: " + formato.format(ahora)

parameters.each { parameter ->
	if(parameter.name != "aplicacion" && parameter.name != "instantanea"
		&& parameter.name != "gitGroup" && parameter.name != "executionUuid") {
		
		// Si no viene clariveId hay que forzarlo con el que generamos porque los equipos lo utilizan
		if (parameter.name == "clariveId" && parameter.value.size() == 0) {
			testParams = testParams + "-e ${parameter.name}=Q_" + formato.format(ahora) + " "
		} else {
			testParams = testParams + "-e ${parameter.name}=${parameter.value} ";
		}
	}
		
	// Se construyen por separado porque se desconoce el órden en el que vienen los parámetros
		
	if (parameter.name == "aplicacion")
		application = parameter.value
	if (parameter.name == "instantanea")
		instantanea = parameter.value
	if (parameter.name == "clariveId") {
		if (parameter.value.size() > 0)
			clariveId = parameter.value
		else
			clariveId = "Q_" + formato.format(ahora)
	}
		
}

dockerVolume = application + "/" + instantanea + "/" + clariveId
def theDockerVolume = ["volumePath":"${dockerVolume.trim()}"]
def theParams = ["testParams":"${testParams.trim()}"]

println "theDockerVolume: " + theDockerVolume

pHelper.addParams(build, theParams);
pHelper.addParams(build, theDockerVolume);
