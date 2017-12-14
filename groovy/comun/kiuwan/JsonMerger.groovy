package kiuwan

/**
 * Se invoca como Groovy Script.
 * 
 * Este script recoge la información de usuarios y permisos de 
 * rtc (rtc.json) y de teamsuite (teamsuite.json), y las mezcla 
 * en un único fichero para pasar a kiuwan.com en la 
 * actualización periódica de usuarios.
 * 
 * Parámetros de entrada:
 * fileNameList: JSON con varias entradas, en cada una de ellas
 * 		+ name: nombre del fichero
 * 		+ charset: nombre del encoding a utilizar para leerlo
 * outFileName: nombre del fichero una vez mezclado
 * parentWorskpace: directorio de ejecución
 * 
 */
import es.eci.utils.ParameterValidator
import es.eci.utils.StringUtil
import es.eci.utils.SystemPropertyBuilder
import groovy.json.*

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

def params = propertyBuilder.getSystemParameters()
String fileNameList = params.get("fileNameList");
String outFileName = params.get("outFileName");

ParameterValidator.builder().
	add("fileNameList", fileNameList).
	add("outFileName", fileNameList).
	build().validate();
	
File parentWorkspace = new File(params.get("parentWorkspace"))


def projectAreas = [:]
projectAreas.projectAreas = [:]

//-----------------------
//	INICIO DEL SCRIPT 
//-----------------------

if (fileNameList!=null){
  def fileNames = new JsonSlurper().parseText(fileNameList)
  fileNames.each { fileName ->
	def file = new File(parentWorkspace, fileName.name);
	def projectAreasTmp = new JsonSlurper().parseText(file.getText(fileName.charset))
	
	if (projectAreasTmp.projectAreas!=null){
	  println "adding ${file} ..."
	  projectAreasTmp.projectAreas.each { key, projectArea ->
		projectArea.name = StringUtil.normalizeProjectArea(projectArea.name)
        def projectAreaTmp = projectAreasTmp.projectAreas.get(projectArea.name)
        if (projectAreaTmp==null) {
          projectAreaTmp = projectArea
        }
        else {
          projectAreaTmp.users.putAll(projectArea.users)
        }
		projectAreas.projectAreas.put(projectArea.name,projectAreaTmp);
	  }
	}else{
	  println "WARNING: El fichero ${file} no tiene projectAreas definido"
	}
  }
  def jsonFile = new File (parentWorkspace, outFileName)
  jsonFile.write(new JsonOutput().toJson(projectAreas),"UTF-8")
}
else{
  println "WARNING: la lista de ficheros está vacia"
}