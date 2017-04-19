// Recopila desde RTC la información de corrientes y áreas de proyecto

import hudson.model.*
import jenkins.model.*
import java.beans.XMLEncoder;

import es.eci.utils.Stopwatch;
import es.eci.utils.TmpDir;
import rtc.*;

// Devuelve un map similar al inicial, pero indexado por los nombres embellecidos
//	de la corriente
def beautify(Map<String, List<String>> areas) {
	Map<String, List<String>> ret = [:]
	areas.keySet().each { String area ->
		ret[ProjectAreasMap.beautify(area)] = 
			areas[area];
	}
	return ret;
}

def build = Thread.currentThread().executable;
def workspace = build.workspace;
def resolver = build.buildVariableResolver;
// Configuración de RTC
def urlRTC = build.getEnvironment(null).get("urlRTC")
def userRTC= build.getEnvironment(null).get("userRTC") 
def pwdRTC = resolver.resolve("pwdRTC") 
def scmToolsHome = build.getEnvironment(null).get("SCMTOOLS_HOME")

TmpDir.tmp { tmpDir ->
	try {
		println "URL RTC: ${urlRTC}"
		println "Usuario RTC: ${userRTC}"
		println "SCM Tools: ${scmToolsHome}"
		// Consulta de las áreas
		ProjectAreasMap p = new ProjectAreasMap(scmToolsHome);
		p.initLogger { println it }
		Map<String, List<String>> areas = p.map(userRTC, pwdRTC, urlRTC, tmpDir);
		println areas
		// Serializarlas en XML		
		// areas.xml -> nombres embellecidos para checking
		XMLEncoder encoder = new XMLEncoder(
			new BufferedOutputStream(
				new FileOutputStream("${build.workspace}/areas.xml")));
		encoder.writeObject(beautify(areas));
		encoder.close();
		// full_areas.xml -> nombres reales para usar en clarive
		XMLEncoder fullEncoder = new XMLEncoder(
			new BufferedOutputStream(
				new FileOutputStream("${build.workspace}/full_areas.xml")));
		fullEncoder.writeObject(areas);
		fullEncoder.close();
	}
	catch (Exception e) {
		println e.getMessage()
		e.printStackTrace();
	}
}