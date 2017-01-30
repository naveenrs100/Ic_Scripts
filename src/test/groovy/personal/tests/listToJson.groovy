import groovy.json.*;

List<String> lista1 = ["aaaa","bbbb","cccc"];
List<String> lista2 = ["11111","22222","3333"];
List<String> lista3 = ["dddd","eeee","fffff","ggggg"];

List<List<String>> lista = [];
lista.add(lista1);
lista.add(lista2);
lista.add(lista3);

def json = JsonOutput.toJson(lista);
def json3 = JsonOutput.toJson(lista3);

println(json + "\n");
println(json3 + "\n")

JsonSlurper js = new JsonSlurper();
def parsedJson = js.parseText(json);
def parsedJson3 = js.parseText(json3);

parsedJson.each{
	println it
}

parsedJson3.each {
	println it
}


println("parsedJson size: ${parsedJson.size()}")