package jenkins.release

import es.eci.utils.NexusHelper
import es.eci.utils.StringUtil
@GrabResolver (name='nexus', root='http://nexus.elcorteingles.int/content/groups/public/')
@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.4')

import es.eci.utils.SystemPropertyBuilder;
import es.eci.utils.TmpDir
import es.eci.utils.ZipHelper
import es.eci.utils.pom.MavenCoordinates
import groovy.json.JsonSlurper
import release.GetReleaseInfo

/**
 * Actualiza sobre RTC, o JIRA, la información de la release actual, cerrándola. 
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

def params = propertyBuilder.getSystemParameters();

def aplicacionUrbanCode = 	params["aplicacionUrbanCode"];
def instantanea = 			params["instantanea"];
def fichasGroupId = 		params["fichasGroupId"];
def nexusUrl = 				params["nexusUrl"];
def gitHost = 				params["gitHost"]
def userGit = 				params["userGit"];
def parentWorkspace = 		params["parentWorkspace"];
def scmToolsHome = 			params["scmToolsHome"];
def userRTC = 				params["userRTC"];
def pwdRTC = 				params["pwdRTC"];
def urlRTC = 				params["urlRTC"];

def releaseId = null;

NexusHelper nxHelper = new NexusHelper(nexusUrl);
MavenCoordinates coordinates = new MavenCoordinates(fichasGroupId, StringUtil.normalize(aplicacionUrbanCode), instantanea);
coordinates.setRepository("fichas_despliegue");
coordinates.setPackaging("zip");


// Recuperar la release ID que se creó durante el proceso de IC
TmpDir.tmp { File tmpDir ->
	File descriptorFile = nxHelper.download(coordinates, tmpDir);
	ZipHelper zipHelper = new ZipHelper();
	zipHelper.unzipFile(descriptorFile, tmpDir);

	File rtcJsonFile = new File(tmpDir,"rtc.json");
	File gitJsonFile = new File(tmpDir,"git.json");
	
	File file = null;
	if (rtcJsonFile.exists()) { file = rtcJsonFile }
	if (gitJsonFile.exists()) { file = gitJsonFile }
	if (file != null) {
		def json = new JsonSlurper().parseText(file.text)
		releaseId = json.releaseId
	}
}

if (releaseId == null) {
	println "[WARNING] No se guardó id de release, no se puede cerrar"	
}
else {
	println "Procediendo a cerrar la release..."
	GetReleaseInfo command = new GetReleaseInfo();
	
	command.initLogger { println it }
	
	propertyBuilder.populate(command);
	
	command.setReleaseId(releaseId);
	
	command.closeRelease();
}
