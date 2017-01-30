import groovy.net.xmlrpc.*
import java.util.Hashtable
/**
 * Creaci�n din�mica de un proyecto en Checking
 * args[0] - Nombre del proyecto.
 * args[1] - Modelo
 * args[2] - url Repositorio RPC
 * args[3] - UserChecking
 * args[4] - PasswordChecking
 * args[5] - workspace
 * args[6] - componente
 * args[7] - plantilla
 * args[8] - ignorar
  */

if (args[8].equals("no")) {
  def proxyLogin = new XMLRPCServerProxy(args[2]+ "/xmlrpc/login");
  def proxyProject  = new XMLRPCServerProxy(args[2]+ "/xmlrpc/project");

  String login = proxyLogin.login.login(args[3],args[4]);
  println login

  def ret= proxyProject.project.load( login , args[7])

  def mapResult = new Hashtable()

  def creado
  def existe = proxyProject.project.exists( login , args[0])
  println "El proyecto ${args[0]} existe ?: $existe"
  if (existe == true) {
    def borrado = proxyProject.project.delete( login , args[0], true)
    println "Proyecto borrado con: $borrado"
  }

  mapResult.put("path","\$" + "{CHECKING_SRC}" + args[0] )
  mapResult.put("relativeName", args[0])
  mapResult.put("qModel",args[1])
  mapResult.put("description","Proyecto creado desde Jenkins")
  mapResult.put("scm",ret.get("scm"))
  mapResult.get("scm").put("scmPath",args[5] + "' '" +  args[6])
  mapResult.put("buildInfo",ret.get("buildInfo"))
  mapResult.put("type",ret.get("type"))
  println mapResult
  creado = proxyProject.project.create( login , args[0], mapResult)
  println "Proyecto creado con: $creado"
}

