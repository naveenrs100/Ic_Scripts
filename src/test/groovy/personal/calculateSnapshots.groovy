import groovy.json.JsonSlurper
import groovy.json.JsonOutput

File jsonFile = new File("C:/Users/dcastro.jimenez/Desktop/json.txt");
File outputFile = new File("C:/Users/dcastro.jimenez/Desktop/snapshots.txt");

def jsonObject = new JsonSlurper().parseText(jsonFile.text);

ArrayList snapshotsList = [];

jsonObject.each { object ->
	if(object.version.endsWith("-SNAPSHOT")) {
		Map tmp = [:]
		tmp.put("version", object.version);
		tmp.put("component", object.component);
		tmp.put("groupId", object.groupId);
		tmp.put("artifactId", object.artifactId);
		snapshotsList.add(tmp);
	}
}


outputFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(snapshotsList));

