//$JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/jenkins/config/getStreams.groovy
import extract.excel.ExcelBuilder

def jenkinsHome	= args[0]
def adminFile = args[1]
def workspace = args[2]

def config = "${jenkinsHome}/jobs/JenkinsConfiguration/workspace/${adminFile}"
def outfile = new File("${workspace}/streams.txt")
outfile.delete()
new ExcelBuilder(config).eachLine([labels:true,sheet:"STREAM"]) {
	if (activo!=null && activo=="true")
		outfile << "${stream}\n"
}