package jenkins.controllers

//El workflow con rollback está en la lí­nea base 2.2.0

def listaParam = params["lista"];
def listaSkipsParam = params["listaSkips"];

if (listaParam == null || listaParam.length() == 0){
	println("Ningún job pasado al Controller!!");
	build.getState().setResult(FAILURE);
	return;
}
def lista = listaParam.split("\n");
def listaSkips = listaSkipsParam!=null?listaSkipsParam.split(","):[];

def injectParam = { String valor ->
	if (valor.indexOf("\${")>=0){
		String ret = valor;
		boolean continuar = true;
		int indice = 0;
		while (continuar){
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
	}else{
		println "Controller: valor -> ${valor}";
		return valor;
	}
}

def buildSkip = { paramsLocal, job ->
	if (listaSkips.find{skip -> job==skip}!=null){
		println "job: ${job} skipped!!";
	}else{
		def suffix = paramsLocal["suffix"];
		if (suffix!=null && suffix!="" && suffix!="null"){
			job = "${job}${suffix}";
		}
		build( paramsLocal , job);
	}
}

def jobs = lista;
def b = null;
def ok = true;
jobs.each() { jobx ->
	println jobx;
	if (!jobx.startsWith("#")){
		def paramsLocal = [:];
		paramsLocal.putAll(params);
		def t = jobx.split("\\?");
		def job = t[0];
		if (t.size()>1){
			if (t[1]!=null && t[1].length()>0){
				t[1].split("&").each {param ->
					def temp = param.split("=");
					paramsLocal[temp[0]] = temp.length>1?injectParam(temp[1]):"";
				}
			}
		}
		if (ok){
			println "INICIO: ${new Date()} ---";
			if (paramsLocal["block"]=="false"){
				try {
					b = buildSkip( paramsLocal , job);
					build.getState().setResult(SUCCESS);
				}catch(Exception e){
					build.getState().setResult(SUCCESS);
				}
			}else{
				b = buildSkip( paramsLocal , job);
				if (b!=null){
					ok = b.getResult()==SUCCESS;
				}
			}
			if (b!=null){
				println "FIN:    ${new Date()} ---";
				println(b.getBuild().getLog());
			}
			println "FIN: ${new Date()} ---";
		}
	}
}