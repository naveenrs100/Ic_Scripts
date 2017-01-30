import es.eci.utils.SystemPropertyBuilder;
import es.eci.utils.CleanAdHoc;

/**
 * Ver el funcionamiento en CleanAdHoc.groovy.
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

CleanAdHoc command = new CleanAdHoc();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();