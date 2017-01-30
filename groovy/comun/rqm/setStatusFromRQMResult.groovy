import hudson.model.*
import jenkins.model.*

def build = Thread.currentThread().executable

def rqmReponseTxt = new File("${build.workspace}/resultRQM.xml").text.replaceAll("ns5:","")
rqmReponseTxt = "<?xml version='1.0' encoding='UTF-8'?><result>${rqmReponseTxt}</result>"
def rqmReponse = new XmlSlurper().parseText(rqmReponseTxt)

def dateUpdate = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'","${rqmReponse.updated}")
println "rqmReponse.updated: ${dateUpdate.format('dd/MM/yyyy HH:mm:ss')}"
println "rqmReponse.state: ${rqmReponse.state}"

switch ( rqmReponse.state ) {
	case "com.ibm.rqm.execution.common.state.passed":
		build.setResult(Result.SUCCESS)
		break
	case "com.ibm.rqm.execution.common.state.failed":
	case "com.ibm.rqm.execution.common.state.error":
	case "com.ibm.rqm.execution.common.state.perm_failed":
		build.setResult(Result.FAILURE)
		break
	case "com.ibm.rqm.execution.common.state.blocked":
	case "com.ibm.rqm.execution.common.state.incomplete":
	case "com.ibm.rqm.execution.common.state.deferred":
	case "com.ibm.rqm.execution.common.state.paused":
	case "com.ibm.rqm.execution.common.state.inprogress":
	case "com.ibm.rqm.execution.common.state.part_blocked":
	case "com.ibm.rqm.execution.common.state.inconclusive":
		build.setResult(Result.UNSTABLE)
		break
	default:
		build.setResult(Result.NOT_BUILT)
		break
}