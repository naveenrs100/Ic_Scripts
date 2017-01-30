import java.io.File;
import java.util.List;
import components.MavenComponent;
import es.eci.utils.ScmCommand;
import es.eci.utils.TmpDir;
import groovy.json.JsonSlurper;
import es.eci.utils.RTCBuildFileHelper;

def light = true;
def scmToolsHome = "C:/OpenDevECI/scmtools/eclipse";
String daemonsConfigDir = "C:/OpenDevECI/scmtools/daemons_home"
def typeOrigin = "workspace"
def nameOrigin = "WSR - GIS - Proyecto Prueba Release - DESARROLLO - BUILD - IC";
def typeTarget = "stream";
def nameTarget = "GIS - Proyecto Prueba Release - DESARROLLO";

def userRTC = "JENKINS_RTC"
def pwdRTC = "12345678"
def urlRTC = "https://rtc.elcorteingles.pre:59443/ccm"

def fileOut = new File("C:/Users/dcastro.jimenez/Desktop/componentsCompare.txt");

TmpDir.tmp { File dir ->
	def parentWorkspace = dir;
	ScmCommand command = new ScmCommand(light, scmToolsHome, daemonsConfigDir);
	try {		
		def ret = command.ejecutarComando("compare ${typeOrigin} \"${nameOrigin}\" ${typeTarget} \"${nameTarget}\" -f i -I dcbsw -C \"|{name}|{email}|\" -D \"|yyyy-MM-dd-HH:mm:ss|\" -j ", userRTC, pwdRTC, urlRTC, parentWorkspace);
		println(ret)
	} catch (Exception e) {
//		def ret = command.ejecutarComando("list components \"${nameTarget}\" ", userRTC, pwdRTC, urlRTC, parentWorkspace);
//		println(ret)
	}
	
//	def retJson = new JsonSlurper().parseText(ret);
//
//	if(retJson.workspaces == null) {
//		retJson.direction[0].components.each { component ->
//			if(component.changesets != null || component.added == "true") {
//				println("El componente ${component.name} tiene cambios. Se añade al componentsCompare.txt")
//				fileOut.append(component.name + "\n");
//			}
//		}
//	}
}


RTCBuildFileHelper helper = new RTCBuildFileHelper("build", new File("C:/Users/dcastro.jimenez/Desktop/Pruebas"));
File baseDirectory = new File("C:/Users/dcastro.jimenez/Desktop/Pruebas");

List changedComponentsList = ["PruebaRelease - Biblioteca 1"];

List<MavenComponent> dependencyGraph = helper.buildDependencyGraph(changedComponentsList, baseDirectory);

//def origin = dependencyGraph.find { it.getName().equals("PruebaRelease - App 1") }
//def dep = dependencyGraph.find { it.getName().equals("PruebaRelease - Biblioteca 1") }
//boolean depends = MavenComponent.dependsOn(origin, dep);
//println(depends);

def additionalComponents = []
dependencyGraph.each { MavenComponent mavenComponent ->
	changedComponentsList.each { String component ->
		MavenComponent thisMavenComponent = dependencyGraph.find { it.getName().equals(component) };
		if(MavenComponent.dependsOn(mavenComponent, thisMavenComponent)) {
			if(!changedComponentsList.contains(mavenComponent.getName())) {
				additionalComponents.add(mavenComponent.getName());
			}
		}
	}
}

changedComponentsList.addAll(additionalComponents);
println("changedComponentsList");
changedComponentsList.each {
	println it
}









