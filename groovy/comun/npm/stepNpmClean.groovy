import es.eci.utils.SystemPropertyBuilder;
import es.eci.utils.npm.NpmCleanCommand

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

NpmCleanCommand command = new NpmCleanCommand();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();