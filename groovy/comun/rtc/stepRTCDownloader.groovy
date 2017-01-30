import es.eci.utils.SystemPropertyBuilder;
import rtc.RTCUtils;
import rtc.commands.RTCDownloaderCommand;

/**
 * Funcionalidad de descarga de RTC implementada en groovy.
 * Ver la parametrización en RTCDownloaderCommand.groovy.
 */

// Obligatorios hasta el décimo

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

RTCDownloaderCommand command = new RTCDownloaderCommand();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();
