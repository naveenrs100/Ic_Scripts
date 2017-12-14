package ftp

import es.eci.utils.Utiles
import es.eci.utils.transfer.FileDeployer

/**
 * Funcionalidad de despliegue de ficheros a través de FTP.
 * 
 * Parámetros de entrada:
 * 
 * --- OBLIGATORIOS
 * source Fichero o directorio de origen en la máquina local
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
	FileDeployer deployer = new FileDeployer(user, password, targetAdress);
	deployer.initLogger { println it };
}