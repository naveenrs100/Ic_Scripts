package npm

import es.eci.utils.SystemPropertyBuilder;
import es.eci.utils.npm.NpmCompileCommand

/**
 * Funcionalidad de compilación NPM
 *
 * <br/>
 * Parámetros de entrada:<br/>
 * <br/>
 * --- OBLIGATORIOS<br/>
 * <b>parentWorkspace</b> Directorio de ejecución<br/>
 * <br/>
 * 
 */

// Obligatorios hasta el décimo

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

NpmCompileCommand command = new NpmCompileCommand();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();