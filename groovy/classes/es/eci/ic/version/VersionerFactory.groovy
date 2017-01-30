package es.eci.ic.version

import es.eci.ic.logging.LogUtils;
import es.eci.utils.Utiles

class VersionerFactory {

	static def tecnologias = ["maven":"pom\\.xml","gradle":"build\\.gradle"]
	
	static Versioner getVersioner(printer, technology,action,parentWorkspace,save,checkSnapshot,checkErrors,homeStream,changeVersion, Boolean fullCheck){
		LogUtils log = new LogUtils(printer)
		Versioner versioner;
		
		if (technology==null || technology.length()==0)
			technology = getTechnology(log, "${parentWorkspace}")
			
		if (technology=="gradle"){
			versioner = new GradleVersioner(log, parentWorkspace, checkSnapshot, checkErrors,changeVersion,action, save)
		}else if (technology=="maven"){
			versioner = new MavenVersioner(log, parentWorkspace, checkSnapshot, checkErrors,changeVersion, action, save,homeStream, fullCheck)
		}else{
			throw new TechnologyNotSupportedException("Technology ${technology} is not supported")
		}
		return versioner
	}
	
	static def getTechnology(log, parentWorkspace){
		def technology = "notfound"
		for (def tec in tecnologias){
			log.log "Probando ${tec.key}..."
			def fichero = Utiles.getRootFile(parentWorkspace,tec.value)
			if (fichero!=null && fichero.exists()){
				technology = tec.key
				break
			}
		}
		return technology
	}
}
