import hudson.model.*;
import es.eci.utils.JobRootFinder;

// Se establecen los parámetros que determinan si hay que llamara
// al build de corriente una vez terminada una release.

def build = Thread.currentThread().executable;
def resolver = build.buildVariableResolver;

def setParams(build, params) {
		def paramsIn = build?.actions.find{ it instanceof ParametersAction }?.parameters
		def index = build?.actions.findIndexOf{ it instanceof ParametersAction }
		def paramsTmp = []
		if (paramsIn!=null) {
			//No se borra nada para compatibilidad hacia atrás.
			paramsTmp.addAll(paramsIn)
			//Borra de la lista los paramaterAction
			build?.actions.remove(index)
		}
		paramsTmp.addAll(params)

		build?.actions.add(new ParametersAction(paramsTmp))
	}

JobRootFinder jRootFinder = new JobRootFinder(build);
def rootResult = jRootFinder.getRootBuild(build).getResult();
println("Resultado del padre: " + rootResult);

// Si la release ha terminado exitosamente se establecen los parámetros "stream" y
// "rootResult" que serán utilizados por el comparador que 
// job de corriente de build.
if(rootResult == Result.SUCCESS) {
  def gitGroup = resolver.resolve("gitGroup");
  def stream = resolver.resolve("stream");
  def params = [];
  params.add(new StringParameterValue("rootResult","SUCCESS"));
  if(gitGroup != null && !gitGroup.trim().equals("")) {        
    params.add(new StringParameterValue("stream","${gitGroup}"));    
  } 
  setParams(build,params);  
  
} else {
  def params = [];
  params.add(new StringParameterValue("rootResult","FAILURE"));
  setParams(build,params);  
}
