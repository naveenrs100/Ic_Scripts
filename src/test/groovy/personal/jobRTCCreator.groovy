import java.io.File;
import java.util.Map;

import es.eci.utils.jenkins.JobCreator
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def scm = "rtc";
def technology = "Broker";

String streamsJson = new File("C:/OpenDevECI/WSECI_NEON/DIC - Scripts/src/test/groovy/personal/${scm}${technology}JobsExample.json").text;
File destinationDir = new File("C:/Users/dcastro.jimenez/Desktop/jobs");

println "parseando:\n" + streamsJson;

def componentsMap = new JsonSlurper().parseText(streamsJson)

println "json que se debería usar de entrada:\n" + JsonOutput.prettyPrint(JsonOutput.toJson(componentsMap));

JobCreator jc = new JobCreator();

jc.createJenkinsJobs(componentsMap, destinationDir, technology.toLowerCase(), scm.toLowerCase());




