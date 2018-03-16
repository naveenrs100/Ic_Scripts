package jenkins;

import es.eci.utils.StringUtil;
import hudson.model.*;


def stream = build.buildVariableResolver.resolve("stream");
def checkLastBuild = build.getEnvironment(null).get("checkLastBuild");

def buildJobName = "${stream} - build"

// Si la última build está en estado FAILURE no se puede hacer release.

if(StringUtil.notNull(checkLastBuild) && checkLastBuild.contentEquals("true")) {

	def buildJob = Hudson.instance.getJob("${buildJobName}");

	def lastSuccess = buildJob.getLastSuccessfulBuild().toString();
	def lastFailure = buildJob.getLastFailedBuild().toString();
	def lastUnstalble = buildJob.getLastUnstableBuild().toString();

	int succNumber
	if(lastSuccess != null) {
		succNumber = lastSuccess.split("#")[1].toInteger()
	} else {
		throw new Exception(" ## ERROR: Por favor, haz una build correcta antes de intentar una release.");
	}

	int failNumber
	if(lastFailure != null) {
		failNumber = lastFailure.split("#")[1].toInteger()
		if(succNumber < failNumber) {
			throw new Exception(" ## ERROR: Por favor, haz una build correcta antes de intentar una release.");
		}
		else {
			println("##INFO: Build previa generada correctamente. Seguir adelante...")
		}
	}
}
else {
	println("##WARNING: No se está comprobando si se ha hecho una build correcta antes.");
}