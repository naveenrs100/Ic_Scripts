package rpm

import static groovy.io.FileType.*
import static groovy.io.FileVisitResult.*
import es.eci.utils.TmpDir
import es.eci.utils.base.Loggable
import es.eci.utils.commandline.CommandLineHelper

/**
 * Esta clase encapsula la funcionalidad para construir un RPM a partir de un
 * directorio.
 * 
 * Incorpora opcionalmente unas exclusiones.
 */
public class RPMPackageHelper extends Loggable {

	//-----------------------------------------------------------
	// Propiedades de la clase
	
	// Directorio a empaquetar
	private File sourceDirectory;
	// Directorio donde se deja el RPM destino
	private File 
	// Directorio base de la instalación
	private String installationDirectory;
	// Lista de exclusiones
	private List<File> exclusions;
	
	
	//-----------------------------------------------------------
	// Métodos de la clase
		
	
	// Se deja privado
	private RPMPackageHelper() {
		exclusions = new LinkedList<File>();
	}
	
	// Builder de las opciones de construcción
	public static class RPMPackageHelperBuilder {
		private RPMPackageHelper helper;
		// Construye un builder con una instancia vacía del helper
		private RPMPackageHelperBuilder() {
			helper = new RPMPackageHelper();
		}
		// Setter de directorio base
		public RPMPackageHelperBuilder setSourceDirectory(File sourceDirectory) {
			helper.sourceDirectory = sourceDirectory;
			return this;
		}
		// Setter de directorio destino
		public RPMPackageHelperBuilder setInstallationDirectory(String installationDirectory) {
			helper.installationDirectory = installationDirectory;
			return this;
		}
		// Setter de exclusiones
		public RPMPackageHelperBuilder addExclusion(File exclusion) {
			helper.exclusions.add(exclusion);
			return this;
		}
		public RPMPackageHelper build(Closure printer = null) {
			helper.initLogger(printer)
			return helper;
		}
	}
	
	// Instancia del builder
	public static RPMPackageHelperBuilder builder() {
		return new RPMPackageHelperBuilder();
	}
	
	// Copia un directorio a otro
	private void copyLargeDir(File dirFrom, File dirTo, List<File> exclusions){
	    // creation the target dir
	    if (!dirTo.exists()){
	        dirTo.mkdir();
	    }
	    // copying the daughter files
	    dirFrom.eachFile(FILES){File source ->
			if (!exclusions.contains(source)) {
		        File target = new File(dirTo,source.getName());
		        target.bytes = source.bytes;
			}
	    }
	    // copying the daughter dirs - recursion
	    dirFrom.eachFile(DIRECTORIES){File source ->
	        File target = new File(dirTo,source.getName());
	        copyLargeDir(source, target)
	    }
	}
	
	/**
	 * Crea el paquete RPM
	 */
	public void createPackage() {
		TmpDir.tmp { File dir ->		
			// Directorios temporales para rpm
			File sources = new File(dir, "SOURCES");
			sources.mkdir();
			File specs = new File(dir, "SPECS");
			specs.mkdir()
			File build = new File(dir, "BUILD");
			build.mkdir()
			File rpms = new File(dir, "RPMS");
			rpms.mkdir();
			File srpms = new File(dir, "SRPMS");
			srpms.mkdir();
			// Copiar todo a SOURCES
			copyLargeDir(sourceDirectory, sources, exclusions);		
			// Fichero specs
			File specFile = new File(specs, "rpm.spec");
			specFile.createNewFile();
			specFile.text = 
'''Summary: rpm package
Name: package
Version: 1.0
Release: 1
License: GPL
Group: Applications/Sound
Distribution: WSS Linux
Vendor: White Socks Software, Inc.
Packager: Santa Claus <sclaus@northpole.com>

%description
Empaquetado con groovy''';
			CommandLineHelper helper = new CommandLineHelper("rpmbuild -ba rpm.spec");
			helper.initLogger(this);
			helper.execute(specs);

		}
	}	
}
