import java.io.File;

import rtc.commands.RTCReplaceCommand;
import es.eci.utils.NexusHelper
import es.eci.utils.SystemPropertyBuilder;
import es.eci.utils.ZipHelper
import es.eci.utils.pom.MavenCoordinates
import es.eci.utils.StringUtil;
import es.eci.utils.TmpDir;
import groovy.json.JsonSlurper

SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
def params = parameterBuilder.getSystemParameters();


def instantanea = params["instantanea"];
//def instantanea = "test_201709071027"; 

def aplicacionUrbanCode = params["aplicacionUrbanCode"];
//def aplicacionUrbanCode = "AppUrban_Complete";

def nexusUrl = params["nexusUrl"]
//def nexusUrl = "http://nexus.elcorteingles.pre/content/groups/public";

def fichasGroupId = params["fichasGroupId"];
//def fichasGroupId = "es.eci.fichas_urbancode";

def parentWorkspace = params["parentWorkspace"];
//def parentWorkspace = new File("C:/Users/dcastro.jimenez/Desktop/Herramientas ECI/tmp");

def scmToolsHome = params["scmToolsHome"];
//def scmToolsHome = "C:/OpenDevECI/scmtools/eclipse";

def userRTC = params["userRTC"];
//def userRTC = "JENKINS_RTC";

def pwdRTC = params["pwdRTC"];
//def pwdRTC = "12345678";

def urlRTC = params["urlRTC"];
//def urlRTC = "https://rtc.elcorteingles.pre:59443/ccm";


NexusHelper nxHelper = new NexusHelper(nexusUrl);
MavenCoordinates coordinates = new MavenCoordinates(fichasGroupId, StringUtil.normalize(aplicacionUrbanCode), instantanea);
coordinates.setPackaging("zip");

TmpDir.tmp { File tmpDir ->
	File descriptorFile = nxHelper.download(coordinates, tmpDir);
	ZipHelper zipHelper = new ZipHelper();
	zipHelper.unzipFile(descriptorFile, tmpDir);
	
	File rtcJsonFile = new File(tmpDir,"rtc.json");
	
	def rtcJsonObject = (new JsonSlurper()).parseText(rtcJsonFile.text);
	
	String originStream = rtcJsonObject.source
	
	String[] auxSplit = originStream.split(" - ");
	String type = auxSplit[auxSplit.length - 1];
	
	String produccionStream = originStream.replace(type, "PRODUCCION");
	
	rtcJsonObject.versions.each { versionObject ->
		versionObject.keySet().each { String key ->
			String componentName = key;
			String version = versionObject.get(key);
			
			println("Promocionando el componente \"${componentName}\"(${version}) a la stream \"${produccionStream}\" ...")
			
			RTCReplaceCommand rc = new RTCReplaceCommand();
			rc.setScmToolsHome(scmToolsHome)
			rc.setUserRTC(userRTC);
			rc.setPwdRTC(pwdRTC);
			rc.setUrlRTC(urlRTC);
			rc.setParentWorkspace(new File(parentWorkspace));
			rc.setLight(true);
			rc.setStream(produccionStream);
			rc.setComponent(componentName);
			rc.setBaseline(version);			
			rc.execute();
			
		}
	}
	
}



