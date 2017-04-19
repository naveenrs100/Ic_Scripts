import docker.DockerBuildImage
import es.eci.utils.SystemPropertyBuilder;

/**
 * Ver el funcionamiento en DockerBuildImage.groovy.
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

DockerBuildImage command = new DockerBuildImage();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();