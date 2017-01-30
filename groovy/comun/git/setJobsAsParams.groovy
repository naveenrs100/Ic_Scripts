import java.util.List;
import git.commands.GitCloneCommand
import git.commands.GitLogCommand
import groovy.io.*
import groovy.json.JsonSlurper
import hudson.model.*
import es.eci.utils.ParamsHelper
import es.eci.utils.TmpDir;
import groovy.json.*;

println("Comienza el setJobsAsParams:");

def build = Thread.currentThread().executable
def resolver = build.buildVariableResolver

def gitGroup = resolver.resolve("gitGroup");
def branch = resolver.resolve("branch")
def gitHost = build.getEnvironment(null).get("gitHost");

def parentWorkspace = "${build.workspace}";
def getOrdered = resolver.resolve("getOrdered");
File jenkinsComponentsJobsFile = new File("${parentWorkspace}/jenkinsComponentsJobs.txt");

def components = []
if(jenkinsComponentsJobsFile.exists() && !jenkinsComponentsJobsFile.text.trim().equals("")) {

}
else if(jenkinsComponentsJobsFile.exists() && jenkinsComponentsJobsFile.text.trim().equals("")) {
	println("NO HAY COMPONENTES NUEVOS QUE CONSTRUIR. TODOS ACTUALIZADOS.")
	build.setResult(Result.NOT_BUILT);
} else if(!jenkinsComponentsJobsFile.exists()) {
	throw new Exception("El archivo \"jenkinsComponents.txt\" no existe en el directorio ${parentWorkspace}")
}

def artifactsJsonFile = new File("${parentWorkspace}/artifacts.json");

def paramsMap = [:];

def componentsUrban = getComponentsUrban(jenkinsComponentsJobsFile.text);
println("Jobs a añadir como parámetros -> ${jenkinsComponentsJobsFile.text}")
paramsMap.put('jobs', jenkinsComponentsJobsFile.text);
paramsMap.put('homeStream', "${build.workspace}");
paramsMap.put('componentsUrban', componentsUrban);
if(artifactsJsonFile.exists()) {
	paramsMap.put('artifactsJson',"${artifactsJsonFile.text}");
}
ParamsHelper.addParams(build, paramsMap);


/********************************/

/**
 * Quita la coma final a un texto.
 * @param text
 * @return String sin coma final.
 */
public String removeLastComma(String text) {
	def result;
	if(text.endsWith(",")) {
		result = text.substring(0, text.length() - 1);
	} else {
		result = text;
	}
	return result;
}

/**
 * Devuelve un String con los componentesUrbancode de cada job (si este está definido)
 * @param jobs
 * @return String componentsUrban
 */
def getComponentsUrban(jobs) {
	def jobsObject = new JsonSlurper().parseText(jobs)
	def componentsUrban = "";
	jobsObject.each { List<String> thisJobList ->
		thisJobList.each { String thisJob ->
			def component = thisJob.split("-COMP- ")[1];
			def job = hudson.model.Hudson.instance.getJob(thisJob);
			def componentUrban;
			def jobParameters = getJobParameters(thisJob);
			if(jobParameters.contains("componenteUrbanCode")) {
				if(job != null) {
					job.getProperties().values().each { value ->
						println("value ->" + value)
						if(value instanceof hudson.model.ParametersDefinitionProperty) {
							def paramValue = value.getParameterDefinition("componenteUrbanCode").getDefaultParameterValue().getValue();
							if(!paramValue.trim().equals("")) {
								componentUrban = "${component}:" + paramValue
							} else {
								componentUrban = "${component}:NULL";
							}
						}
					}
				}
			} else {
				componentUrban = "${component}:NULL";
			}
			componentsUrban = componentsUrban + componentUrban +","
		}
	}
	return removeLastComma(componentsUrban);
}

/**
 * Devuelve los parámetros de un job formado como "${gitGroup} -COMP- ${component}";
 * @param gitGroup
 * @param component
 * @return
 */
def getJobParameters(jobName) {
	println("Comprobando si el job \"${jobName}\" en Jenkins tiene el parámetro \"componenteUrbanCode\"");
	def job = hudson.model.Hudson.instance.getJob(jobName);
	def jobParameters = [];
	if(job != null) {
		job.getProperties().values().each { value ->
			if(value instanceof hudson.model.ParametersDefinitionProperty) {
				jobParameters = value.getParameterDefinitionNames();
				println("JobParameters: ${jobParameters}");
			}
		}
	} else {
		println("El job ${jobName} no existe en Jenkins");
	}

	return jobParameters;
}







