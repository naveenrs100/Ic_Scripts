import es.eci.utils.SystemPropertyBuilder;
import rtc.RTCUtils
import rtc.commands.RTCCheckinCommand
import rtc.commands.RTCCreateRepositoryWorkspace;

/**
 * Funcionalidad de check-in RTC implementada en groovy.
 * Ver la parametrización en la clase RTCCreateRepositoryWorkspace.groovy.
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

RTCCreateRepositoryWorkspace command = new RTCCreateRepositoryWorkspace();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();