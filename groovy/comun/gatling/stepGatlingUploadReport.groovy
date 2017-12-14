package gatling

import es.eci.utils.SystemPropertyBuilder;
import es.eci.utils.gatling.GatlingUploadReport

/**
 * Ver el funcionamiento en GatlingUploadReport.groovy.
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

GatlingUploadReport command = new GatlingUploadReport();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();