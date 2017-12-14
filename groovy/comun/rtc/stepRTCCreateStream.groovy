package rtc

import es.eci.utils.SystemPropertyBuilder;
import rtc.RTCUtils
import rtc.commands.RTCCreateStreamCommand;

/**
 * Funcionalidad de creación de corriente en RTC implementada en groovy.
 * Ver la parametrización en RTCCreateStreamCommand.groovy.
 */

// Obligatorios hasta el décimo

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

RTCCreateStreamCommand command = new RTCCreateStreamCommand();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();
