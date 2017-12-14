package rpm

import rpm.RPMGruntPackageHelper
import es.eci.utils.SystemPropertyBuilder

/**
 * Este script crea un RPM a partir de una configuración subida a nexus (gruntfile.js, etc.)
 * 
 * Recibe los parámetros:
 * 
 * configGroupId
 * configArtifactId
 * configVersion
 * sourceDirectory
 * destDirectory
 * installationDirectory
 * arch
 * packageName
 * packageVersion
 * nexusURL
 * 
 */

// ¿Cadena vacía?
 def isNull(s) {
	 return s == null || s.trim().length() == 0;
 }

 SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
 
 def b = RPMGruntPackageHelper.builder();
 
 propertyBuilder.populate(b);
 
 // Falta el packageVersion
 // Las opciones son: parámetro version -> fichero version.txt -> fallar
 
 if (isNull(propertyBuilder.getSystemParameters().get("packageVersion"))) {
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
 
 RPMGruntPackageHelper helper = b.build( {println it} );
 
 helper.initLogger { println it }
  
 helper.createPackage();