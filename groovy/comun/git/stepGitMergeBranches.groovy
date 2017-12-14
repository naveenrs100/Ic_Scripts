package git

import es.eci.utils.SystemPropertyBuilder;
import git.commands.GitMergeBranchesCommand;;

/**
 * Funcionalidad de descarga de RTC implementada en groovy.
 * Ver la parametrización en RTCDownloaderCommand.groovy.
 */

// Obligatorios hasta el décimo

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

GitMergeBranchesCommand command = new GitMergeBranchesCommand();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();