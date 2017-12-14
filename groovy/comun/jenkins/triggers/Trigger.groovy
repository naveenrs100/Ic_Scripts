package jenkins.triggers

def listaJobs = params["jobs"]

if (listaJobs !=null && listaJobs.length()>0){
	def lista = listaJobs.split(",");
	println lista
	def log = {b, job, direction ->
		println "FIN:    ${new Date()} ---"
		println(b.getBuild().getLog())
	}
	def jobsFailed = false
	def executeJobs = {jobs ->
		def b
		jobs.each() { job ->
			try{
				println "INICIO: ${new Date()} ---"
				b = build(params, job)
				if (b!=null){
					log(b,job,"EXECUTION")
				}
				if (build.getState().getResult() != null && build.getState().getResult() == FAILURE){
					jobsFailed = true
				}
				build.getState().setResult(SUCCESS)
			}catch(Exception e){
				jobsFailed = true
				build.getState().setResult(SUCCESS)
			}
		}
		return true
	}
	executeJobs(lista)
	if (jobsFailed){
		build.getState().setResult(FAILURE)
	}
}else{
	println("No se ha lanzado ningún job!")
	build.getState().setResult(NOT_BUILT)
}