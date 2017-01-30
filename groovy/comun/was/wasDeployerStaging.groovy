import es.eci.utils.SFTPUtility

/**
 * Script para invocar el upload de la clase SRTPUtility
 */
// Parámetros
int argCounter = 0;
String hostName = args[argCounter++]
String userName = args[argCounter++]
String password= args[argCounter++]
String localFilePath = args[argCounter++]
String remoteFilePath = args[argCounter++]

// 1- Descargar el artefacto a desplegar de Nexus
SFTPUtility.upload(
	hostName, 
	userName, 
	password, 
	localFilePath, 
	remoteFilePath);