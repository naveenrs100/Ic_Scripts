package tpv

import es.eci.utils.Utiles
import es.eci.utils.VersionUtils;
import es.eci.utils.transfer.FileDeployer

/**
 * Funcionalidad de despliegue de ficheros a través de FTP.  Este script se diferencia
 * de stepDeployFiles en que saca primero la versión del TPV del pom.xml.
 * 
 * Parámetros de entrada:
 * 
 * --- OBLIGATORIOS
 * source Directorio de origen del componente TPV
 * xsdPath Ruta relativa del directorio público de XSD (relativa a source)
 * targetDirectory Directorio de destino en la máquina remota
 * targetAddress URL de la máquina de destino
 * user Usuario en la máquina remota
 * password Clave del usuario en la máquina remota
 * 
 * --- OPCIONALES
 * includes Si viene vacío, se copia el directorio completo; si viene informado,
 * 	solo los ficheros que cumplan con el patrón (debe ser una expresión regular
 * 	válida)
 */

// 5 obligatorios
Utiles.validate(args, 5);
 
//---------------> Parámetros obligatorios
int argCounter = 0;
File source = Utiles.toFile(args[argCounter++]);
String xsdPath = args[argCounter++];
String targetDirectory = args[argCounter++];
String targetAdress = args[argCounter++];
String user = args[argCounter++];
String password = args[argCounter++];

//---------------> Parámetros opcionales
String includes = Utiles.readOptionalParameter(args, argCounter++);

if (source == null || !source.exists()) {
	throw new Exception("ERROR: el directorio de origen no existe");
}
else {
	// Lectura de la versión del pom.xml
	def pom = new XmlSlurper().parse(new File(source.getCanonicalPath() + "/pom.xml"));
	String tpvVersion = new VersionUtils().solve(pom, "\${tpv-version}");
	
	println "Despliegue de XSD:: versión de TPV: $tpvVersion";
	
	targetDirectory = targetDirectory + "/" + tpvVersion;
	
	println "Despliegue de XSD:: directorio de destino: $targetDirectory";
	
	FileDeployer deployer = new FileDeployer(user, password, targetAdress);
	deployer.initLogger { println it };
	
	File sourceDirectory = new File(source.getCanonicalPath() + "/" + xsdPath);
	deployer.deploy(sourceDirectory, targetDirectory);
}