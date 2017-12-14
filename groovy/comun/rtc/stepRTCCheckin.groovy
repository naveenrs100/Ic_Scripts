package rtc

import es.eci.utils.SystemPropertyBuilder;
import rtc.RTCUtils
import rtc.commands.RTCCheckinCommand

/**
 * Funcionalidad de check-in RTC implementada en groovy.
 * Ver la parametrización en la clase RTCCheckinCommand.groovy.
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

RTCCheckinCommand command = new RTCCheckinCommand();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();
