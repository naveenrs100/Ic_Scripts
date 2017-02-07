import es.eci.utils.ScmCommand;
import groovy.json.JsonSlurper;

File wksTxt = new File("C:/Users/dcastro.jimenez/Desktop/workspaces.txt");
def scmToolsHome = "C:/OpenDevECI/scmtools/eclipse"
def daemonsConfigDir = "C:/Temp2/daemons"
def workspace = "WSR_G.DOC_Logistica_DESARROLLO_X02540MA";
def listCommand = "list workspaces --maximum 100000 -j";

ScmCommand command = new ScmCommand(true, scmToolsHome, daemonsConfigDir);

try {
	def workspacesJsonText = command.ejecutarComando(listCommand, "JENKINS_RTC", "12345678",
			"https://rtc.elcorteingles.int:9443/ccm", new File(daemonsConfigDir));

	JsonSlurper js = new JsonSlurper()
	def workspacesJson = js.parseText(workspacesJsonText);

	def workspacesDuplicados = [:];
	def workspacesnames = [];
	
	wksTxt.text = "";
	workspacesJson.each {
		if(it.name.startsWith("WSR") && it.name.endsWith("IC")) {
			workspacesnames.add(it.name);
			wksTxt << "${it.name}\n"
		}		
	}

	def passed = [];
	for(a in workspacesnames) {
		if(!passed.contains(a)) {

			def contador = 0;
			for(b in workspacesnames) {
				if(a.equals(b)) {
					contador++;
				}
			}
			if(contador > 1){
				println("${a}: ${contador}")
			}
			passed.add(a);
		}
	}

} finally {
	command.detenerDemonio(new File(daemonsConfigDir));
}

