package jenkins.release

import release.GetReleaseInfo;

@GrabResolver (name='nexus', root='http://nexus.elcorteingles.int/content/groups/public/')
@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.4')

import es.eci.utils.SystemPropertyBuilder;

/**
 * Deja en un fichero la información de la release actual
 */
SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

GetReleaseInfo command = new GetReleaseInfo();

command.initLogger { println it }

propertyBuilder.populate(command);

command.storeReleaseInfoIntoFile();