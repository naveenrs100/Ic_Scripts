import groovy.json.*;
import java.beans.*;

List lista1 = ["CC.CC A1","CC.CC A2","CC.CC A3"]
List lista = [];
lista.add(lista1);

def json = JsonOutput.toJson(lista);
println json

def jsonObject = new ().parseText(json);



//InputStream stream = new ByteArrayInputStream(json.getBytes());
//XMLDecoder d = new XMLDecoder(stream);
//Object result = d.readObject();
//d.close();
//
//result.each {
//	println it
//}