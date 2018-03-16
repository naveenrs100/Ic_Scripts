package git

@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.3.6')

import git.commands.GitGetGroupComponentsCommand;
import es.eci.utils.SystemPropertyBuilder;

GitGetGroupComponentsCommand command = new GitGetGroupComponentsCommand();

command.initLogger{	println it };

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
propertyBuilder.populate(command);

command.execute();