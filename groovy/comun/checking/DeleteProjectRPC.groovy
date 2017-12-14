package checking

//$JENKINS_HOME/jobs/ScriptsStore/workspace/workFlowChecking/DeleteProjectRPC.groovy
import groovy.net.xmlrpc.*
import java.util.Hashtable  
/**
 * Borrado din�mico de un proyecto en Checking 
 * args[0] - Nombre del proyecto.
 * args[1] - Modelo
 * args[2] - url Repositorio RPC
 * args[3] - UserChecking
 * args[4] - PasswordChecking
 * args[5] - ignorar 
 */  
if (args[5].equals("no")) {
  def proxyLogin = new XMLRPCServerProxy(args[2]+ "/xmlrpc/login");
  def proxyProject  = new XMLRPCServerProxy(args[2]+ "/xmlrpc/project");

  String login = proxyLogin.login.login(args[3],args[4]);
  println login

  def borrado = proxyProject.project.delete( login , args[0], true)
  println "Proyecto borrado  con: $borrado"
}