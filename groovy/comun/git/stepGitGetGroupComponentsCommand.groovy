package git

@GrabResolver (name='nexus', root='http://nexus.elcorteingles.int/content/groups/public/')
@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.3.6')

import es.eci.utils.SystemPropertyBuilder;
import git.commands.GitGetGroupComponentsCommand

/**
 * Descarga desde un grupo de GIT todos los proyectos existentes, dejándolos en un fichero
 * en el WS de lanzamiento: jenkinsComponents.txt
 */

// Obligatorios hasta el décimo

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

GitGetGroupComponentsCommand command = new GitGetGroupComponentsCommand();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();