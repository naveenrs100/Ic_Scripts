import es.eci.utils.GitBuildFileHelper;
import components.MavenComponent;
import es.eci.utils.pom.SortGroupsStrategy;
import groovy.json.JsonOutput;

def parentWorkspace = "C:/jenkins/workspace/test2";
def action = "build";
def groupComponents = [ "PruebaRelease-App-1",
						"PruebaRelease-App-2",
						"PruebaRelease-Biblioteca-1",
						"PruebaRelease-Biblioteca-2" ]

GitBuildFileHelper gitBuildFileHelper = new GitBuildFileHelper(action,new File(parentWorkspace));
List <MavenComponent> reactor = gitBuildFileHelper.buildArtifactsFile(groupComponents, new File(parentWorkspace))

reactor.each {
	println it.getName();
}

def lib1MavenCompo = reactor.find { it.getName().equals("PruebaRelease-Biblioteca-1") }
def lib2MavenCompo = reactor.find { it.getName().equals("PruebaRelease-Biblioteca-2") }
def app1MavenCompo = reactor.find { it.getName().equals("PruebaRelease-App-1") }
def app2MavenCompo = reactor.find { it.getName().equals("PruebaRelease-App-2") }

println("B2 depende de B1 -> " + MavenComponent.dependsOn(lib2MavenCompo, lib1MavenCompo));
println("A1 depende de B1 -> " + MavenComponent.dependsOn(app1MavenCompo, lib1MavenCompo));
println("A2 depende de B1 -> " + MavenComponent.dependsOn(app2MavenCompo, lib1MavenCompo));
println("A1 depende de B2 -> " + MavenComponent.dependsOn(app1MavenCompo, lib2MavenCompo));
println("A2 depende de B2 -> " + MavenComponent.dependsOn(app2MavenCompo, lib2MavenCompo));
println("A2 depende de A1 -> " + MavenComponent.dependsOn(app2MavenCompo, app1MavenCompo));
println("A1 depende de A2 -> " + MavenComponent.dependsOn(app1MavenCompo, app2MavenCompo));

List<List<MavenComponent>> grupos = new SortGroupsStrategy().sortGroups(reactor);
List<List<String>> gruposString = [];
grupos.each { List<MavenComponent> grupo ->
	List<String> grupoString = [];
	grupo.each {		
		grupoString.add(it.getName());
	}
	gruposString.add(grupoString);
}

println(JsonOutput.toJson(gruposString));