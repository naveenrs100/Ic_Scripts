import es.eci.utils.SystemPropertyBuilder;
import rtc.RTCUtils
import rtc.commands.RTCReplaceCommand;

/**
 * Funcionalidad para reemplazar un componente en una corriente por una
 * línea base concreta.
 * Ver la parametrización en RTCReplaceCommand.groovy.
 * 
 */

// Obligatorios hasta el décimo

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

RTCReplaceCommand command = new RTCReplaceCommand();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();