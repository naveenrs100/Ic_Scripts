import es.eci.utils.ScmCommand;
import groovy.json.JsonSlurper
import es.eci.utils.*;

String baselinesTxt = new File("C:/Users/dcastro.jimenez/Desktop/baselines.txt").text;

println(getLineaBaseInicial(baselinesTxt));

private String getLineaBaseInicial(String baselinesTxt) {
	String lastBaseline = null;
	
	TmpDir.tmp { File baseDir ->
		
		JsonSlurper slurper = new JsonSlurper();
		def listObject = slurper.parseText(baselinesTxt);
		
		listObject[0].baselines.each { baseline ->
			lastBaseline = baseline.name;
		}
	}
	
	
	
	return lastBaseline;
}