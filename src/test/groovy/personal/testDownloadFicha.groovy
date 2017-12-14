import testing.commands.RunTestsCommand;

def aplicacion = "GIS - Cluster Servicios";
def instantanea = "SKSM0005_1.2.1.0";
def nexusUrl = "http://nexus.elcorteingles.int";


RunTestsCommand command = new RunTestsCommand(aplicacion, instantanea, nexusUrl);

command.execute();
