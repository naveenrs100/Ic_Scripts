package rpm

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

import es.eci.utils.ParameterValidator
import es.eci.utils.TmpDir
import es.eci.utils.base.Loggable
import es.eci.utils.commandline.CommandLineHelper
import rpm.MavenRPMPackageHelper.MavenRPMPackageHelperBuilder

/**
 * Esta clase agrupa la funcionalidad para construir RPM mediante
 * el plugin RPM de maven
 */
class MavenRPMPackageHelper extends Loggable {

	//-------------------------------------------------------------------------
	// Constantes de la clase
	
	private static final String POM_CONTENT = 
'''
<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>es.eci</groupId>
  <artifactId>${eci.rpm.packageName}</artifactId>
  <version>${eci.rpm.packageVersion}</version>
  <packaging>jar</packaging>
  <properties>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
		<build>
				<plugins>
						<plugin>
								<groupId>org.codehaus.mojo</groupId>
								<artifactId>rpm-maven-plugin</artifactId>
								<version>2.1.5</version>
								<executions>
								  <execution>
										<id>generate-rpm</id>
										<goals>
										  <goal>rpm</goal>
										</goals>
								  </execution>
								</executions>
								<configuration>
										<needarch>${eci.rpm.arch}</needarch>
										<release>1</release>
										<group>ECI</group>
										<defineStatements>
												<defineStatement>_unpackaged_files_terminate_build 0</defineStatement>
										</defineStatements>
										<mappings>
											<mapping>
											  <directory>${eci.rpm.installationDirectory}</directory>
											  <filemode>750</filemode>
											  <recurseDirectories>true</recurseDirectories>
											  <sources>
													<source>
													  <location>${eci.rpm.sourceDirectory}</location>
													</source>
											  </sources>
											</mapping>
										</mappings>
								</configuration>
						</plugin>
				</plugins>
		</build>
</project>
'''
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
	
	//---------------------------------------------------------------
	// Métodos de la clase
	
	// Constructor privado y vacío
	private MavenRPMPackageHelper() {
		
	}
	
	// Builder de las opciones de construcción
	public static class MavenRPMPackageHelperBuilder {
		private MavenRPMPackageHelper helper;
		// Construye un builder con una instancia vacía del helper
		private MavenRPMPackageHelperBuilder() {
			helper = new MavenRPMPackageHelper();
		}
		// Setter de directorio base
		public MavenRPMPackageHelperBuilder setSourceDirectory(File sourceDirectory) {
			helper.sourceDirectory = sourceDirectory;
			return this;
		}
		// Setter de directorio destino
		public MavenRPMPackageHelperBuilder setDestDirectory(File destDirectory) {
			helper.destDirectory = destDirectory;
			return this;
		}
		// Setter de directorio de instalación
		public MavenRPMPackageHelperBuilder setInstallationDirectory(String installationDirectory) {
			helper.installationDirectory = installationDirectory;
			return this;
		}
		// Setter de nombre de paquete
		public MavenRPMPackageHelperBuilder setPackageName(String packageName) {
			helper.packageName = packageName;
			return this;
		}
		// Setter de arquitectura
		public MavenRPMPackageHelperBuilder setArch(String arch) {
			helper.arch = arch;
			return this;
		}
		// Setter de versión de empaquetado
		public MavenRPMPackageHelperBuilder setPackageVersion(String packageVersion) {
			helper.packageVersion = packageVersion;
			return this;
		}
		
		// Construye la instancia
		public MavenRPMPackageHelper build(Closure printer = null) {
			// Validar la corrección
			// Validación de obligatorios
			ParameterValidator.builder()
					.add("sourceDirectory", helper.sourceDirectory)
					.add("destDirectory", helper.destDirectory)
					.add("installationDirectory", helper.installationDirectory)
					.add("packageName", helper.sourceDirectory)
					.add("packageVersion", helper.packageVersion)
					.add("arch", helper.arch)
				.build().validate();
			// Arrancar el log y devolver el helper
			helper.initLogger(printer)
			return helper;
		}
	}
	
	// Instancia del builder
	public static MavenRPMPackageHelperBuilder builder() {
		return new MavenRPMPackageHelperBuilder();
	}
	
	// Encuentra el RPM en el directorio indicado
	private File lookForRPM(File dir) {
		File ret = null;
		File[] files = new File(dir, "target/rpm/${packageName}/RPMS/${arch}").listFiles();
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
			File pom = new File(dir, "pom.xml");
			pom.text = POM_CONTENT
			CommandLineHelper helper = new CommandLineHelper(
				"mvn "
				+ "-Deci.rpm.packageVersion=${this.packageVersion} "
				+ "-Deci.rpm.sourceDirectory=${this.sourceDirectory} "
				+ "-Deci.rpm.installationDirectory=${this.installationDirectory} "
				+ "-Deci.rpm.packageName=${this.packageName} "
				+ "-Deci.rpm.arch=${this.arch} "
				+ "rpm:rpm");
			helper.initLogger(this) 
			helper.execute(dir)
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
