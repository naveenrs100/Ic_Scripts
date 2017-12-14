import es.eci.utils.ScmCommand
import es.eci.utils.StringUtil;
import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.TmpDir;
import es.eci.utils.NexusHelper
import es.eci.utils.ZipHelper
import es.eci.utils.pom.MavenCoordinates
import git.commands.GitPromoteTagToProductionCommand
import groovy.json.JsonSlurper
import rtc.RTCUtils;
import rtc.commands.RTCCreateSnapshot
import rtc.commands.RTCReplaceCommand

SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
def params = parameterBuilder.getSystemParameters();

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

NexusHelper nxHelper = new NexusHelper(nexusUrl);
MavenCoordinates coordinates = new MavenCoordinates(fichasGroupId, StringUtil.normalize(aplicacionUrbanCode), instantanea);
coordinates.setRepository("fichas_despliegue");
coordinates.setPackaging("zip");

TmpDir.tmp { File tmpDir ->
	File descriptorFile = nxHelper.download(coordinates, tmpDir);
	ZipHelper zipHelper = new ZipHelper();
	zipHelper.unzipFile(descriptorFile, tmpDir);

	File rtcJsonFile = new File(tmpDir,"rtc.json");
	File gitJsonFile = new File(tmpDir,"git.json");


	if(rtcJsonFile.exists()) {
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

				// Para evitar posible duplicidad en los nombres de baseline que corresponden a cada versión, buscaremos la última
				// uuid correspondiente a esa baseline y será la que se use a partir de ahora.
				RTCUtils ru = new RTCUtils();
				String baselineUuid = ru.getBaselineUuid(componentName, version, userRTC, pwdRTC, urlRTC, scmToolsHome);
				
				if(baselineUuid != null) {
					RTCReplaceCommand rc = new RTCReplaceCommand();
					rc.initLogger { println it };
					rc.setScmToolsHome(scmToolsHome)
					rc.setUserRTC(userRTC);
					rc.setPwdRTC(pwdRTC);
					rc.setUrlRTC(urlRTC);
					rc.setParentWorkspace(new File(parentWorkspace));
					rc.setLight(true);
					rc.setStream(produccionStream);
					rc.setComponent(componentName);
					rc.setBaseline(baselineUuid);
					rc.execute();
				} else {
					println("######### WARNING: No existe baseline \"${version}\" (uuid: ${baselineUuid}) para el componente ${componentName}");
				}
			}
		}

		// Hacer una snapshot de RTC de la stream de PRODUCCION recién creada mediante el comando:
		// scm create snapshot "GIS - Proyecto Prueba Release - PRODUCCION"
		// --name "SNAPSHOT_DE_PRUEBA_1" --description "Creada baseline de prueba"
		// -r https://rtc.elcorteingles.pre:59443/ccm -P 12345678 -u JENKINS_RTC
		RTCCreateSnapshot cs = new RTCCreateSnapshot();
		cs.initLogger { println it };
		cs.setScmToolsHome(scmToolsHome);
		cs.setUserRTC(userRTC);
		cs.setPwdRTC(pwdRTC);
		cs.setUrlRTC(urlRTC);
		cs.setParentWorkspace(new File(parentWorkspace));
		cs.setLight(true);
		cs.setStream(produccionStream);
		cs.setSnapshotName(instantanea);
		cs.setDescription("Snapshot creada automáticamente desde un paso a Produccion.");
		cs.execute();

	}
	else if(gitJsonFile.exists()) {
		def gitJsonObject = (new JsonSlurper()).parseText(gitJsonFile.text);

		String originStream = "RELEASE";
		String produccionBranch = "PRODUCCION";

		String grupoGit = gitJsonObject.source;

		gitJsonObject.versions.each { versionObject ->
			versionObject.keySet().each { String key ->
				String componentName = key;
				String version = versionObject.get(key);

				println("Promocionando el componente \"${componentName}\"(${version}) a la stream \"${produccionBranch}\" ...")

				TmpDir.tmp { File tmpDir2 ->
					GitPromoteTagToProductionCommand gp = new GitPromoteTagToProductionCommand();
					gp.initLogger { println it };
					gp.setParentWorkspace(tmpDir2.getCanonicalPath());
					gp.setProductionBranch(produccionBranch);
					gp.setTag(version);
					gp.setGitCommand("git");
					gp.setGitHost(gitHost)
					gp.setRepoPath("${grupoGit}/${componentName}");
					gp.execute();
				}
			}
		}
	}
	else {
		throw new Exception("No se pueden copiar los componentes a la stream de Producción " +
		"porque no hay archivo \"rtc.json\" ni \"git.json\" en el descriptor descargado.");
	}
}