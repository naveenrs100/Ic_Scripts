import es.eci.utils.CheckSnapshots;
import es.eci.utils.SystemPropertyBuilder;
import groovy.json.JsonSlurper
import urbanCode.UrbanCodeExecutor

def application;
def instantanea;
def stream;
def streamTarget;
def rtcUrl;
def rtcUser;
def rtcPass;
def udClientCommand;
def urlUdeploy;
def userUdclient;
def pwdUdclient;
def componentsUrban;

// Comunicación con Urban: variable global
def urbanCodeConnection;


SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
def inputParams = propertyBuilder.getSystemParameters();

if(checkProperties()) {
	application = inputParams["application"];
	instantanea = inputParams["instantanea"];
	stream = inputParams["stream"];
	streamTarget = inputParams["streamTarget"].equals("") ? stream : inputParams["streamTarget"];
	rtcUrl = inputParams["rtcUrl"];
	rtcUser = inputParams["rtcUser"];
	rtcPass = inputParams["rtcPass"];
	udClientCommand = inputParams["udClientCommand"];
	urlUdeploy = inputParams["urlUdeploy"];
	userUdclient = inputParams["userUdclient"];
	pwdUdclient = inputParams["pwdUdclient"];
	componentsUrban = inputParams["componentsUrban"];
	urbanCodeConnection = inputParams["urbanCodeConnection"];
	
}
else if(args.length > 0) {
	application = 	  		args[0];
	instantanea = 	  		args[1];
	stream = 		  		args[2];
	streamTarget = 	  		args[3].equals("") ? stream : args[3];
	rtcUrl = 		  		args[4];
	rtcUser = 		 		args[5];
	rtcPass = 		  		args[6];
	udClientCommand = 		args[7];
	urlUdeploy = 	  		args[8];
	userUdclient = 	  		args[9];
	pwdUdclient = 	  		args[10];
	urbanCodeConnection = 	args[11];
	
	println("Los parámetros usados son:")
	println("Parámetro application: " + application)
	println("Parámetro instantanea: " + instantanea)
	println("Parámetro stream: " + stream)
	println("Parámetro streamTarget: " + streamTarget)
	println("Parámetro rtcUrl: " + rtcUrl)
	println("Parámetro rtcUser: " + rtcUser)
	println("Parámetro rtcPass: " + rtcPass)
	println("Parámetro udClientCommand: " + udClientCommand)
	println("Parámetro urlUdeploy: " + urlUdeploy)
	println("Parámetro userUdclient: " + userUdclient)
	println("Parámetro pwdUdclient: " + pwdUdclient)
	println("Parámetro urbanCodeConnection: " + urbanCodeConnection)
	
}

// ¿Hay comunicación con urban?
boolean urbanEnabled = true;
if (urbanCodeConnection != null && urbanCodeConnection.trim().length() > 0) {
	urbanEnabled = Boolean.valueOf(urbanCodeConnection);
}

if(instantanea == null || instantanea.trim().equals("")) {
	throw new Exception("No ha sido informada la variable \"instantanea\".");
}
if(instantanea.indexOf(" ") != -1) {
	throw new Exception("No puede haber espacios en blanco en la variable \"instantanea\".");
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

// Si no está informada la aplicación urban 
if (urbanEnabled && application != null && application.trim().length() > 0) {
	UrbanCodeExecutor urbExe = new UrbanCodeExecutor(
										udClientCommand, 
										urlUdeploy, 
										userUdclient, 
										pwdUdclient);
	urbExe.initLogger { println it };					
	boolean existsUrbanSnapshot = chk.checkUrbanCodeSnapshots(urbExe, application, instantanea);
	List composNotUrban = chk.checkComposInUrban(urbExe, application, componentsUrban);
	
	if(existsUrbanSnapshot) {
		throw new Exception("Ya existe la instantánea \"${instantanea}\" en UrbanCode.");
	}
	if(composNotUrban.size() > 0) {
		composNotUrban.each {
			println("NO ESTÁ EN DADO DE ALTA EN URBANCODE EL COMPONENTE: \"${it}\"");
		}
		throw new Exception("Hay componentes que no están en UrbanCode:");	
	}
}

def checkProperties() {
	def props = false;
	System.properties.each {
		def property = it.key;
		if (property.startsWith('param.')) { props = true; }
	}
	return props;
}