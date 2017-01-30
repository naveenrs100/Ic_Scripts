import es.eci.utils.ParamsHelper;

def build = Thread.currentThread().executable
def resolver = build.buildVariableResolver
def homeStream = resolver.resolve("homeStream")
String artifacts = new File(homeStream + '/artifacts.json').text;
ParamsHelper.addParams(build, ['artifactsList':artifacts])
