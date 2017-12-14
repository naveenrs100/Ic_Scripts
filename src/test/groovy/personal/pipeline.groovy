
def branches = [:] 
for (int i = 0; i < 2 ; i++) {
       int index=i, branch = i+1
       stage ("build"){ 
            branches["branch_${branch}"] = { 
                node ('principal'){
                    build job: "Tarea ${branch}", parameters: [string(name: 'param1', value: "${branch}")]
                }
            }
      }
}


def jobs = params["jobs"].split(",");
def count = 0;
def listaStages = [];
jobs.each { String job ->
	count++;
	listaStages.add({
		node("principal") {
			stage("construcción") {
				build job: "${job}", parameters: [string(name: "param1", value: "COSAS ${count}")]
			}
		}
	});
}


parallel listaStages
