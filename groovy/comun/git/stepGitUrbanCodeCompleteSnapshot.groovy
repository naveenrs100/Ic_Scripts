@GrabResolver (name='nexus', root='http://nexus.elcorteingles.int/content/groups/public/')
@Grab(group='com.ibm.icu', module='icu4j', version='57.1')

import es.eci.utils.SystemPropertyBuilder;
import git.commands.GitUrbanCodeCreateCompleteDescriptorCommand;

/**
 * Funcionalidad de descarga de RTC implementada en groovy.
 * Ver la parametrización en RTCDownloaderCommand.groovy.
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

GitUrbanCodeCreateCompleteDescriptorCommand command = new GitUrbanCodeCreateCompleteDescriptorCommand();

command.initLogger { println it }

propertyBuilder.populate(command);

// Se crea la snapshot en UrbanCode
command.execute();

