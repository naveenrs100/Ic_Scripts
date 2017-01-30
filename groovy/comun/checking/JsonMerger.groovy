//$JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/checking/JsonMerger.groovy
import hudson.model.*
import groovy.json.*
import java.io.*
import java.text.Normalizer
  
def resolver = build.buildVariableResolver
  
def fileNameList = resolver.resolve("fileNameList")
def outFileName = resolver.resolve("outFileName")

def projectAreas = [:]
projectAreas.projectAreas = [:]

// FUNCTIONS -----------------

def transform(text){
	int index = text.indexOf("-");
	if (index != -1)
		text = text.substring(index + 1);
		
	text = text.replace("(RTC)","")
	text = text.replace(" - RTC","")
	text = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "")
	text = text.replace("(", "-")
	text = text.replace(")", "-")
	return text.trim();
}

//-----------------------
//	INICIO DEL SCRIPT 
//-----------------------

if (fileNameList!=null){
  def fileNames = new JsonSlurper().parseText(fileNameList)
  fileNames.each { fileName ->
	def file = new File("${build.workspace}/${fileName.name}");
	def projectAreasTmp = new JsonSlurper().parseText(file.getText(fileName.charset))
	
	if (projectAreasTmp.projectAreas!=null){
	  println "adding ${file} ..."
	  projectAreasTmp.projectAreas.each { key, projectArea ->
		if (fileName.transform)
		  projectArea.name = transform(projectArea.name)
        def projectAreaTmp = projectAreasTmp.projectAreas.get(projectArea.name)
        if (projectAreaTmp==null)
          projectAreaTmp = projectArea
        else
          projectAreaTmp.users.putAll(projectArea.users)          
        
		projectAreas.projectAreas.put(projectArea.name,projectAreaTmp);
	  }
	}else{
	  println "WARNING: El fichero ${file} no tiene projectAreas definido"
	}
  }
  def jsonFile = new File ("${build.workspace}/${outFileName}")
  jsonFile.write(new JsonOutput().toJson(projectAreas),"UTF-8")
}else{
  println "WARNING: la lista de ficheros está vacia"
}