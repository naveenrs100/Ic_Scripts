package atlassian

@Grab(group='com.google.code.gson', module='gson', version='1.7.1')
@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.4')

import atlassian.ExtractUsersAndProjects
import es.eci.utils.SystemPropertyBuilder

/**
 * Ver el funcionamiento en ExtractUsersAndProjects.groovy.
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

ExtractUsersAndProjects command = new ExtractUsersAndProjects();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();