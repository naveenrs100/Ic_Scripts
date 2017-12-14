package git

import es.eci.utils.SystemPropertyBuilder;
import git.commands.GitTaggerCommand;

/**
 * Funcionalidad de descarga de RTC implementada en groovy.
 * Ver la parametrización en RTCDownloaderCommand.groovy.
 */

// Obligatorios hasta el décimo

//Properties properties = new Properties()
//File propertiesFile = new File('version.txt')
//propertiesFile.withInputStream {
//	properties.load(it)
//}
//def version = properties.version;
//if(version == null) { throw new Exception("No se ha especificado version. No se puede crear tag.")}

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

GitTaggerCommand command = new GitTaggerCommand();

command.initLogger { println it }

propertyBuilder.populate(command);

//command.setTag(version);
//command.setComment(version);

command.execute();