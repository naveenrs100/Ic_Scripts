package jenkins

@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.4')

import es.eci.utils.CheckSnapshots;
import es.eci.utils.StringUtil;
import es.eci.utils.SystemPropertyBuilder;
import groovy.json.JsonSlurper
import release.GetReleaseInfo
import urbanCode.UrbanCodeExecutor

def instantanea;
def stream;
def streamTarget;
def rtcUrl;
def rtcUser;
def rtcPass;
def componentsUrban;
def urbanCodeConnection;
def provider;
def releaseManagement;

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
def inputParams = propertyBuilder.getSystemParameters();

if(checkProperties()) {	
	instantanea = inputParams["instantanea"];
	stream = inputParams["stream"];
	streamTarget = inputParams["streamTarget"].equals("") ? stream : inputParams["streamTarget"];
	rtcUrl = inputParams["rtcUrl"];
	rtcUser = inputParams["rtcUser"];
	rtcPass = inputParams["rtcPass"];
	componentsUrban = inputParams["componentsUrban"];
	provider = inputParams["provider"];
	
}
else if(args.length > 0) {
	instantanea = 	  		args[1];
	stream = 		  		args[2];
	streamTarget = 	  		args[3].equals("") ? stream : args[3];
	rtcUrl = 		  		args[4];
	rtcUser = 		 		args[5];
	rtcPass = 		  		args[6];
	urbanCodeConnection = 	args[11];
	provider = 				args[12];
	
	println("Los parámetros usados son:")	
	println("Parámetro instantanea: " + instantanea)
	println("Parámetro stream: " + stream)
	println("Parámetro streamTarget: " + streamTarget)
	println("Parámetro rtcUrl: " + rtcUrl)
	println("Parámetro rtcUser: " + rtcUser)
	println("Parámetro rtcPass: " + rtcPass)	
	println("Parámetro urbanCodeConnection: " + urbanCodeConnection)
	println("Parámetro provider: " + provider);
	
}

/*
 * Error si instantánea viene vacía o nula
 */
if(instantanea.startsWith('$') || instantanea == null || instantanea.trim().equals("")) {
	throw new Exception("No ha sido informada la variable \"instantanea\".");
}
if(instantanea.indexOf(" ") != -1) {
	throw new Exception("No puede haber espacios en blanco en la variable \"instantanea\".");
}

/*
 * Error si provider viene vacía o nula
// */
if(provider == null || provider.trim().equals("") || provider.trim().startsWith("\${")) {
	throw new Exception("No ha sido informada la variable \"provider\" desde QUVE.");
}


CheckSnapshots chk = new CheckSnapshots();
chk.initLogger { println it };	

if (stream != null && stream.trim().length() > 0) {

	boolean existsRtcStream = chk.checkRTCstreams(streamTarget, rtcUser, rtcPass, rtcUrl);
	boolean existsRtcSnapshot = chk.checkRTCSnapshots(streamTarget, instantanea, rtcUser, rtcPass, rtcUrl);
	
	if(!existsRtcStream) {
		throw new Exception("No existe el stream \"${streamTarget}\" en RTC.");
	}
	if(existsRtcSnapshot) {
		throw new Exception("Ya existe la snapshot \"${streamTarget} - ${instantanea}\" en RTC.");
	}
	
}

releaseManagement = inputParams["releaseManagement"];
boolean manageRelease = false;
if (!StringUtil.isNull(releaseManagement)) {
	manageRelease = Boolean.parseBoolean(releaseManagement);
}

if (manageRelease) {
	println "--> Gestión de release"
	// Comprobar: que el workitem/issue de release existe
	// Comprobar que es del tipo adecuado
	// Comprobar que no está cancelado ni finalizado
	GetReleaseInfo command = new GetReleaseInfo();
	command.initLogger { println it }	
	propertyBuilder.populate(command);
	// Este comando de validación fallará si no se cumple alguna de las 
	//	condiciones
	command.validateRelease();
}
else {
	println "[WARNING] Se salta la gestión de releases al estar desactivada"
}

def checkProperties() {
	def props = false;
	System.properties.each {
		def property = it.key;
		if (property.startsWith('param.')) { props = true; }
	}
	return props;
}