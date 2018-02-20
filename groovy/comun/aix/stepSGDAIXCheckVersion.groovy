package aix

import aix.SGDAIXCheckVersion;
import es.eci.utils.SystemPropertyBuilder;

/**
 * Ver el funcionamiento en SGDAIXCheckVersion.groovy.
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

SGDAIXCheckVersion command = new SGDAIXCheckVersion();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();