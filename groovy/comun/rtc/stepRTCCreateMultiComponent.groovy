package rtc

import java.util.List
import java.util.Map;

import es.eci.utils.ComponentVersionHelper;
import es.eci.utils.SystemPropertyBuilder;
import rtc.RTCUtils;
import rtc.commands.RTCCreateComponentCommand;
import rtc.commands.RTCCreateWorkspaceCommand;
import rtc.commands.RTCCreateStreamCommand;
import rtc.commands.RTCLoadWorkspaceCommand

/**
 * Funcionalidad de creación de varios componentes en RTC implementada en groovy.
 * Ver la parametrización en RTCCreateComponentCommand.groovy.
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
RTCCreateComponentCommand command_component = new RTCCreateComponentCommand();
RTCCreateStreamCommand command_stream = new RTCCreateStreamCommand();
RTCCreateWorkspaceCommand command_ws = new RTCCreateWorkspaceCommand();
RTCLoadWorkspaceCommand command_loadws = new RTCLoadWorkspaceCommand();
ComponentVersionHelper helper = new ComponentVersionHelper();

command_component.initLogger { println it }
command_stream.initLogger { println it }
command_ws.initLogger { println it }
command_loadws.initLogger { println it }

// Obtener los parametros del job
Map<String, String> parametros = propertyBuilder.getSystemParameters();
List<String> componentesTraspasar = null;

// Obtener la lista de componentes en origen que se deben replicar en destino
try {
	componentesTraspasar = helper.getComponents(
		new File(parametros.get("parentWorkspace")), parametros.get("stream"),
		parametros.get("userRTC"), parametros.get("pwdRTC"), parametros.get("origen"));
	helper.log("Componentes recuperados : ");
	componentesTraspasar.each { println it }
} catch(Exception e) {
	e.printStackTrace();
	throw e;
}

// Se crea la corriente en destino si no existe
propertyBuilder.populate(command_stream);
// Actualizamos el destino, ya que el parámetro urlRTC no viene informado en el propertyBuilder
/*command_stream.setUrlRTC(parametros.get("destino"));

try {
	command_stream.execute();
} catch(Exception e) {
	e.printStackTrace();
	throw e;
}*/

// Se crean los componentes vacios en destino
/*propertyBuilder.populate(command_component);
command_component.setUrlRTC(parametros.get("destino"));

componentesTraspasar.each {
	command_component.setComponent(it);
	try {
		command_component.execute();
	} catch(Exception e) {
		e.printStackTrace();
		throw e;
	}
}*/

// Creación del workspace de origen y de destino
/* propertyBuilder.populate(command_ws);
command_ws.setUrlRTC(parametros.get("origen"));
String nombre_origen = UUID.randomUUID().toString();
command_ws.log("### Workspace generado: " + nombre_origen);
command_ws.setWsName(nombre_origen);
try {
	command_ws.execute();
} catch(Exception e) {
	e.printStackTrace();
	throw e;
}
command_ws.log("### Workspace " + nombre_origen + " creado en origen ###"); */

// Borrar
propertyBuilder.populate(command_ws);

command_ws.setUrlRTC(parametros.get("destino"));
String nombre_destino = UUID.randomUUID().toString();
command_ws.log("### Workspace generado: " + nombre_destino);
command_ws.setWsName(nombre_destino);
try {
	command_ws.execute();
} catch(Exception e) {
	e.printStackTrace();
	throw e;
}
command_ws.log("### Workspace " + nombre_destino + " creado en destino ###");

// Carga de los workspace creados
propertyBuilder.populate(command_loadws);
command_loadws.setUrlRTC(parametros.get("destino"));
command_loadws.setWsName(nombre_destino);
// String sandbox = parametros.get("parentWorkspace") + "/" + nombre_destino;
command_loadws.log("### Sandbox destino: " + nombre_destino);
command_loadws.setSandbox(nombre_destino);
try {
	command_loadws.execute();
} catch(Exception e) {
	e.printStackTrace();
	throw e;
}
