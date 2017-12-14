package gatling

import es.eci.utils.SystemPropertyBuilder;
import es.eci.utils.gatling.GatlingModifyConfigFile

/**
 * Ver el funcionamiento en GatlingModifyConfigFile.groovy.
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

GatlingModifyConfigFile command = new GatlingModifyConfigFile();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();