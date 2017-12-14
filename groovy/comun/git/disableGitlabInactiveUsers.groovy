package git

@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.4')

/**
 * Se invoca como Groovy Script.  Consulta la API REST de gitlab para 
 * deshabilitar aquellos usuarios que lleven inactivos un número de días por
 * encima del umbral indicado
 * 
 * Parámetros
 * 
 * gitCommand - Comando git a utilizar
 * keystoreVersion - Versión del keystore de gitlab
 * privateGitLabToken - Autenticación de usuario en gitlab
 * urlGitlab - URL para los servicios rest de gitlab
 * urlNexus - URL de acceso a nexus
 * days - Número de días de inactividad necesarios para inhabilitar usuario
 */

import git.commands.GitDisableInactiveUsersCommand
import es.eci.utils.SystemPropertyBuilder;
 
 GitDisableInactiveUsersCommand command = new GitDisableInactiveUsersCommand();
 
 command.initLogger{ println it };
 
 SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
 propertyBuilder.populate(command);
 
 command.execute();