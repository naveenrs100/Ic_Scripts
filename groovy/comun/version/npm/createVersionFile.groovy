package version.npm

import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.npm.CreateVersionFileCommand



/**
 * Esta clase modela el entry point para el Check de una version 
 * 
 * <br/>
 * Parámetros de entrada:<br/>
 * <br/>
 * --- OBLIGATORIOS<br/>
 * <b>parentWorkspace</b> Directorio de ejecución<br/>
 * <br/>
 * --- OPCIONALES<br/>
 * <b>filename</b> Nombre del fichero de configuración<br/>
 *
*/


SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

CreateVersionFileCommand command = new CreateVersionFileCommand()

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();