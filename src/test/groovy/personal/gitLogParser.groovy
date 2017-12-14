import java.awt.geom.Path2D.Iterator;

import es.eci.utils.excel.HelperGenerateReleaseNotes;
import git.commands.GitLogCommand;
import groovy.json.JsonOutput;
import groovy.json.JsonSlurper

def componentName = "PruebaRelease-App-1";
def startTag = "400.0.21.0-SNAPSHOT";
def endTag = "400.0.21.0_1-SNAPSHOT";

GitLogCommand logCommand = new GitLogCommand("30", "C:/jenkins/workspace/test2/${componentName}", startTag, endTag);

String logTxt = logCommand.execute();

println logTxt;

List<String> lineas = [];
logTxt.eachLine { String line ->	
	lineas.add(line);
}

List<List<String>> listaBloques = [];

for(int i=0; i < lineas.size(); i++) {	
	if(lineas[i].startsWith("commit")) {
		List<String> thisBloque = [];		
		for(int k=i+1; k < lineas.size(); k++) {
			if(!lineas[k].startsWith("commit")) {
				if(!lineas[k].trim().equals("")) {
					thisBloque.add(lineas[k].trim());
				}				
			} else {
				break;
			}			
		}
		listaBloques.add(thisBloque);
	}
}

Map superComponentsMap = [:];
List compoList = [];

 Map componentMap = [:];
 componentMap.put("name", componentName);
 componentMap.put("scm", "git");

 List changesList = [];
 
listaBloques.each { List<String> changeSet ->
	Map changeSetMap = [:];
	def author = "";
	def email = "";
	def date = "";
	// Cada bloque del listaBloques es realmente un changeset
	def comment = changeSet[changeSet.size() -1];
	changeSetMap.put("Comentario", comment);
	changeSet.each { String line ->
		if(line.startsWith("Author")) {
			author = line.split("Author: ")[1].split(" <")[0];
			changeSetMap.put("Autor", author);
			email = line.split("<")[1].split(">")[0];
			changeSetMap.put("Email", email);
		}
		if(line.startsWith("Date:")) {
			date = line.split("Date: ")[1];
			changeSetMap.put("Fecha", date);
		}
	}	
	changesList.add(changeSetMap);	
}


componentMap.put("changeset", changesList);

compoList.add(componentMap);
superComponentsMap.put("components",compoList);

String jsonFinal = JsonOutput.toJson(superComponentsMap);

println JsonOutput.prettyPrint(jsonFinal);

HelperGenerateReleaseNotes.writeExcelReport(jsonFinal, "changeLogGit.xls");


