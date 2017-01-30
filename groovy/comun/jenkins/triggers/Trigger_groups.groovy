import groovy.json.JsonSlurper

def getOrdered = params["getOrdered"].toString();
def maxParallelJobs = params["MaxParallelJobs"];
def fallar_inmediatamente = params["fallar_inmediatamente"];
def componentesRelease = params["componentesRelease"];
def jobs = params["jobs"];

if(maxParallelJobs == null || maxParallelJobs.trim().equals("") || maxParallelJobs.trim().equals("0")) {
	maxParallelJobs = 5;
} else {
	maxParallelJobs = maxParallelJobs.trim().toInteger();
}

JsonSlurper js = new JsonSlurper();
def jobsJson = js.parseText(jobs);

// "job" es un json que define los grupos de jobs que podrán lanzarse en paralelo.
// En caso de getOrdered = false jobs sólo contendrá una lista con todos los jobs a lanzar en paralelo.
// En caso de getOrdered = true jobs podrá contener varias listas con los grupos de jobs a lanzar en paralelo.
jobsJson.each { thisJobList ->
	def jobsFailed = false;
	// Poblar una cola
	java.util.concurrent.ConcurrentLinkedQueue queue = new java.util.concurrent.ConcurrentLinkedQueue();
	thisJobList.each { queue.add(it) }
	def builds = [:];
	// Deseamos instanciar maxParallelJobs
	def consumer = {
		boolean keepOn = true;
		while (keepOn) {
			def job = queue.poll();
			if (job == null) {
				keepOn = false;
			}
			else {
				try {
					def b = build(params, job);
					builds["${job}"] = b;
					def result = b.getBuild().getResult();
					if(!result.equals(hudson.model.Result.SUCCESS)) {
						println("Job job ${job} finished: FAILURE");
					} else if(result.equals(hudson.model.Result.SUCCESS)) {
						println("Job \"${job}\" finished: SUCCESS");
					}
				} catch(Exception e) {
					jobsFailed = true
					build.getState().setResult(SUCCESS)
				}
			}
		}
	}

	def parallelJobs = []

	for (int i = 0; i < maxParallelJobs; i++) {
		// Añadir un consumidor nuevo a la lista
		parallelJobs << consumer;
	}
	// Lanzar los consumidores
	parallel(parallelJobs);
	println "Consumida la lista de jobs..."

	builds.keySet().each { String job ->
		def result = builds.get(job).getBuild().getResult();
		if(!result.equals(hudson.model.Result.SUCCESS)) {
			jobsFailed = true;
		}
	}

	if(jobsFailed) {
		build.getState().setResult(FAILURE)
		if(getOrdered == "true") {
			throw new Exception("Ha habido fallos en jobs.");
		}
	}
}



