@GrabResolver (name='nexus', root='http://nexus.elcorteingles.int/content/groups/public/')
@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.3.6')

import es.eci.utils.CheckGitParameters;
import es.eci.utils.CheckSnapshots;
import es.eci.utils.SystemPropertyBuilder
import urbanCode.UrbanCodeExecutor;;

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
def inputParams = propertyBuilder.getSystemParameters();

def application = inputParams["application"];
def instantanea = inputParams["instantanea"];
def branch = inputParams["branch"];
def targetBranch = inputParams["targetBranch"].equals("") ? stream : inputParams["targetBranch"];
def rtcUrl = inputParams["rtcUrl"];
def rtcUser = inputParams["rtcUser"];
def rtcPass = inputParams["rtcPass"];
def udClientCommand = inputParams["udClientCommand"];
def urlUdeploy = inputParams["urlUdeploy"];
def userUdclient = inputParams["userUdclient"];
def pwdUdclient = inputParams["pwdUdclient"];

def gitGroup = inputParams["gitGroup"];
def urlGitlab = inputParams["urlGitlab"];
def privateGitLabToken = inputParams["privateGitLabToken"];
def keystoreVersion = inputParams["keystoreVersion"];
def urlNexus = inputParams["urlNexus"];


boolean existsGitGroup = CheckGitParameters.checkGitGroup(gitGroup, urlGitlab, privateGitLabToken, keystoreVersion, urlNexus);

CheckSnapshots chk = new CheckSnapshots();
chk.initLogger { println it };
UrbanCodeExecutor urbExe = new UrbanCodeExecutor(udClientCommand, urlUdeploy, userUdclient, pwdUdclient);
urbExe.initLogger { println it };
boolean existsUrbanSnapshot = chk.checkUrbanCodeSnapshots(urbExe, application, instantanea);

if(instantanea == null || instantanea.trim().equals("")) {
	throw new Exception("No ha sido informada la variable \"instantanea\".");
}
if(instantanea.indexOf(" ") != -1) {
	throw new Exception("No puede haber espacios en blanco en la variable \"instantanea\".");
}
if(!existsGitGroup) {
	throw new Exception("No existe el grupo \"${gitGroup}\" indicado en gitLab \"\${urlGitlab}\"");
}
if(existsUrbanSnapshot) {
	throw new Exception("Ya existe la instantánea \"${instantanea}\" en UrbanCode.");
}