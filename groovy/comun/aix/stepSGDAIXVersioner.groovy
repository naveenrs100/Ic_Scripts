package aix

import aix.SGDAIXVersioner;
import es.eci.utils.SystemPropertyBuilder;

/**
 * Ver el funcionamiento en SGDAIXVersioner.groovy.
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

SGDAIXVersioner command = new SGDAIXVersioner();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();