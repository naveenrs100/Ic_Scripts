package jenkins.controllers

import hudson.model.ParametersAction;

// JENKINS: System groovy script.
//-----------------------------------------------------------------
//-----------------------------------------------------------------
// Punto de entrada del Controller
//-----------------------------------------------------------------
//-----------------------------------------------------------------

String handle = params["identificador"];;

def swarmNode = getNode(handle,
		Long.valueOf(build.getEnvironment().get("DOCKER_SLAVE_DETECTION_MSEC")),
		Integer.valueOf(build.getEnvironment().get("DOCKER_SLAVE_DETECTION_TRIES")));
if (swarmNode == null) throw new Exception("El esclavo no ha arrancado");
println "Nodo levantado en contenedor Docker: $swarmNode";

def populateParams = [:];
populateParams.put("WHERE", swarmNode.getNodeName());
build(populateParams, "PopulateSlave");

// Lanzar los jobs en la lista contra el nodo indicado
def jobs = params["jobsList"].split("\n");
def listaSkipsParam = params["listaSkips"];
def listaSkips = listaSkipsParam!=null?listaSkipsParam.split(","):[];

Boolean ok = true;
Boolean unstable = false;
def b = null;

int jobsNumber = jobs.size();
int failCount = 0;

ignore(ABORTED) {
	jobs.each { String job ->
		if(!job.startsWith("#")) {
			def t = job.split("\\?");
			def jobx = t[0];
			println "$jobx ->"
			Map jobParams = [:];
			params.keySet().each { key ->
				jobParams[key] = params[key]
			}
			if(t.size() > 1) {
				if(t[1] != null && t[1].length() > 0) {
					t[1].split("&").each {
						def param = it.split("=")[0];
						def value = it.split("=")[1];
						println "Asignando param ${param} a value ${value} para el job ${jobx}"
						jobParams[param] = value.length() > 0 ? injectParam(value) : "";
					}
				}
			}
			jobParams.put("WHERE", swarmNode.getNodeName());

			println("El estado del flow por ahora es: ok=${ok}");
			if (ok) {
				println "********** EJECUTANDO job: \"${jobx}\"";
				println "INICIO: ${new Date()} ---";
				// Si el job no es bloqueante tendrÃ¡ el parÃ¡metro "block" a "false".
				//En todo caso el resultado serÃ¡ "SUCCESS".
				if (jobParams["block"]=="false") {
					println("El job ${jobx} no es bloqueante.")
					if(jobParams["markUnstable"]=="true") {
						println("El job ${jobx} puede marcar la construcción como inestable.");
						b = buildSkip(jobParams, jobx, listaSkips);
						if(b.getResult()!=SUCCESS) {
							unstable = true;
							if(b.getResult()==FAILURE) {
								failCount++;
								build.getState().setResult(SUCCESS);
							}
						}
					} else {
						try {
							b = buildSkip(jobParams, jobx, listaSkips);
							build.getState().setResult(SUCCESS);
						} catch(Exception e) {
							build.getState().setResult(SUCCESS);
						}
					}
					// Si el job es bloqueante tendrÃ¡ el parÃ¡metro "block" distinto de "false".
				} else {
					println("El job ${jobx} es bloqueante.")
					b = buildSkip(jobParams, jobx, listaSkips);
					if (b!=null) {
						ok = b.getResult()==SUCCESS;
					}
				}
				if (b!=null) {
					println("FIN:	${new Date()} ---");
				}
				println("FIN: ${new Date()} ---");
			}
		}
	}
}

// Finalmente, destruimos el contenedor creado en todo caso al final del Controller.
if(unstable) {
	if(failCount == jobsNumber) {
		build.getState().setResult(FAILURE);
	} else {
		build.getState().setResult(UNSTABLE);
	}
}
if(!ok) {
	build.getState().setResult(FAILURE);
}


// FunciÃ³n que nos permite referirnos al nodo levantado en el contenedor Docker
def getNode(String handle, long msecWait, int triesLeft) {
	long startGetNode = new java.util.Date().getTime();
	// Se puede sacar a una configuraciÃ³n externa
	def lookForNode = { String pattern ->
		def ret = null;
		for (slave in hudson.model.Hudson.instance.slaves) {
			if (slave.getNodeName().startsWith(pattern)){
				ret = slave;
			}
		}
		return ret;
	}
	def ret = null;
	// lo intenta $triesLeft veces, separadas cada una por periodos de
	// de $msecWait milisegundos.  Pasado este tiempo, asume que el
	// esclavo swarm no ha conseguido levantarse.
	while (ret == null && triesLeft > 0) {
		ret = lookForNode(handle);
		if (ret == null) {
			triesLeft--;
			Thread.sleep(msecWait);
		}
	}
	long endGetNode = new java.util.Date().getTime();
	println "Tiempo empleado en buscar el nodo: " + (endGetNode  - startGetNode ) + " mseg"
	return ret;
}

// EjecuciÃ³n sÃ³lo de los jobs que no formen parte de listaSkips
def buildSkip(paramsLocal, job, listaSkips) {
	def b = null;
	if (listaSkips.find{skip -> job==skip}!=null) {
		println "job: ${job} skipped!!";
	} else {
		def suffix = paramsLocal["suffix"];
		if (suffix!=null && suffix!="" && suffix!="null") {
			job = "${job}${suffix}";
		}
		b = build(paramsLocal, job);
	}
	return b;
}

// Detener el contenedor
def dockerDestroy(handle) {
	println("Destruimos el esclavo ${handle}")
	Map destroyParams = [:];
	destroyParams['nodo']='docker-MX03010018D0350';
	destroyParams['contenedor']=handle;
	build(destroyParams, "docker.destroy");
}

// Inyecta valores
def injectParam(String valor) {
	String returnValue;
	if (valor.indexOf("\${")>=0) {
		String ret = valor;
		boolean continuar = true;
		int indice = 0;
		while (continuar){
			indice = ret.indexOf("\${", indice);
			if (indice != -1) {
				def remp=params[ret.substring(ret.indexOf("{", indice)+1,ret.indexOf("}", indice))];
				// DetecciÃ³n temprana de bucles
				if (remp == valor) {
					throw new Exception("Bucle infinito detectado en $valor");
				}
				if (remp!=null) {
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
		returnValue = ret;
	} else {
		println "Controller: valor -> ${valor}";
		returnValue = valor;
	}
	return returnValue;
}

