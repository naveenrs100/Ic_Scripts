import groovy.json.JsonOutput;

def map = [:];
def map1 = [:];
map1.put("sssss","ssssss1");
map1.put("wwwwww","wwwwww1");

map.put("name","David");
map.put("versions", map1);

def jsonString = JsonOutput.prettyPrint(JsonOutput.toJson(map));

println jsonString