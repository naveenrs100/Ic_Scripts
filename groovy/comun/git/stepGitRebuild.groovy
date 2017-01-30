import es.eci.utils.GlobalVars
import es.eci.utils.SystemPropertyBuilder;
import git.commands.GitLogCommand;


def build = Thread.currentThread().executable;
def resolver = build.buildVariableResolver;

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

GitLogCommand command = new GitLogCommand();
command.setResultsNumber(resolver.resolve("resultsNumber"));
command.setParentWorkspace(resolver.resolve("parentWorkspace"));

def gitLog = command.execute();

def authorMail;
gitLog.eachLine { line ->
	if(line.startsWith("Author")) {
		authorMail = line.substring(line.indexOf("<") + 1, line.indexOf(">"));
	}
}

println("Mail del autor: ${authorMail}");

GlobalVars gVars = new GlobalVars(build);
if(authorMail.equals("david.castrojimenez@gexterno.es")) {
	// No se hace rebuild.
	gVars.put("rebuild", "false");	
} else {
	// Se hace rebuild.
	gVars.put("rebuild", "true");
}





