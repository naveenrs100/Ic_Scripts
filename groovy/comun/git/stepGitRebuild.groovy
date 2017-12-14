package git

import es.eci.utils.GlobalVars
import es.eci.utils.SystemPropertyBuilder;
import git.commands.GitLogCommand;

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

GitLogCommand command = new GitLogCommand();
command.setResultsNumber(build.buildVariableResolver.resolve("resultsNumber"));
command.setParentWorkspace(build.buildVariableResolver.resolve("parentWorkspace"));

def gitLog = command.execute();

def authorMail;
gitLog.eachLine { line ->
	if(line.startsWith("Author")) {
		authorMail = line.substring(line.indexOf("<") + 1, line.indexOf(">"));
	}
}

println("Mail del autor: ${authorMail}");

GlobalVars gVars = new GlobalVars();
if(authorMail.equals("david.castrojimenez@gexterno.es")) {
	// No se hace rebuild.
	gVars.put(build, "rebuild", "false");	
} else {
	// Se hace rebuild.
	gVars.put(build, "rebuild", "true");
}





