package rtc

import es.eci.utils.SystemPropertyBuilder;
import rtc.RTCUtils
import rtc.commands.RTCDeliverCommand

/**
 * Funcionalidad de deliver RTC implementada en groovy.
 * Ver la parametrización en RTCDeliverCommand.groovy.
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

RTCDeliverCommand command = new RTCDeliverCommand();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();
