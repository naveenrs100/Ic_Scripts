import java.io.File

import es.eci.utils.SystemPropertyBuilder;
import git.commands.GitCloneCommand;

/**
 * Funcionalidad de descarga de git implementada en groovy.
    <br/><br/>
    --- OBLIGATORIOS<br/>
  
 	<b>gitUser</b> Usuario de conexión.<br/>
	<b>gitHost</b> URL de git.<br/>
	<b>gitPath</b> Ruta del repositorio en git (incluye nombre de grupo).<br/>
	<b>gitBranch</b> Rama de la que hacer el clone.<br/>
	<b>parentWorkspace</b> Directorio de trabajo.<br/>
	<br/>
	--- OPCIONALES<br/>
	
	<b>localFolderName</b> Directorio de destino del clone.<br/>
	<b>additionalParams</b> Parámetros adicionales al git clone.<br/>
	<b>tag</b> Si viene informada la etiqueta, baja el contenido de la etiqueta.<br/>
	<b>empty</b> Por defecto: false.  Si viene informado, hace un git clone --no-checkout, es
		decir, un dry run<br/>
	<b>gitCommand</b> Por defecto: git.  Si viene informado, usa un comando git distinto
		en la máquina de ejecución<br/>
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

GitCloneCommand command = new GitCloneCommand();

command.initLogger { println it }

propertyBuilder.populate(command);

command.execute();