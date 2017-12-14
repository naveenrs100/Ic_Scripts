import java.io.File;

import es.eci.utils.StringUtil;
import es.eci.utils.NexusHelper
@GrabResolver(name='nexuseci', root='http://nexus.elcorteingles.int/content/groups/public/')
@Grab(group='org.apache.poi', module='poi', version='3.6')

import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.excel.HelperGenerateReleaseNotes
import es.eci.utils.pom.MavenCoordinates
import es.eci.utils.TmpDir
import es.eci.utils.ZipHelper;


SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
def params = parameterBuilder.getSystemParameters();


def aplicacionUrbanCode = params["aplicacionUrbanCode"];
def instantanea1 = params["instantanea1"];
def instantanea2 = params["instantanea2"];
def lineaBaseInicial = params["lineaBaseInicial"];
def scmToolsHome = params["SCMTOOLS_HOME"]
def daemonConfigDir = params["DAEMONS_HOME"]
def userRTC = params["userRTC"]
def pwdRTC = params["pwdRTC"]
def urlRTC = params["urlRTC"]
def fichasGroupId = params["fichasGroupId"]
def nexusUrl = params["nexusUrl"]
def n = "100"; // Máximo de resultados a buscar.

def gitHost = params["GIT_HOST"];
def gitUser = params["GIT_USER"];

def parentWorkspace = new File(".");


HelperGenerateReleaseNotes helperRTC =
		new HelperGenerateReleaseNotes(nexusUrl, pwdRTC, userRTC, urlRTC, n, parentWorkspace,
		scmToolsHome, daemonConfigDir, fichasGroupId, gitHost, gitUser);
helperRTC.initLogger { println it }

String scm = getScmSource(fichasGroupId, aplicacionUrbanCode, instantanea1, nexusUrl)

if(scm.equals("rtc")) {
	helperRTC.generateReleaseNotesRTC(aplicacionUrbanCode,instantanea1,instantanea2,lineaBaseInicial,"changelogRTC.xls");
} else if(scm.equals("git")) {
	helperRTC.generateReleaseNotesGIT(aplicacionUrbanCode,instantanea1,instantanea2,"changelogGIT.xls");
} else  {
	println("No hay fichero rtc.json ó git.json");
}


/**
 * Averigua cuál es el SCM origen de los proyectos en base a que en la ficha de
 * despliegue se encuentre el rtc.json o el git.json
 * @param fichasGroupId
 * @param aplicacionUrbanCode
 * @param instantanea1
 * @param nexusUrl
 * @return
 */
private getScmSource(fichasGroupId, aplicacionUrbanCode, instantanea1, nexusUrl) {
	String scm;
	TmpDir.tmp { File tmpDir ->
		NexusHelper nxHelper = new NexusHelper(nexusUrl);
		MavenCoordinates coordinates = new MavenCoordinates(fichasGroupId, StringUtil.normalize(aplicacionUrbanCode), instantanea1);
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
