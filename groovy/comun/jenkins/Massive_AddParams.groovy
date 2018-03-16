package jenkins;

import hudson.model.*
import java.util.regex.*
import groovy.xml.*
import groovy.util.Node
import java.io.*
import java.nio.charset.Charset

// Este script lee la lista de jobs, decide si debe o no intervenir sobre cada uno de ellos
//  y almacena en su propio workspace de repositorio la configuración modificada, es decir:
//  es inocuo en cuanto a la configuración de jenkins.  El resultado debe recogerse desde el
//  workspace del job e importarse a jenkins por algún medio (p. ej. jenkins-cli)
def resolver = build.buildVariableResolver

// Parámetros de entrada
def jobList = resolver.resolve("jobList")
def parameterName = resolver.resolve("parameterName")
def paramType = resolver.resolve("paramType")
def defaultValue = resolver.resolve("defaultValue")

// Entorno del job
final home = build.getEnvironment().get("JENKINS_HOME")
final workspace = build.workspace

// Esta función escribe un config.xml en la 'mochila' del job de modificación masiva
def writeConfig(text, jobName, workspace){
	println "cambiando ${jobName}..."
	new File("${workspace}/${jobName}").mkdirs()
	def destFile = new File("${workspace}/${jobName}/config.xml")
	destFile.delete()
	//destFile << text
	Writer writer = new OutputStreamWriter(new FileOutputStream(destFile), Charset.forName("UTF-8"))
	writer.write(text, 0, text.length())
	writer.flush()
}

def isNull(String text) {
	return text == null || text.trim().length() == 0;
}

// Implementar en esta función la validación de si debe o no debe intervenir el script
def mustChange(xml) {
	// Siempre lo modifica, tanto si existe como si no
	return true;
}

def notNull(String s) {
	return s != null && s.trim().length() > 0;
}


// Implementar en esta función la acción a realizar sobre el xml
def change(xml, parameterName, defaultValue, paramType) {

	def newBooleanParameterXML = """
<hudson.model.BooleanParameterDefinition>
<name>${parameterName}</name>
<description>Dado de alta mediante modif. masiva</description>
<defaultValue>${defaultValue}</defaultValue>
</hudson.model.BooleanParameterDefinition>
"""

	def newStringParameterXML = """
<hudson.model.StringParameterDefinition>
<name>${parameterName}</name>
<description>Dado de alta mediante modif. masiva</description>
<defaultValue>${defaultValue}</defaultValue>
</hudson.model.StringParameterDefinition>
"""

	def newParameterXML = "";

	if(paramType.equals("boolean")) {
		newParameterXML = newBooleanParameterXML;
	}
	else if(paramType.equals("string")) {
		newParameterXML = newStringParameterXML;
	}


	def newParameterGroupXML = """
<hudson.model.ParametersDefinitionProperty>
<parameterDefinitions>
${newParameterXML}
</parameterDefinitions>
</hudson.model.ParametersDefinitionProperty>
"""
	if (xml.properties != null && xml.properties.size() > 0) {
		// Intentar actualizar el valor
		boolean found = false;
		def parameters = xml.properties['hudson.model.ParametersDefinitionProperty'];
		if (parameters != null && parameters.size() > 0) {
			def parameterDefinitions = parameters.parameterDefinitions;
			if (parameterDefinitions != null && parameterDefinitions.size() > 0) {
				def stringParameters = parameterDefinitions["hudson.model.StringParameterDefinition"]
				def booleanParameters = parameterDefinitions["hudson.model.BooleanParameterDefinition"]
				// Recorrer todos los parámetros de cadena
				stringParameters?.each { def parameterDefinition ->
					if (parameterDefinition.name.text().equals(parameterName) && notNull(defaultValue)) {
						found = true;
						println "Actualizando el valor de $parameterName ..."
						parameterDefinition.defaultValue[0].setValue(defaultValue);
					}
				}
				// Recorrer todos los parámetros de cadena
				booleanParameters?.each { def parameterDefinition ->
					if (parameterDefinition.name.text().equals(parameterName) && notNull(defaultValue)) {
						found = true;
						println "Actualizando el valor de $parameterName ..."
						parameterDefinition.defaultValue[0].setValue(defaultValue);
					}
				}
				// Si no se ha encontrado, añadirlo
				if (!found) {
					def node = new XmlParser().parseText(newParameterXML);
					println "Añadiendo $parameterName ..."
					parameterDefinitions[0].children().add(node);
				}
			}
		}
		else {
			// Job sin parámetros
			// Se lo añadimos directamente
			def node = new XmlParser().parseText(newParameterGroupXML);
			println "Añadiendo $parameterName a job sin parámetros ..."
			xml.properties[0].append(node);
		}
	}
}

def jobs = jobList.split("\n")
jobs.each(){ job ->
	boolean changed = false;
	def configFile = new File("${home}/jobs/${job}/config.xml")
	if (configFile.exists()){
		def xml = new XmlParser().parseText(configFile.getText("UTF-8"));
		if (mustChange(xml)) {
			println "Modificando $job ..."
			change(xml, parameterName, defaultValue, paramType);
			// Guardar el job en el ws local
			writeConfig(XmlUtil.serialize(xml), job, workspace);
		}
	}else{
		println "--> ${configFile} exists: ${configFile.exists()}"
	}
	println "***********************"
}