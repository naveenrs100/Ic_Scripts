import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.npm.NpmMavenUploadDcosCommand;

/**
 * Esta clase modela el entry point para el upload a Nexus con maven
 *
 * <br/>
 * Parámetros de entrada:<br/>
 * <br/>
 * --- OBLIGATORIOS<br/>
 * <b>parentWorkspace</b> Directorio de ejecución<br/>
 * <br/>
 * <b>maven</b> ejecutable maven (ejemplo ejemplo mvn si esta en el path) <br/>
 * <br/>
 * <b>groupId</b> grupo del artefacto que se subirá a nexus<br/>
 * <br/>
 * <b>artifactId</b> nombde del artifact<br/>
 * <br/>
 * <b>nexusPath</b> url completa de nexus para subir la versión<br/>
 *
*/

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

NpmMavenUploadDcosCommand command = new NpmMavenUploadDcosCommand();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();