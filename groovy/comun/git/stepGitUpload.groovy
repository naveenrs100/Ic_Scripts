import es.eci.utils.SystemPropertyBuilder;
import git.commands.GitUploadCommand;

/**
 * Funcionalidad de descarga de RTC implementada en groovy.
 * Ver la parametrización en RTCDownloaderCommand.groovy.
 */

// Obligatorios hasta el décimo

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

GitUploadCommand command = new GitUploadCommand();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();