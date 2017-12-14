/**
 * 
 */
package cobertura

import java.io.File;

import groovy.io.FileVisitResult;

/**
 * Esta clase analiza un directorio en busca de pruebas unitarias para determinar
 * si es necesario o no pasar cobertura a dicho directorio.
 */
class CoberturaHelper {
	
	/**
	 * Este método indica si existen fuentes para pruebas unitarias dentro de un
	 * directorio
	 * @param folder Directorio de proyecto
	 * @param technology Tecnología del proyecto (maven/gradle)
	 * @return Cierto si existe alguna prueba unitaria en el proyecto, falso
	 * en otro caso
	 */
	public static boolean findUnitTests(File folder, String technology) {
		boolean ret = false;
		
		// Buscar los descriptores maven dentro del directorio
		List<File> buildFiles = findBuildFiles(folder, technology);
		
		// Para cada uno de ellos, buscar el directorio donde estarían 
		//	los fuentes de las unitarias
		List<File> unitTestFolders = findUnitTestFolders(buildFiles, technology);
		
		// Buscar fuentes java en cada uno de esos directorios de tests
		List<File> javaSources = findUnitTestSources(unitTestFolders);
		ret = javaSources.size() > 0
		
		return ret;
	}
	
	/**
	 * Busca ficheros de construcción dentro del directorio de proyecto
	 * @param folder Directorio de proyecto
	 * @param technology Tecnología del proyecto (maven/gradle)
	 * @return Lista de ficheros de construcción dentro del directorio de proyecto
	 */
	private static List<File> findBuildFiles(File folder, String technology) {
		def tecnologias = ["maven":"pom\\.xml","gradle":"build\\.gradle"]
		List<File> ret = new LinkedList<File>();
		folder.traverse(
			type: groovy.io.FileType.FILES,
			preDir: { if (it.name.startsWith(".") || it.name == 'target') 
				return FileVisitResult.SKIP_SUBTREE
			},
			nameFilter: ~/${tecnologias[technology]}/
		){
			ret << it
		}
		return ret;		
	}
	
	// Construye una ruta con los argumentos
	private static String ruta(String... args) {
		List<String> laRuta = new LinkedList<String>();
		args.each { arg ->
			laRuta << arg
		}
		return laRuta.join(System.getProperty("file.separator"));
	}
	
	// Toma una ruta y la añade al directorio base
	private static void anyadirRuta(List<String> ret, String s, File directorioBase) {		
		String[] dirs = s.split(/[\\/]/)
		List<String> path = [ directorioBase.getCanonicalPath() ]
		dirs.each { dir -> path << dir }
		ret.add(new File(path.join(System.getProperty("file.separator"))));
	}
	
	// Si existe, añade la ruta estándar
	private static void anyadirRutaEstandar(List<File> ret, File directorioBase) {
		File ruta = new File(ruta(directorioBase.getCanonicalPath(), "src", "test", "java"))
		if (ruta.exists() && ruta.isDirectory()) {
			ret.add(ruta)
		}
	}
	
	/**
	 * Busca el directorio de pruebas unitarias dentro de cada fichero.  Si no tiene
	 * ningún directorio definido, asume que es src/test/java
	 * @param buildFiles Lista de ficheros de construcción
	 * @param technology Tecnología del proyecto (maven/gradle)
	 * @return
	 */
	private static List<File> findUnitTestFolders(List<File> buildFiles, String technology) {
		List<File> ret = new LinkedList<File>();
		if (buildFiles != null) {
			buildFiles.each { File file ->
				File directorioBase = file.getParentFile()
				if ("maven".equals(technology)) {
					// Por defecto, src/test/java
					anyadirRutaEstandar(ret, directorioBase)
					def pom = new XmlSlurper().parse(file);
					// Buscar ocurrencias del plugin build-helper-maven-plugin
					pom.build.plugins.plugin.each { plugin ->
						if ("build-helper-maven-plugin".equals(plugin.artifactId.text())) {
							// ¿Define directorios?
							plugin.executions.execution.each { execution ->
								if ("add-test-source".equals(execution.id.text())) {
									execution.configuration.sources.source.each { source ->
										String s = source.text();
										// Partir la ruta para componerla con el separador adecuado
										anyadirRuta(ret, s, directorioBase)
									}
								}
							}
						}
					}
				}
				else if ("gradle".equals(technology)) {
					// Si define algo, se toman esos directorios
					// Si no, el directorio por defecto
					List<String> dirs = GradleBuildParser.parseTestSourceSetsGradle(file.text)
					if (dirs != null && dirs.size() == 0) {
						anyadirRutaEstandar(ret, directorioBase)
					}
					else {
						dirs.each { String dir ->
							anyadirRuta(ret, dir, directorioBase)
						}
					}
				}
 			}
		}
		return ret;
	}
	
	/**
	 * Busca fuentes java en los directorios de tests unitarios
	 * @param unitTestFolders Lista de directorios en los que mirar
	 * @return Lista de ficheros java correspondientes a pruebas unitarias
	 */
	private static List<File> findUnitTestSources(List<File> unitTestFolders) {
		List<File> ret = new LinkedList<String>()
		// Para cada directorio, busca ficheros java presentes en los mismos
		if (unitTestFolders != null && unitTestFolders.size() > 0) {
			unitTestFolders.each { folder ->
				folder.traverse(
					type: groovy.io.FileType.FILES,
					preDir: { if (it.name.startsWith(".") || it.name == 'target') return FileVisitResult.SKIP_SUBTREE},
					nameFilter: ~/.*\.java/
				){
					ret << it
				}
			}
		}
		
		return ret;
	}
} 
