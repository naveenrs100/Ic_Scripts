// Este script escribe en su parentworkspace un fichero artifacts.json con el
//	contenido del parámetro artifactsJson, y lo limpia de su invocante

import hudson.model.*
import jenkins.model.*
import java.beans.XMLDecoder

import es.eci.utils.GlobalVars;
import es.eci.utils.ParamsHelper
import es.eci.utils.transfer.FTPClient;

def build = Thread.currentThread().executable;
def resolver = build.buildVariableResolver;

String artifactsJson = build.getEnvironment(null).get("artifactsJson")

if(artifactsJson != null && !artifactsJson.trim().equals("")) {
	String path = build.getEnvironment(null).get("parentWorkspace")
	File workspace = new File(path)
	File artifacts = new File(workspace, "artifacts.json")
	artifacts.text = artifactsJson
	
	def parent = GlobalVars.getParentBuild(build);
	if (parent != null) {
		ParamsHelper.deleteParams(parent, "artifactsJson")
	}
	
	// Subir a ftp el fichero
	def user = build.getEnvironment(null).get("CC_FTP_USER")
	def password = resolver.resolve("CC_FTP_PWD")
	def address = build.getEnvironment(null).get("CC_FTP")
	
	FTPClient client = new FTPClient(user, password, address);
	client.initLogger { println it }
	client.copy(artifacts, "dejar" + path)
	client.flush()
	
	// Asignar un parámetro para facilitar el acceso a la ruta FTP
	ParamsHelper.addParams(build, ["rutaFTP":path.substring(1)]);
}


