import es.eci.utils.NexusHelper
import es.eci.utils.ParameterValidator
import es.eci.utils.StringUtil
import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.commandline.CommandLineHelper
import es.eci.utils.pom.MavenCoordinates

/**
 * Este paso prueba la conectividad con Nexus.
 * 
 * Baja un artefacto de unas coordenadas.
 * 
 * Parámetros
 * 
 * groupId - groupId de artefacto
 * artifactId - artifactId de artefacto
 * version - Versión de artefacto
 * urlNexus - URL de Nexus
 * parentWorkspace - Directorio de trabajo
 * 
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
Map params = propertyBuilder.getSystemParameters();

String groupId = params['groupId']
String artifactId = params['artifactId']
String version = params['version']
String urlNexus = params['urlNexus']
String parentWorkspace = params['parentWorkspace']
 
ParameterValidator.builder().
 	add("groupId", groupId).
 	add("artifactId", artifactId).
 	add("version", version).
 	add("urlNexus", urlNexus).
 	add("parentWorkspace", parentWorkspace, 
		 { StringUtil.notNull(it) && new File((String) it).exists()}).
	 			build().validate();
	 
new NexusHelper(urlNexus).download(
	new MavenCoordinates(groupId, artifactId, version),
	new File(parentWorkspace))

// Listar ficheros en el directorio
println new File(parentWorkspace).listFiles()
 