import java.io.File;

import rtc.commands.RTCReplaceCommand;
import es.eci.utils.GitCommand;
import es.eci.utils.NexusHelper
import es.eci.utils.SystemPropertyBuilder;
import es.eci.utils.ZipHelper
import es.eci.utils.pom.MavenCoordinates
import es.eci.utils.StringUtil;
import es.eci.utils.TmpDir;
import git.commands.GitCloneCommand;
import git.commands.GitPromoteTagToProductionCommand;
import groovy.json.JsonSlurper

SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
def params = parameterBuilder.getSystemParameters();

def instantanea = params["instantanea"];
//def instantanea = "test_201709121452"; 
//def instantanea = "test_201709121656";

def aplicacionUrbanCode = params["aplicacionUrbanCode"];
//def aplicacionUrbanCode = "88888 - Prueba GATES";

def nexusUrl = params["nexusUrl"]
//def nexusUrl = "http://nexus.elcorteingles.pre/content/groups/public";

def fichasGroupId = params["fichasGroupId"];
//def fichasGroupId = "es.eci.fichas_urbancode";

def parentWorkspace = params["parentWorkspace"];
//def parentWorkspace = new File("C:/Users/dcastro.jimenez/Desktop/Herramientas ECI/tmp");

def gitHost = params["gitHost"]
//def gitHost = "mx00000018d0323.eci.geci"

def userGit = params["userGit"];
//def userGit = "ecici";

/*
 * Método de promoción de una tag a la rama PRODUCCION
 * git clone <host>:<path> --branch PRODUCCION
 * git reset --hard "tag_ABC"
 * git push --force origin PRODUCCION
 */

NexusHelper nxHelper = new NexusHelper(nexusUrl);
MavenCoordinates coordinates = new MavenCoordinates(fichasGroupId, StringUtil.normalize(aplicacionUrbanCode), instantanea);
coordinates.setPackaging("zip");

TmpDir.tmp { File tmpDir ->
	File descriptorFile = nxHelper.download(coordinates, tmpDir);
	ZipHelper zipHelper = new ZipHelper();
	zipHelper.unzipFile(descriptorFile, tmpDir);
	
	File gitJsonFile = new File(tmpDir,"git.json");
	
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



