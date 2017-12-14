package portal

//$JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/portal/Portalizador.groovy
import hudson.model.*
import java.util.regex.*
import groovy.xml.*
import groovy.util.Node

def jobList = build.buildVariableResolver.resolve("jobList")
home = build.getEnvironment(null).get("JENKINS_HOME")
workspace = build.workspace

// --------- GLOBAL ------------

streamPublisher= '<publishers><hudson.plugins.parameterizedtrigger.BuildTrigger plugin="parameterized-trigger@2.25">'+
	  '<configs><hudson.plugins.parameterizedtrigger.BuildTriggerConfig><configs>'+
	  '<hudson.plugins.parameterizedtrigger.CurrentBuildParameters/>'+
	  '<hudson.plugins.parameterizedtrigger.PredefinedBuildParameters><properties>'+
	  'jobInvokerType=streams\nparentWorkspace=${WORKSPACE}</properties>'+
	  '</hudson.plugins.parameterizedtrigger.PredefinedBuildParameters></configs><projects>stepNotifierPortal</projects>'+
	  '<condition>ALWAYS</condition><triggerWithNoParameters>false</triggerWithNoParameters>'+
	  '</hudson.plugins.parameterizedtrigger.BuildTriggerConfig></configs>'+
	  '</hudson.plugins.parameterizedtrigger.BuildTrigger></publishers>'
stepPublisher='<publishers><hudson.plugins.parameterizedtrigger.BuildTrigger plugin="parameterized-trigger@2.25">'+
	  '<configs><hudson.plugins.parameterizedtrigger.BuildTriggerConfig><configs>'+
	  '<hudson.plugins.parameterizedtrigger.CurrentBuildParameters/>'+
	  '<hudson.plugins.parameterizedtrigger.PredefinedBuildParameters><properties>jobInvokerType=steps\nNODE=</properties>'+
	  '</hudson.plugins.parameterizedtrigger.PredefinedBuildParameters></configs><projects>stepNotifierPortal,</projects>'+
	  '<condition>ALWAYS</condition><triggerWithNoParameters>false</triggerWithNoParameters>'+
	  '</hudson.plugins.parameterizedtrigger.BuildTriggerConfig></configs>'+
	  '</hudson.plugins.parameterizedtrigger.BuildTrigger></publishers>'
parameter = '<properties><hudson.model.ParametersDefinitionProperty><parameterDefinitions>'+
	  '<hudson.model.StringParameterDefinition><name>executionUuid</name><description></description><defaultValue>'+
	  '</defaultValue></hudson.model.StringParameterDefinition></parameterDefinitions>'+
	  '</hudson.model.ParametersDefinitionProperty></properties>'
parameterAlone = '<hudson.model.StringParameterDefinition><name>executionUuid</name><description>'+
	'</description><defaultValue></defaultValue></hudson.model.StringParameterDefinition>'
buildersToAdd = '''
	<org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder plugin="conditional-buildstep@1.3.3">
    <condition class="org.jenkins_ci.plugins.run_condition.contributed.ShellCondition" plugin="run-condition@1.0">
    <command>
if [ &quot;${jobs}&quot; = &quot;&quot; ] || [ &quot;${BUILD_RESULT}&quot; = &quot;NOT_BUILT&quot; ];
then 
exit -1 
fi</command>
    </condition>
    <buildStep class="hudson.plugins.parameterizedtrigger.TriggerBuilder" plugin="parameterized-trigger@2.25">
    <configs>
    <hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig>
    <configs>
    <hudson.plugins.parameterizedtrigger.CurrentBuildParameters/>
    <hudson.plugins.parameterizedtrigger.PredefinedBuildParameters>
    <properties>jobInvokerType=streams
create=true
parentWorkspace=${WORKSPACE}</properties>
    </hudson.plugins.parameterizedtrigger.PredefinedBuildParameters>
    </configs>
    <projects>stepNotifierPortal</projects>
    <condition>ALWAYS</condition>
    <triggerWithNoParameters>false</triggerWithNoParameters>
    <block/>
    <buildAllNodesWithLabel>false</buildAllNodesWithLabel>
    </hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig>
    </configs>
    </buildStep>
    <runner class="org.jenkins_ci.plugins.run_condition.BuildStepRunner$Fail" plugin="run-condition@1.0"/>
    </org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder>
'''

//------- FUNCIONES ------------

// Devuelve true si el patrón se encuentra
def publisherContainsPattern(String text,Pattern patron) {
  boolean ret = false;
  Matcher match = text =~ patron
  resultado = match.size() != 0
  if (match.size() >0){
	  ret = true
  }
  return ret;
}

def updateConfig(type, configFile, jobName){
  String text = configFile.getText('UTF-8')
	  println "************* JOB: " + jobName
	if (type == "streams"){
	  def index = -1
	  if (!publisherContainsPattern(text,Pattern.compile(/<builders>[\s\S]*stepNotifierPortal[\s\S]*<\/builders>/))) {
		println "ENTRO a cambiar builders"
		int posicion = 0
		Node xmldoc = new XmlParser().parse(configFile);
		  if (xmldoc.builders.size() > 0){
			  xmldoc.builders[0].children().each { buildstep ->
			posicion ++
			buildstep.each{configs ->
			  if (configs.toString().contains('projects[attributes={}; value=[Trigger]]')){
				  index = posicion
			  }
			}
		  }
		}
		
		if (index != -1){
		  // Añadimos nodo en la posicion determinada
		  Node fragmentToAdd = new XmlParser().parseText( buildersToAdd )
		  index = index - 1
		  xmldoc.builders[0].children().add(index,fragmentToAdd)
		  
		  text = XmlUtil.serialize(xmldoc)
		  writeConfig(text, configFile, jobName)
		}else {
			 println "No se encuentran builders -> no se actualiza"
		}
		
	  }
  }
  // Si no tenía stepNotifierPortal entre los publishers
  if (!publisherContainsPattern(text,Pattern.compile(/<publishers>[\s\S]*stepNotifierPortal[\s\S]*<\/publishers>/))) {
	if (type == "components"){
			println "ENTRO a cambiar publishers"
			text = text.replace("stepNotifierMail","stepNotifierMail,stepNotifierPortal")
			text = text.replace("parentWorkspace=","jobInvokerType=components\nparentWorkspace=")
			 text = addParameter(text)
			writeConfig(text, configFile, jobName)
	  }else if (text.indexOf("<publishers/>")!=-1){
		println "ENTRO a cambiar publishers"
		if (type == "steps"){
			text = text.replace("<publishers/>",stepPublisher)
		}else if (type == "streams"){
			text = text.replace("<publishers/>",streamPublisher)
			text = addParameter(text)
		}
		
		writeConfig(text, configFile, jobName)
	  }
	}
}

def addParameter(text){
  if (text.indexOf("<properties/>")!=-1){
	text = text.replace("<properties/>","<properties>\n${parameter}\n</properties>");
  }else{
	  text = text.replace("</parameterDefinitions>","${parameterAlone}\n</parameterDefinitions>");
  }
  return text
}

def writeConfig(text, file, jobName){
  println "cambiando ${file}..."
  new File("${workspace}/${jobName}").mkdirs()
  def destFile = new File("${workspace}/${jobName}/config.xml")
  destFile.delete()
  destFile << text
}

//------- EJECUCION ------------

def jobs = jobList.split("\n")

jobs.each(){ pair ->
	def job = pair.split(",")
	 def configFile = new File("${home}/jobs/${job[1]}/config.xml")
  if (configFile.exists()){
	updateConfig(job[0],configFile, job[1])
  }else{
	  println "--> ${configFile} exists: ${configFile.exists()}"
  }
  println "***********************"
}