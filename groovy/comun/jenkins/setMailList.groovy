package jenkins;

import es.eci.utils.NexusHelper;
import es.eci.utils.pom.MavenCoordinates
import es.eci.utils.ParamsHelper;
import es.eci.utils.StringUtil;
import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.TmpDir;
import es.eci.utils.ZipHelper
import groovy.json.JsonOutput;
import groovy.json.JsonSlurper
import hudson.model.ParametersAction
import hudson.model.StringParameterValue


def nexusUrl = 				build.getEnvironment(null).get("MAVEN_REPOSITORY")
def fichasGroupId = 		build.getEnvironment(null).get("URBAN_GROUP_ID")
def aplicacionUrbanCode = 	build.buildVariableResolver.resolve("aplicacionUrbanCode");
def instantanea = 			build.buildVariableResolver.resolve("instantanea");

NexusHelper nxHelper = new NexusHelper(nexusUrl);
nxHelper.initLogger { println it }
MavenCoordinates coordinates = new MavenCoordinates(fichasGroupId, StringUtil.normalize(aplicacionUrbanCode), instantanea);
coordinates.setRepository("fichas_despliegue");
coordinates.setPackaging("zip");

TmpDir.tmp { File tmpDir ->
	File descriptorFileZip = nxHelper.download(coordinates, tmpDir);
	ZipHelper zipHelper = new ZipHelper();	
	zipHelper.unzipFile(descriptorFileZip, tmpDir);

	File rtcJsonFile = new File(tmpDir,"rtc.json");
	File gitJsonFile = new File(tmpDir,"git.json");

	def rtcJsonObject;

	if(rtcJsonFile.exists()) {
		rtcJsonObject = (new JsonSlurper()).parseText(rtcJsonFile.text);
		println "rtcJsonFile:\n" + rtcJsonFile.text;
	} else if(gitJsonFile.exists()) {
		rtcJsonObject = (new JsonSlurper()).parseText(gitJsonFile.text);
		println "gitJsonFile:\n" + gitJsonFile.text;
	}

	// Inlcuidmos la lista de mails como parámetro
	
	String managersMail = "GCSoporteplataformaIC@elcorteingles.es";

	if(rtcJsonObject != null && rtcJsonObject.managersMail != null && !rtcJsonObject.managersMail.startsWith("\${")) {
		managersMail = rtcJsonObject.managersMail + ",GCSoporteplataformaIC@elcorteingles.es";
	}

	ParamsHelper pHelper = new ParamsHelper();
	def paramsToSet = [];
	paramsToSet.add(new StringParameterValue("MAIL_LIST", managersMail));
	
	setParams(build,paramsToSet);
}


public void setParams(build, params) {
	def paramsIn = build?.actions.find{ it instanceof ParametersAction }?.parameters
	def index = build?.actions.findIndexOf{ it instanceof ParametersAction }
	def paramsTmp = []
	if (paramsIn!=null) {
		//No se borra nada para compatibilidad hacia atrás.
		paramsTmp.addAll(paramsIn)
		//Borra de la lista los paramaterAction
		build?.actions.remove(index)
	}
	paramsTmp.addAll(params)

	build?.actions.add(new ParametersAction(paramsTmp))
}
