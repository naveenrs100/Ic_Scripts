import es.eci.utils.SystemPropertyBuilder;
import rtc.RTCUtils
import rtc.commands.RTCCreateComponentCommand;

/**
 * Funcionalidad de creación de componente en RTC implementada en groovy.
 * Ver la parametrización en RTCCreateComponentCommand.groovy.
 */

// Obligatorios hasta el décimo

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

RTCCreateComponentCommand command = new RTCCreateComponentCommand();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();
