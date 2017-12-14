package aix
import hudson.model.*
import jenkins.model.*


Closure logger = null


def initLogger( Closure _logger ) {
	logger = _logger
}

// log de la clase haciendo uso de closure
def log(def msg) {
  if (logger != null) {
  	println logger(msg)
  }
}

def getPathCatalogo(stream) {
	def streamWithoutEnv = stream
	if (stream.contains("-DESARROLLO")){
		streamWithoutEnv = stream.replace("-DESARROLLO","")
	}else{
		streamWithoutEnv = stream.replace("-RELEASE","")
	}
	def pathBase = "/jenkins/entornosCompilacion/${streamWithoutEnv}/catalogo"
	//def pathBase = "/jenkins/entornosC/${stream}/catalogo"
	return pathBase
} 

def getPathCatalogo() {
  //return Thread.currentThread().executable.getEnvironment().get("ENVIRONMENT_CATALOGO_PATH")
} 

def checkExistsOrRelease(def rutaStream,def rutaStreamRelease) {
	 def fileSearched =new File("${rutaStream}")
	 if (fileSearched.exists()) {
	 	println "Encontrada ruta \"${rutaStream}\""
        return rutaStream
     }
     fileSearched = new File("${rutaStreamRelease}") 
     if (fileSearched.exists()) {
     	println "Encontrada ruta \"${rutaStreamRelease}\""
        return rutaStreamRelease
     }     
     return null
}

def getEnvSHByStream(def stream) {
	def path = getPathCatalogo(stream)
	log("Path: ${path}")
	def rutaStreamOpt1 = "${path}/${stream}"
	def rutaStreamOpt2 = "${path}/Release/${stream}"

    log("Revisando carpeta del stream $stream para determinar entorno de compilacion")
	def fileStream = null
	def fileExists = checkExistsOrRelease(rutaStreamOpt1, rutaStreamOpt2)
	if ( fileExists != null) {
		log("Existe el fichero.")
		fileStream = new File("$fileExists")
	}
	def env = null
	def partHost,parts
	
	 // parte del stream
    if ( fileStream == null) {
       throw new UndefinedEnvironment(" No existe directorio para incluir el stream $stream en IC !!")
    }
    // parte del stream
    fileStream.eachFile { file ->
         log(" Tratando fichero de Stream: $file")
        
         partHost = "$file".split(/\#/)
         if (partHost.size()==2) {            
            parts = partHost[1].split(/\./)              
         } else {
            parts = "$file".split(/\./)
		 }
			 
         if (parts.size() == 1) return         
         if (parts.size()!=3) {
             throw new UndefinedEnvironment("Fichero ${file} con formato incorrecto: stream.{entorno}.sh")
         }
         if (!parts[2].equals("sh")) {
             throw new UndefinedEnvironment("Fichero ${file} en ruta $rutaStream no finaliza con .sh")
         }
         if (!parts[0].endsWith("stream")) {
             throw new UndefinedEnvironment("Fichero ${file} en ruta $rutaStream no es de stream (stream.)")
         }
         if (env == null) {
             env = parts[1]
         } else {
             if ( !env.equals(parts[1])) {
	             throw new UndefinedEnvironment("Definidos mas de un entorno para el stream en $rutaStream")
             }
         }
     }
     return env
}

def getEnvSHByComponent(String  stream, String  component) {
	def path = getPathCatalogo()
    def rutaCompOpt1 = "${path}/${stream}/${component}"
	def rutaCompOpt2 = "${path}/Release/${stream}/${component}"
    def env = null
    def partHost,parts
    log("Revisando carpeta del componente $component en stream $stream para determinar entorno de compilacion")
	def fileStream = null 
	def fileExists = checkExistsOrRelease(rutaCompOpt1, rutaCompOpt2)	
    if ( fileExists != null) {
		fileStream = new File("$fileExists")
	}
    if (fileStream != null) {
       fileStream.eachFile { file ->
             log (" Tratando fichero de entorno de Componente: $file")
             
             partHost = "$file".split(/\#/)
             if (partHost.size()==2) {            
               parts = partHost[1].split(/\./)              
             } else {
               parts = "$file".split(/\./)
			 }
			 
             if (parts.size()!=3) {
                 throw new UndefinedEnvironment("Fichero ${file} con formato incorrecto: comp.{entorno}.sh")
             }
             if (!parts[2].equals("sh")) {
                 throw new UndefinedEnvironment("Fichero ${file} en ruta $rutaComp no finaliza con .sh")
             }
             if (!parts[0].endsWith("comp")) {
                 throw new UndefinedEnvironment( "Fichero ${file} en ruta $rutaComp no es de componente (comp.)")
             }

             if (env == null) {
                 env = parts[1]             
             } else {
                 if ( !env.equals(parts[1])) {
                     throw new UndefinedEnvironment("Definidos mas de un entorno $env para el componente en $rutaComp")
                 }
             }
         }
     }
     
     if (env == null) return getEnvSHByStream(stream)  
     return env; 
}




def concatFile(StringBuffer stringEnv, def namefileInput) {
	def fileInput = new File(namefileInput)
    if (fileInput.exists()) {
    	log (" Incluyendo contenido del fichero $namefileInput ...")
        stringEnv.append(fileInput.text)
        stringEnv.append("\n")
     }
}

def getStringEnvSh(def stream, def component, def maquina, def env) {
  def path = getPathCatalogo(stream)
  
  def rutaComp = "${path}/${stream}/${component}"
  def rutaCompRelease = "${path}/Release/${stream}/${component}"
  
  def rutaStream = "${path}/${stream}"
  def rutaStreamRelease = "${path}/Release/${stream}"
  
  rutaComp = 	checkExistsOrRelease(rutaComp,rutaCompRelease)
  rutaStream = 	checkExistsOrRelease(rutaStream,rutaStreamRelease)
  
  def rutaBase = "${path}/base"
  StringBuffer stringEnv = new StringBuffer("") 
  
  log( "Inicio getStringEnvSh...")
  log( "Cargando configuracin de entorno $env en mquina $maquina para stream $stream y componente $component")
  concatFile(stringEnv,"$rutaBase/base.${env}.sh")  
  if (maquina!=null) concatFile(stringEnv,"${rutaBase}/${maquina}#base.${env}.sh")
  
  concatFile(stringEnv,"$rutaStream/stream.${env}.sh")
  if (maquina!=null) concatFile(stringEnv,"${rutaStream}/${maquina}#stream.${env}.sh")
  
  if (component!=null) {
  	concatFile(stringEnv,"$rutaComp/comp.${env}.sh")
  	if (maquina!=null) concatFile(stringEnv,"${rutaComp}/${maquina}#comp.${env}.sh")
  }
  
  log( "Fin getStringEnvSh...")
  log( "$stringEnv")
  return "$stringEnv"
}

def getenvSh(def fileIC, def stream, def component, def maquina, def env) {
  log( "Inicio getenvSh...")
  fileIC <<  getStringEnvSh(stream, component, maquina, env)
  fileIC << '\n'
  log( "Fin getenvSh...")
}