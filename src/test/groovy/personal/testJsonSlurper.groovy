import groovy.json.JsonSlurper;

String jobs = "[[\"GIS - Proyecto Prueba Release - DESARROLLO -COMP- PruebaRelease - Biblioteca 1\"],[\"GIS - Proyecto Prueba Release - DESARROLLO -COMP- PruebaRelease - Biblioteca 2\"],[\"GIS - Proyecto Prueba Release - DESARROLLO -COMP- PruebaRelease - App 2\",\"GIS - Proyecto Prueba Release - DESARROLLO -COMP- PruebaRelease - App 1\"]]";

def newJobs = jobs.substring(0,jobs.size()-1);
println(newJobs); 

List<List<String>> jobsObject = (List<List<String>>)(new JsonSlurper().parseText(jobs));

List<List<String>> jobsList = new ArrayList<List<String>>();

jobsList.each { ArrayList it ->
	println(it);
	ArrayList<String> tmpList = new ArrayList<String>();
	for(String jobX : it) {
		println(jobX);
		tmpList.add(jobX);
	}
	jobsList.add(tmpList);
}

println(jobsList);