import es.eci.utils.SystemPropertyBuilder;
import git.commands.GitTaggerCommand;
import groovy.json.JsonSlurper


SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

GitTaggerCommand command = new GitTaggerCommand()

command.initLogger { println it }

File packageJson = new File('package.json');
def jsonSlurper = new JsonSlurper();
def object = jsonSlurper.parseText(packageJson.text);

def version = object.version;

propertyBuilder.populate(command);
command.setTag(version);
command.setComment(version);

println("Se va a etiquetar con la versión \"${version}\".")

command.execute();