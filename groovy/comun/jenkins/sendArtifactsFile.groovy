package jenkins

import es.eci.utils.ParamsHelper;

def homeStream = build.buildVariableResolver.resolve("homeStream")
String artifacts = new File(homeStream + '/artifacts.json').text;
ParamsHelper.addParams(build, ['artifactsList':artifacts])
