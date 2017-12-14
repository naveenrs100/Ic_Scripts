//def jobs = params["jobs"].split("\n");
//def count = 0;
//
//jobs.each { String job ->
//	pipeline {
//		def jobName = job.split("\\?")[0]
//		def params = job.split("\\?")[1]
//		def paramList = []
//		params.split("&").each { String param ->
//			def paramName = param.split("=")[0]
//			def paramValue = param.split("=")[1]
//			paramList.add(string(name: "$paramName", value: "$paramValue"));
//		}
//		def b = build(job: "${jobName}", parameters: paramList);
//		println(b.getResult());
//	}
//}
//
//currentBuild.result = "ABORTED"

//El workflow con rollback está en la lí­nea base 2.2.0

def listaParam = params["lista"];
def listaSkipsParam = params["listaSkips"];

if (listaParam == null || listaParam.length() == 0) {
	println("Ningún job pasado al Controller!!");	
	currentBuild.result = "FAILURE"
	return;
}
def lista = listaParam.split("\n");
def listaSkips = listaSkipsParam!=null ? listaSkipsParam.split(",") : [];

def String injectParam(String valor) {
	if (valor.indexOf("\${") >= 0) {
		String ret = valor;
		boolean continuar = true;
		int indice = 0;
		while (continuar) {
			indice = ret.indexOf("\${", indice);
			if (indice != -1) {
				def remp=params[ret.substring(ret.indexOf("{", indice)+1,ret.indexOf("}", indice))];
				// Detección temprana de bucles
				if (remp == valor) {
					throw new Exception("Bucle infinito detectado en $valor");
				}
				if (remp!=null){
					ret = ret.replace("\${${ret.substring(ret.indexOf("{", indice)+1,ret.indexOf("}", indice))}}",remp);
				}
				else {
					ret = ret.replace("\${${ret.substring(ret.indexOf("{", indice)+1,ret.indexOf("}", indice))}}",'');
				}
			}
			else {
				continuar = false;
			}
		}
		return ret;
	} else {
		println "Controller: valor -> ${valor}";
		return valor;
	}
}

def buildSkip = { paramsLocal, job, paramList ->
	if (listaSkips.find{skip -> job==skip}!=null) {
		println "job: ${job} skipped!!";
	} else {
		def suffix = paramsLocal["suffix"];
		if (suffix != null && suffix!="" && suffix != "null") {
			job = "${job}${suffix}";
		}
		def b = build(job: "${job}", parameters: paramList);
	}
}

def jobs = lista;
def b = null;
def ok = true;
jobs.each() { jobx ->
	println jobx;
	if (!jobx.startsWith("#")) {
		def paramsLocal = [:];	
		def parameters = [];
		paramsLocal.putAll(params);
		params.keySet().each { String key ->
			parameters.add(string(name: key, value: params[key]));
		}		
		def t = jobx.split("\\?");
		def job = t[0];
		if (t.size() > 1) {
			if (t[1] != null && t[1].length() > 0){
				t[1].split("&").each { String param ->
					def temp = param.split("=");					
					def value = temp.length>1 ? injectParam(temp[1]) : "";
					parameters.add(string(name: temp[0], value: value));
				}
			}
		}
		if (ok) {
			println "INICIO: ${new Date()} ---";
			if (paramsLocal["block"]=="false") {
				try {
					b = buildSkip(paramsLocal , job, parameters);
					currentBuild.result = "SUCCESS";
				} catch(Exception e) {
					currentBuild.result = "SUCCESS";
				}
			} else {
				b = buildSkip(paramsLocal, job, parameters);
				if (b != null) {
					ok = b.getResult() == "SUCCESS";
				}
			}
			if (b != null) {
				println "FIN:    ${new Date()} ---";				
			}
			println "FIN: ${new Date()} ---";
		}
	}
}