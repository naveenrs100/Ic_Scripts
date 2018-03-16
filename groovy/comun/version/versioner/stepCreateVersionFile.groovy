package version.versioner

//@Grab(group='com.ibm.icu', module='icu4j', version='57.1')

import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.versioner.PomXmlOperations

SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
def params = parameterBuilder.getSystemParameters();

def parentWorkspace = params["parentWorkspace"];

def componentDir = new File("${parentWorkspace}");

PomXmlOperations.createVersionFile(componentDir);