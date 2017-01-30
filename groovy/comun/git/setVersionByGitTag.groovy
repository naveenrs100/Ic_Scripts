import es.eci.utils.ParamsHelper;
import hudson.model.*;
import es.eci.utils.commandline.CommandLineHelper;

def build = Thread.currentThread().executable;
def resolver = build.buildVariableResolver;

def version = resolver.resolve("version");

if(version == null || version.trim().equals("")) {
  def getTgCommand = "/jenkins/buildtools/gitAIX/git.sh for-each-ref --sort=taggerdate --format '%(refname) %(taggerdate)' refs/tags";
  CommandLineHelper buildCommandLineHelper = new CommandLineHelper(getTgCommand);
  println("Lanzando el comando ${getTgCommand} en el directorio ${build.workspace}")
  def returnCode = buildCommandLineHelper.execute(new File(build.workspace.toString()));
  if(returnCode != 0) {
	throw new Exception("Error al ejecutar el comando ${getTgCommand}. Código -> ${returnCode}");
  }
  def salida = buildCommandLineHelper.getStandardOutput();
  def lastTag;
  salida.eachLine {
	def lastTagWithDate = it.split("/")[2];
	lastTag = lastTagWithDate.split(" ")[0];
  }
  
  // Se borra el valor antiguo vacío de "version"
  
  ParamsHelper.deleteParams(build, ["version"].toArray(new String[1]));
  // Se introduce el nuevo valor de "version" calculado a partir de la última tag de Git
  def paramsMap = [:];
  paramsMap.put('version',lastTag);
  ParamsHelper.addParams(build, paramsMap);
}
