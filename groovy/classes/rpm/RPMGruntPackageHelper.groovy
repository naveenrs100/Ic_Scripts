package rpm

import java.nio.file.Files
import java.nio.file.StandardCopyOption;

import es.eci.utils.NexusHelper;
import es.eci.utils.ParameterValidator
import es.eci.utils.TmpDir
import es.eci.utils.ZipHelper;
import es.eci.utils.base.Loggable
import es.eci.utils.commandline.CommandLineHelper;

/**
 * Esta clase baja una configuración de empaquetado de Nexus, la utiliza para
 * generar un RPM y lo deja en el directorio de destino indicado.
 */
class RPMGruntPackageHelper extends Loggable {

	//---------------------------------------------------------------
	// Propiedades de la clase
	
	// Directorio fuente
	private File sourceDirectory;
	// Directorio de instalación (ruta interna en el RPM de los ficheros)
	private String installationDirectory;
	// Directorio de destino para dejar el RPM
	private File destDirectory;
	// Nombre de paquete
	private String packageName;
	// Arquitectura
	private String arch;
	// Versión de empaquetado
	private String packageVersion;
	
	// Coordenadas de la configuración de empaquetado
	
	// URL de Nexus
	private String nexusURL;
	// G
	private String configGroupId;
	// A
	private String configArtifactId;
	// V
	private String configVersion; 
	
	//---------------------------------------------------------------
	// Métodos de la clase
	
	// Constructor privado y vacío
	private RPMGruntPackageHelper() {
		
	}
	
	// Builder de las opciones de construcción
	public static class RPMGruntPackageHelperBuilder {
		private RPMGruntPackageHelper helper;
		// Construye un builder con una instancia vacía del helper
		private RPMGruntPackageHelperBuilder() {
			helper = new RPMGruntPackageHelper();
		}
		// Setter de directorio base
		public RPMGruntPackageHelperBuilder setSourceDirectory(File sourceDirectory) {
			helper.sourceDirectory = sourceDirectory;
			return this;
		}
		// Setter de directorio destino
		public RPMGruntPackageHelperBuilder setDestDirectory(File destDirectory) {
			helper.destDirectory = destDirectory;
			return this;
		}
		// Setter de directorio de instalación
		public RPMGruntPackageHelperBuilder setInstallationDirectory(String installationDirectory) {
			helper.installationDirectory = installationDirectory;
			return this;
		}
		// Setter de nombre de paquete
		public RPMGruntPackageHelperBuilder setPackageName(String packageName) {
			helper.packageName = packageName;
			return this;
		}
		// Setter de arquitectura
		public RPMGruntPackageHelperBuilder setArch(String arch) {
			helper.arch = arch;
			return this;
		}
		// Setter de URL de Nexus
		public RPMGruntPackageHelperBuilder setNexusURL(String nexusURL) {
			helper.nexusURL = nexusURL;
			return this;
		}
		// Setter de versión de empaquetado
		public RPMGruntPackageHelperBuilder setPackageVersion(String packageVersion) {
			helper.packageVersion = packageVersion;
			return this;
		}
		
		// GAV
		
		// groupId
		public RPMGruntPackageHelperBuilder setConfigGroupId(String configGroupId) {
			helper.configGroupId = configGroupId;
			return this;
		}
		// artifactId
		public RPMGruntPackageHelperBuilder setConfigArtifactId(String configArtifactId) {
			helper.configArtifactId = configArtifactId;
			return this;
		}
		// versión
		public RPMGruntPackageHelperBuilder setConfigVersion(String configVersion) {
			helper.configVersion = configVersion;
			return this;
		}
		
		// Construye la instancia
		public RPMGruntPackageHelper build(Closure printer = null) {
			// Validar la corrección
			// Validación de obligatorios
			ParameterValidator.builder()
					.add("sourceDirectory", helper.sourceDirectory)
					.add("destDirectory", helper.destDirectory)
					.add("installationDirectory", helper.installationDirectory)
					.add("packageName", helper.sourceDirectory)
					.add("packageVersion", helper.packageVersion)
					.add("configGroupId", helper.configGroupId)
					.add("configArtifactId", helper.configArtifactId)
					.add("configVersion", helper.configVersion)
					.add("arch", helper.arch)
					.add("nexusURL", helper.nexusURL)
				.build().validate();
			// Arrancar el log y devolver el helper
			helper.initLogger(printer)
			return helper;
		}
	}
	
	// Instancia del builder
	public static RPMGruntPackageHelperBuilder builder() {
		return new RPMGruntPackageHelperBuilder();
	}
	
	// Encuentra el RPM en el directorio indicado
	private File lookForRPM(File dir) {
		File ret = null;
		File[] files = dir.listFiles();
		int i = 0;
		while (i < files.length && ret == null) {
			if (files[i].getName().toLowerCase().endsWith(".rpm")) {
				ret = files[i];
			}
			i++;
		}
		return ret;
	}
	
	// Construir el RPM
	public void createPackage() {
		log("Construyendo el paquete con versión ${packageVersion}");
		// Se debe bajar la configuración sobre un directorio temporal
		TmpDir.tmp { File dir ->
			// Bajar y descomprimir la configuración
			NexusHelper.downloadLibraries(
				configGroupId, 
				configArtifactId, 
				configVersion, 
				dir.getCanonicalPath(), 
				"zip", 
				nexusURL);
			// Fichero zip recién descargado
			ZipHelper.unzipFile(dir.listFiles()[0], dir);
			// Resolver las dependencias de npm
			CommandLineHelper npmCommand = new CommandLineHelper("npm install");
			npmCommand.initLogger(this);
			npmCommand.execute(dir);
			// Construir el entregable RPM
			// RPM no acepta guiones
			String cleanVersion = packageVersion.replaceAll("\\-", "_");
			CommandLineHelper rpmCommand = 
				new CommandLineHelper(
					"grunt package \"--src=${sourceDirectory}\" --arch=${arch} \"--dest=${installationDirectory}\" --icversion=${cleanVersion} --pkg-name=${packageName}");
			rpmCommand.initLogger(this);
			rpmCommand.execute(dir);
			// Copiar el rpm al directorio de destino
			File rpm = lookForRPM(dir);
			if (!destDirectory.exists()) {
				destDirectory.mkdirs();
			}
			File destRPM = new File(destDirectory, rpm.getName());
			if (destRPM != null) {
				log ("Copiando de vuelta ${rpm} a ${destRPM}");
				CommandLineHelper stats = new CommandLineHelper("rpm -qip ${rpm}")
				stats.initLogger(this);
				stats.execute();
				Files.copy(rpm.toPath(), destRPM.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			else {
				throw new Exception("¡¡¡No hay fichero RPM!!!")
			}
		}		
	}
}
