import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.npm.NpmMavenUploadCommand;

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
 * <b>nexusPathOpen</b> url completa de nexus para subir la versión abierta<br/>
 * <br/>
 * <b>nexusPathClosed</b> url completa de nexus para subir la versión cerrada<br/>
 * <br/>
 * <b>type</b> extension del fichero<br/>
 * <br/>
 * <b>distFolder</b> carpeta donde se encuentra el fichero generado<br/>
 * <br/>
 * 
 *
*/

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

NpmMavenUploadCommand command = new NpmMavenUploadCommand();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();