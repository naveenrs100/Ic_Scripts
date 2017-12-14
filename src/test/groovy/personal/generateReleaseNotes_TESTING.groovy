import java.io.File;

import es.eci.utils.NexusHelper
@GrabResolver(name='nexuseci', root='http://nexus.elcorteingles.int/content/groups/public/')
@Grab(group='org.apache.poi', module='poi', version='3.6')

import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.ZipHelper;
import es.eci.utils.excel.HelperGenerateReleaseNotes;
import es.eci.utils.pom.MavenCoordinates
import es.eci.utils.TmpDir;


SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
def params = parameterBuilder.getSystemParameters();

/* Parámetros de pruebas RTC */
def aplicacionUrbanCode = "GIS-QUVE";
def instantanea1 = "QUVE_1.2.25.3";
def instantanea2 = "QUVE_1.2.25.1";

//def aplicacionUrbanCode = "88888-Prueba-GATES";
//def instantanea1 = "test_201708041634";
//def instantanea2 = "test_201708041656";


/* Parámetros globales */
def scmToolsHome = "C:/OpenDevECI/scmtools/eclipse";
def daemonConfigDir = "C:/OpenDevECI/scmtools/daemons_home";
def userRTC = "JENKINS_RTC";
def pwdRTC = "12345678";
def urlRTC = "https://rtc.elcorteingles.int:9443/ccm";
def fichasGroupId = "es.eci.fichas_urbancode";
def nexusUrl = "http://nexus.elcorteingles.int/content/groups/public";
def nexusUrlPre = "http://nexus.elcorteingles.pre/content/groups/public";
def n = "100";
def gitHost = "mx00000018d0323.eci.geci";
def gitUser = "git"

def parentWorkspace = new File(".");

/*************************/

HelperGenerateReleaseNotes helperRTC =
new HelperGenerateReleaseNotes(nexusUrl, pwdRTC, userRTC, urlRTC, n, parentWorkspace,
scmToolsHome, daemonConfigDir, fichasGroupId, gitHost, gitUser);
helperRTC.initLogger { println it }

String scm = getScmSource(fichasGroupId, aplicacionUrbanCode, instantanea1, nexusUrl)

if(scm == "rtc") {
	helperRTC.generateReleaseNotesRTC(aplicacionUrbanCode,instantanea1,instantanea2,"changelogRTC.xls");
} else if(scm == "git") {
	helperRTC.generateReleaseNotesGIT(aplicacionUrbanCode,instantanea1,instantanea2,"changelogGIT.xls");
} else  {
	println("No haym fichero rtc.json ó git.json")
}



private getScmSource(fichasGroupId, aplicacionUrbanCode, instantanea1, nexusUrl) {
	String scm;
	TmpDir.tmp { File tmpDir ->
		NexusHelper nxHelper = new NexusHelper(nexusUrl);
		MavenCoordinates coordinates = new MavenCoordinates(fichasGroupId, aplicacionUrbanCode, instantanea1);
		coordinates.setPackaging("zip");

		File descriptorFile = nxHelper.download(coordinates, tmpDir);

		ZipHelper zipHelper = new ZipHelper();
		zipHelper.unzipFile(descriptorFile, tmpDir);

		if(new File(tmpDir,"git.json").exists()) {
			scm = "git";
		} else if(new File(tmpDir,"rtc.json").exists()) {
			scm = "rtc";
		}

	}
	return scm;
}
























