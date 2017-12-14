package clarive

import hudson.model.*;
import groovy.json.*;
import es.eci.utils.ParamsHelper;
import com.cloudbees.plugins.flow.FlowCause;
import es.eci.utils.ParamsHelper;

def given_result = build.buildVariableResolver.resolve("resultado");

def causa = build.getCause(Cause.UpstreamCause);
if(causa == null) {
	causa = build.getCause(FlowCause);
}

def jobName = causa.getUpstreamProject();
def buildNumber = causa.getUpstreamBuild().toInteger();

def resultado = causa.getUpstreamRun().getResult();


if(resultado == null || !resultado.toString().trim().equals("SUCCESS")) {
	resultado = "INCORRECTO";
} else if(resultado.toString().trim().equals("SUCCESS")) {
	resultado = "CORRECTO";
}

if(given_result != null && !given_result.trim().equals("")) {
	 println("El resultado del job padre ya viene indicado: ${given_result}");
} else {
	def params = [:];
	params.put("resultado", "${resultado}");
	ParamsHelper.addParams(build, params);
}





