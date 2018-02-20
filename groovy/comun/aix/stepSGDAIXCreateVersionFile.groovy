package aix

import aix.SGDAIXCreateVersionFile;
import es.eci.utils.SystemPropertyBuilder;

/**
 * Ver el funcionamiento en SGDAIXCreateVersionFile.groovy.
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

SGDAIXCreateVersionFile command = new SGDAIXCreateVersionFile();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();