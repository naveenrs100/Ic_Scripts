package aix

import aix.SGDAIXNexusUpload;
import es.eci.utils.SystemPropertyBuilder;

/**
 * Ver el funcionamiento en DockerBuildImage.groovy.
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

SGDAIXNexusUpload command = new SGDAIXNexusUpload();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();