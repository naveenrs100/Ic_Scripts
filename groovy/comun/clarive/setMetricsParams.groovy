package clarive

import hudson.model.*;
import groovy.json.*;
import es.eci.utils.ParamsHelper;
import com.cloudbees.plugins.flow.FlowCause;

def causa = build.getCause(Cause.UpstreamCause);
if(causa == null) {
	causa = build.getCause(FlowCause);
}

def jobName = causa.getUpstreamProject();
def buildNumber = causa.getUpstreamBuild().toInteger();

println("Sacamos métrica para el job \"${jobName}\" y build \"${buildNumber}\" si procede...");

def job = hudson.model.Hudson.instance.getJob(jobName);

// Sólo sacamos métrica si se trata de un job de componente.
if(jobName.toString().contains("-COMP-")) {
	println("... sí procede sacar métricas al ser el job padre de componente.");
	if(job != null) {
		def buildInvoker = job.getBuildByNumber(buildNumber);
		def actions = buildInvoker.getActions();
		def testMap = [:];
		def jacocoMap = [:];
		def coberturaMap = [:];
		actions.each { action ->
			def claseAction = action.getClass();
			def nombreClaseAction = claseAction.getName();
			if(nombreClaseAction == 'hudson.tasks.junit.TestResultAction') {
				testMap.put('junittotal',action.getTotalCount());
				testMap.put('junitfailed',action.getFailCount());
				testMap.put('junitskiped',action.getSkipCount());
				testMap.put('junithealthScaleFactor',action.getHealthScaleFactor());
				
				// Borrar siguiente entradas si no se desea un informe tan detallado.
//				def resultObject = action.getResult();
//				testMap.put('junitDuration',resultObject.getDuration());
//				testMap.put('junitPassCount',resultObject.getPassCount());
//
//				def suites = resultObject.getSuites();
//				def jsonSuites = [];
//
//				suites.each { suite ->					
//					def jsonSuite = [:];
//					jsonSuite.put('duration', suite.getDuration());
//					jsonSuite.put('stdErr', suite.getStderr());
//					jsonSuite.put('stdOut', suite.getStdout());
//					jsonSuite.put('timestamp', suite.getTimestamp());
//					jsonSuite.put('name', suite.getName());
//					def cases = suite.getCases();
//					def jsonCases = [];
//					cases.each { thisCase ->
//						def jsonCase = [:];
//						jsonCase.put('age', thisCase.getAge());
//						jsonCase.put('className', thisCase.getClassName());
//						jsonCase.put('duration', thisCase.getDuration());
//						jsonCase.put('errorDetails', thisCase.getErrorDetails());
//						jsonCase.put('errorStackTrace', thisCase.getErrorStackTrace());
//						jsonCase.put('failedSince', thisCase.getFailedSince());
//						jsonCase.put('name', thisCase.getName());
//						jsonCase.put('skipped', thisCase.isSkipped());
//						jsonCase.put('skippedMessage', thisCase.getSkippedMessage());
//						jsonCase.put('status', thisCase.getStatus());
//						jsonCase.put('stdErr', thisCase.getStderr());
//						jsonCase.put('stdOut', thisCase.getStdout());
//						jsonCases.add(jsonCase);
//					}
//					jsonSuite.put('cases',jsonCases);
//					jsonSuites.add(jsonSuite);
//				}
//				testMap.put('suites',jsonSuites);

			} else if(nombreClaseAction == 'hudson.plugins.jacoco.JacocoBuildAction') {
				def jacocoResult = action.getResult();
				println("JacocoResult:");
				println(jacocoResult);
				jacocoMap.put('branchCoverage', jacocoResult.branch.percentage);
				jacocoMap.put('complexityScore', jacocoResult.complexity.percentage);
				jacocoMap.put('instructionCoverage', jacocoResult.instruction.percentage);
				jacocoMap.put('methodCoverage', jacocoResult.method.percentage);
				jacocoMap.put('lineCoverage', jacocoResult.line.percentage);
				jacocoMap.put('lineTotal', jacocoResult.line.total);
				jacocoMap.put('classCoverage', jacocoResult.clazz.percentage);

			} else if(nombreClaseAction == 'hudson.plugins.cobertura.CoberturaBuildAction') {
				// TODO: Sacar los datos de Cobertura (Sólo lo usa Omnistore)
			}
		}

		def jsonTest = JsonOutput.toJson(testMap);
		def jsonJacoco = JsonOutput.toJson(jacocoMap);

		def params = [:];
		params.put("metrica_PU","${jsonTest}");
		params.put("metrica_PC","${jsonJacoco}");

		ParamsHelper.addParams(build,params);

	}

} else {
	println("El job padre es de corriente. No mostramos métricas.");
}
