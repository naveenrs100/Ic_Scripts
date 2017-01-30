// Recopila desde RTC la información de corrientes y áreas de proyecto

import hudson.model.*
import jenkins.model.*
import java.beans.XMLEncoder;

import es.eci.utils.Stopwatch;
import es.eci.utils.TmpDir;
import rtc.*;


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
		XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream("${build.workspace}/areas.xml")));
		encoder.writeObject(areas);
		encoder.close();
	}
	catch (Exception e) {
		println e.getMessage()
		e.printStackTrace();
	}
}