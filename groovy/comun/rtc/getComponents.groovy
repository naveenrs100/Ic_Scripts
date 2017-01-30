import es.eci.utils.SystemPropertyBuilder
import rtc.RTCUtils
import rtc.commands.RTCgetComponentsCommand

/**
 * Funcionalidad de getComponents RTC implementada en groovy.
 * Parámetros de entrada:
 * 
 * --- OBLIGATORIOS
 * scmToolsHome Directorio raíz de las herramientas RTC 
 * daemonsConfigDir Directorio raíz de configuración de daemons (obsoleto)
 * userRTC Usuario RTC
 * pwdRTC Password RTC
 * urlRTC URL de repositorio RTC
 * onlyChanges Flag onlyChange (construcción de todos los componentes o sólo de los que continene cambios desde la última ejecución)
 * fileOut Fichero en el que se almacena el resultado
 * nameTarget Destino de la comparación (nombre del elemento definido en typeTarget)
 * parentWorkspace Directorio de ejecución 
 * typeOrigin Tipo de elemento del origen de la comparación
 * nameOrigin Origen de la comparación (nombre del elemento definido en typeOrigin)
 * typeTarget Tipo de elemento del destino de la comparación
 * light Indica si se debe usar o no la versión ligera del comando scm
 */

RTCgetComponentsCommand command = new RTCgetComponentsCommand();
command.initLogger { println it }

// Retrocompatibilidad.
if(checkProperties()) {
	println("INFO: Se toman los parámetros mediante properties.");
	SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
	propertyBuilder.populate(command);
	
} else if(args.length > 0) {	
	println("INFO: Se toman los parámetros mediante argumentos de entrada.");
	RTCUtils.validate(args, 10)
	int argCounter = 0;
	command.setScmToolsHome(args[argCounter++]);
	command.setDaemonsConfigDir(args[argCounter++]);
	command.setUserRTC(args[argCounter++]);
	command.setPwdRTC(args[argCounter++]);
	command.setUrlRTC(args[argCounter++]);
	command.setOnlyChanges(RTCUtils.toBoolean(args[argCounter++]));
	command.setFileOut(new File(args[argCounter++]));
	command.setNameTarget(args[argCounter++]);
	command.setParentWorkspace(new File(args[argCounter++]));
	command.setTypeOrigin(args[argCounter++]);
	command.setNameOrigin(args[argCounter++]);
	command.setTypeTarget(args[argCounter++]);
	command.setLight(RTCUtils.toBoolean(args[argCounter++]));
	
} else {
	println("Se necesitan parámetros de entrada, ya sea mediante argumentos o mediante properties.");
}

command.execute();

def checkProperties() {
	def props = false;
	System.properties.each { 
		def property = it.key;
		if (property.startsWith('param.')) { props = true; }
	}
	return props;	
}

