package rpm

import es.eci.utils.StringUtil
import es.eci.utils.SystemPropertyBuilder
import rpm.MavenRPMPackageHelper

/**
 * Este script crea un RPM mediante el plugin Maven RPM
 *
 * Recibe los parámetros:
 *
 * sourceDirectory
 * destDirectory
 * installationDirectory
 * arch
 * packageName
 * packageVersion
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

// Crear un directorio temporal

def b = MavenRPMPackageHelper.builder();
 
 propertyBuilder.populate(b);
 
 // Falta el packageVersion
 // Las opciones son: parámetro version -> fichero version.txt -> fallar
 
 if (StringUtil.isNull(propertyBuilder.getSystemParameters().get("packageVersion"))) {
	 println "No existe el parámetro param.packageVersion o es nulo"
	 // Fichero version.txt
	 File f = new File("version.txt");
	 if (!f.exists()) {
		 throw new Exception("Versión del RPM desconocida");
	 }
	 else {
		 def config = new ConfigSlurper().parse(f.text)
		 def version = config.version
		 println "Leyendo versión de version.txt -> ${version}"
		 b.setPackageVersion(version);
	 }
 }
 
 MavenRPMPackageHelper helper = b.build( {println it} );
 
 helper.initLogger { println it }
  
 helper.createPackage();