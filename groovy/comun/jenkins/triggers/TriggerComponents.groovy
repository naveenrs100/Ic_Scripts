package jenkins.triggers

def componentList = params["componentsStream"]
if (componentList!=null && componentList.length()>0){
	final List list = componentList.replaceAll("\\[","").replaceAll("\\]","").replaceAll(" ","").split(",");
	//println list
	def log = {b, component, direction ->
		println "FIN:    ${new Date()} ---"
		println(b.getBuild().getLog())
	}
	def componentsFailed = false
	def librariesParam = params["compilationEnvironmentC"]
	final List libraries = []
	if (librariesParam != null && librariesParam.trim().length()  > 0) {
		println "Bibliotecas informadas a TriggerComponents:"
		librariesParam.split(",").each { libraries.add(it); println it }
	}
	def executeComponents = {components ->
		def b
		components.each() { component ->
			try{
				println "INICIO: ${new Date()} ---"
				println 'params: ' + params
				println('Building job: ' + params["job"])
				//b = build(params,params["job"],componente:"${component}")
				component=component.replace('[','')
				component=component.replace(']','')
				component=component.replace(' ','')
				println('Building component: ' + component)
				//b = build(params["job"],componente:"${component}")
				def componentsMap = new HashMap()
				componentsMap.put("component","${component}")
				if (libraries.contains(component)) {
					println "${component} <- IDENTIFICADO COMO BIBLIOTECA"
					componentsMap.put("isLibrary","true")
				}
				else {
					componentsMap.put("isLibrary","false")
				}
				componentsMap.put("stream",params["stream"])
				componentsMap.put("action",params["action"])
				componentsMap.put("maquina",params["maquina"])
				componentsMap.put("environmentCatalogC",params["environmentCatalogC"])
				componentsMap.put("workItem",params["workItem"])
				componentsMap.put("parentWorkspace",params["homeStream"])
				componentsMap.put("workspaceRTC",params["workspaceRTC"])
				componentsMap.put("release",params["release"])
				componentsMap.put("executionUuid",params["executionUuid"])
				b = build(componentsMap,params["job"])
				if (b!=null){
					log(b,component,"EXECUTION")
				}
				if (build.getState().getResult() != null && build.getState().getResult() == FAILURE){
					componentsFailed = true
				}
				build.getState().setResult(SUCCESS)
			}catch(Exception e){
				println e
				componentsFailed = true
				build.getState().setResult(SUCCESS)
			}
		}
		return true
	}
	// Ordenar la lista
	java.util.Collections.sort(list, new java.util.Comparator() {
				public int compare(Object o1, Object o2) {

					int tmp1 = 1;
					int tmp2 = 1;

					if (libraries.contains(o1)) {
						tmp1 = 0;
					}

					if (libraries.contains(o2)) {
						tmp2 = 0;
					}

					return tmp1 - tmp2;
				}
			});
	println "---> Orden de ejecución: "
	int index = 0
	list.each { println(index++); println(it); }
	executeComponents (list)
	if (componentsFailed){
		build.getState().setResult(FAILURE)
	}
}else{
	println("¡No se ha lanzado el job de ningun componente!")
	build.getState().setResult(NOT_BUILT)
}