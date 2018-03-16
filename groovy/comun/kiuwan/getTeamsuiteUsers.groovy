package kiuwan

/**
 * 
 * Se invoca como Groovy Script.
 * 
 * Recoge del servidor de teamsuite el fichero de usuarios y permisos
 * sobre proyectos, en formato json. Este fichero debe después mezclarse
 * con el de RTC y enviarse a kiuwan.com.
 * 
 * Parámetros de entrada:
 * 
 * server: Servidor de teamsuite
 * remoteFile: Ruta del fichero en el servidor de teamsuite
 * username: Usuario de acceso al servidor de teamsuite
 * password: Password de acceso al servidor de teamsuite
 * fileName: Nombre de fichero a guardar en local
 * parentWorkspace: directorio de ejecución
 * 
 */

@Grab(group='commons-net', module='commons-net', version='3.3')

import groovy.json.*
import es.eci.utils.*
import org.apache.commons.net.ftp.*

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

def params = propertyBuilder.getSystemParameters()

def username = params.get("username")
def password = params.get("password")
def server = params.get("server")
def fileName = params.get("fileName")
def parentWorkspace = params.get("parentWorkspace")
def remoteFile = params.get("remoteFile")

ParameterValidator.builder().
	add("username", username).
	add("password", password).
	add("server", server).
	add("fileName", fileName).
	add("parentWorkspace", parentWorkspace).
	add("remoteFile", remoteFile).
	build().validate();
	
fileName = "${parentWorkspace}/${fileName}"

new FTPClient().with {
  connect server
  
  enterLocalPassiveMode()
  login username, password
  
  //changeWorkingDirectory "directory the file is in"
  
  fileType = FTPClient.BINARY_FILE_TYPE
  def incomingFile = new File(fileName)
  incomingFile.withOutputStream { ostream -> retrieveFile remoteFile, ostream }
  
  disconnect()
}