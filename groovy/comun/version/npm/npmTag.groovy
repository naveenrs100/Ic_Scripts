package version.npm

import es.eci.utils.SystemPropertyBuilder;
import git.commands.GitTaggerCommand;
import groovy.json.JsonSlurper

// Devuelve el package.json si existe, y si no, 
//	el npm-shrinkwrap.json
def findFile() {
	File ret = null;
	File p = new File("package.json");
	if (p.exists()) {
		ret = p;
	}
	else {
		File s = new File("npm-shrinkwrap.json");
		if (s.exists()) {
			ret = s;
		}
	}
	return ret;
}

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

GitTaggerCommand command = new GitTaggerCommand()

command.initLogger { println it }

File packageJson = findFile();
def jsonSlurper = new JsonSlurper();
def object = jsonSlurper.parseText(packageJson.text);

def version = object.version;

propertyBuilder.populate(command);
command.setTag(version);
command.setComment(version);

println("Se va a etiquetar con la versión \"${version}\".")

command.execute();