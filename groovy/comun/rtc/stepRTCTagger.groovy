import es.eci.utils.SystemPropertyBuilder;
import rtc.RTCUtils;
import rtc.commands.RTCTaggerCommand;

/**
 * Funcionalidad de create baseline y create snapshot RTC implementada en groovy.
 * Ver la parametrización en RTCTaggerCommand.groovy.
 * 
 */

// Obligatorios hasta el noveno

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

RTCTaggerCommand command = new RTCTaggerCommand();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();