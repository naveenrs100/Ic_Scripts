package portal.interfase

import org.apache.http.entity.StringEntity

import es.eci.utils.ParameterValidator
import es.eci.utils.QuveHelper
import es.eci.utils.Stopwatch
import es.eci.utils.SystemPropertyBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import org.apache.http.entity.ContentType

@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')
@Grab(group='org.apache.httpcomponents', module='httpmime', version='4.4')

/**
 * Este script alimenta QUVE con un fichero de permisos generado desde el SCM.
 * Parámetros
 * 
 * permissionsFile - Ruta del fichero JSON con la información
 * jenkinsHome - Directorio raíz de Jenkins, donde se encuentra el portalSessionKey
 * quveURL - URL de servicios de QUVE
 * 
 * 
 */

// Lectura de parámetros de entrada
SystemPropertyBuilder b = new  SystemPropertyBuilder();
b.initLogger { println it }

def params = b.getSystemParameters()
String jenkinsHome = params.get("jenkinsHome");
String quveURL = params.get("quveURL");
String permissionsFile = params.get("permissionsFile");

ParameterValidator.builder().
	add("quveURL", quveURL).
	add("permissionsFile", permissionsFile,
		{it != null && new File(it.toString()).exists() &&
			new File(it.toString()).isFile()
		}).
	add("jenkinsHome", jenkinsHome,
		{it != null && new File(it.toString()).exists() &&
			new File(it.toString()).isDirectory()
		}).build().validate();

// Acceso a servicios de QUVE
QuveHelper helper = new QuveHelper(jenkinsHome, quveURL);
helper.initLogger { println it }

String ret = null;
long millis = Stopwatch.watch {
	def entity = new StringEntity(new File(permissionsFile).text, ContentType.APPLICATION_JSON);
	ret = helper.sendQuve(quveURL + "/", "permissions/update", entity, "application/json")	
}
println "Actualización completa -> $millis msec."

if (ret == null) {
	println "No ha dado retorno, ha habido algún problema"
	println "Consultar el log de QUVE"
}
else {
	println JsonOutput.prettyPrint(ret)
	def result = new JsonSlurper().parseText(ret);
	// Mostrar un resumen
	println "=================================================================="
	println "RESUMEN DE LA ACTUALIZACIÓN"
	println "Total de áreas de proyecto resultante: ${result.projectAreasTotal}"
	if (result.changedProjectAreas?.keySet().size() > 0) {
		println "Cambios de nombre de área de proyecto:"
		for  (String key: result.changedProjectAreas.keySet()) {
			println "\t${key} -> ${result.changedProjectAreas[key]}"
		}
	}
	if (result.addedProjectAreas?.size() > 0 ) {
		println "Áreas de proyecto añadidas:"
		result.addedProjectAreas.each{ String pa ->
			println "\t${pa}"
		}
	}
	if (result.deletedProjectAreas?.size() > 0 ) {
		println "Áreas de proyecto eliminadas:"
		result.deletedProjectAreas.each{ String pa ->
			println "\t${pa}"
		}
	}
	if (result.restoredProjectAreas?.size() > 0 ) {
		println "Áreas de proyecto restauradas:"
		result.restoredProjectAreas.each{ String pa ->
			println "\t${pa}"
		}
	}
	if (result.newUsers?.size() > 0 ) {
		println "Usuarios añadidos:"
		result.newUsers.each{ String u ->
			println "\t${u}"
		}
	}
	if (result.disabledUsers?.size() > 0 ) {
		println "Usuarios desactivados:"
		result.disabledUsers.each{ String u ->
			println "\t${u}"
		}
	}
	if (result.usersAddedToProjectArea?.keySet().size() > 0 
			|| result.usersDeletedFromProjectArea?.keySet().size() > 0) {
		println "Cambios de permisos por área de proyecto"
		if (result.usersAddedToProjectArea?.keySet().size() > 0) {
			println "Accesos AÑADIDOS por área de proyecto"
			for (String pa: result.usersAddedToProjectArea.keySet()) {
				println "\t${pa}:"
				List<String> users = result.usersAddedToProjectArea[pa]
				for (String user: users) {
					println "\t\t${user}"
				}
			}
		}
		if (result.usersDeletedFromProjectArea?.keySet().size() > 0) {
			println "Accesos RETIRADOS por área de proyecto"
			for (String pa: result.usersDeletedFromProjectArea.keySet()) {
				println "\t${pa}:"
				List<String> users = result.usersDeletedFromProjectArea[pa]
				for (String user: users) {
					println "\t\t${user}"
				}
			}
		}
	}
	println "=================================================================="
}
