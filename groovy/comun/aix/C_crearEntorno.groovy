package aix

import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import hudson.model.*
import jenkins.model.*
import java.util.regex.*

try{
	def jenkinsHome = build.getEnvironment().get("JENKINS_HOME")
	def entrada 	= build.buildVariableResolver.resolve("nombreFichero")
	def jobName	= build.getEnvironment().get("JOB_NAME")
	def stream 		= build.buildVariableResolver.resolve("STREAM")
	if (stream == null || stream.trim().length() == 0) {
		// Intentar obtenerla del nombre del job 
		stream = jobName.replaceAll("-CreateEnvironment","")
	}
	
	// No interesa el calificador de la corriente (se quita el -DESARROLLO)
	stream = stream.replaceAll("-DESARROLLO","")
	
	esCompila 		= false
	error			= false
	
	File pathArchivoXML				= new File ("${build.workspace}/entornos/"+entrada.replace(".env","")+"/"+entrada)
	def pathDescargaLibreriasBase	= jenkinsHome + "/entornosCompilacion/${stream}"
	def pathDescargaLibrerias		= creaPathLibrerias(entrada, pathDescargaLibreriasBase)
	
	File directorioLibrerias = new File (pathDescargaLibrerias)
	if (!directorioLibrerias.exists()){	
		println "El entorno de compilación no existe. Se ha buscado en:"+pathDescargaLibrerias
	}
	else {
		println "El entorno de compilación ya existe en ${pathDescargaLibrerias}.  Eliminándolo..."
		directorioLibrerias.deleteDir()
	} 
	File createDirLibs = new File ("${pathDescargaLibrerias}/lib")
	println "Se crea el directorio:"+createDirLibs
	createDirLibs.mkdirs()
	println "descargaLibrerias..."
	descargaLibrerias(pathArchivoXML, pathDescargaLibrerias, pathDescargaLibrerias + "/lib", build)
	println "empaquetaLibrerias..."
	empaquetaLibrerias(entrada, pathDescargaLibrerias)
	//creaFicheroComandos(esCompila, entrada)
	println "creaShellScript..."
	creaShellScript(stream,esCompila, entrada)
	
	def params = []
	params.add(new StringParameterValue("stream","$stream"))
  	build.addAction(new ParametersAction(params))
}catch(InterruptedException ie){
	println "Se interrumpe la ejecución"
}finally{
	File directorio = new File(pathDescargaLibrerias)
	if (directorio.exists() && error) {
		println "HA HABIDO UN ERROR. Se elimina el rastro creado por el proceso"
		directorio.deleteDir()
		build.getExecutor().interrupt(Result.FAILURE)
	}
}


def descargaLibrerias(File archivo, String pathDescarga, String pathDescargaLibrerias, AbstractBuild build){
	try{
	println "===== Descarga Librerías ====="
	def pathNexusRelease = build.getEnvironment().get("C_NEXUS_RELEASES_URL")
	def pathNexus = build.getEnvironment().get("C_NEXUS_SNAPSHOTS_URL")
	def libs 		= new XmlSlurper().parse(archivo)
	def librerias 	= libs.lib
	for (int i=0; i<librerias.size(); i++){
		String groupId 		= librerias[i].groupId.text().replace(".", "/");
		String componente 	= librerias[i].componente.text()
		String version 		= librerias[i].version.text();
		String scope 		= librerias[i].scope.text();
		
		pathNexusFinal = ""
		if (version.contains("-SNAPSHOT")){
			pathNexusFinal 	= pathNexus + groupId + "/" + componente + "/" + version + "/" + componente + "-" + version + ".tar"
		}else{
			pathNexusFinal 	= pathNexusRelease + groupId + "/" + componente + "/" + version + "/" + componente + "-" + version + ".tar"
		}
		println pathNexusFinal
		URL urlNExus = new URL(pathNexusFinal);
		ReadableByteChannel lectorEci = Channels.newChannel(urlNExus.openStream());
		ReadableByteChannel lectorEciRelease = Channels.newChannel(urlNExus.openStream());
		println "pathDescargaLibrerias:"+pathDescargaLibrerias
		File destino = new File(pathDescargaLibrerias+"/"+componente+".tar")
		FileOutputStream fos = new FileOutputStream(destino);
		fos.getChannel().transferFrom(lectorEci, 0, 1 << 24);
		println "To local file system path:"+pathDescargaLibrerias+"/"+componente+".tar"
	}
	println "pathDescarga: ${pathDescarga}" 
	new AntBuilder().copy(todir: "${pathDescarga}") {
		fileset(dir : "${build.workspace}/entornos/makes/") {
			include(name:"**/*")
		}
	}

			
	}catch(FileNotFoundException fileExc){
		println "El error:"+fileExc 
		build.getExecutor().interrupt(Result.FAILURE)
	}catch(Exception e){
		println "ERROR en descargaLibrerias"
		error = true
		e.printStackTrace()
	}	
}

def creaPathLibrerias (String file, String path){
	try{
		if (file.contains(".sf")){
			path += "/compila_sf/"
			esCompila = false
			println "escompila:"+esCompila
		}else{
			path += "/compila/"
			esCompila = true
			println "escompila:"+esCompila
		}
		path += file.replace(".env","")
		println "Ruta donde se descargan las librerías del entorno:" + path
		return path;
	}catch(Exception e){
		println "ERROR en creaPathLibrerias"
		error = true
		e.printStackTrace()
	}
}

def empaquetaLibrerias (String nombreTar, String path){
	try{
		println "===== Empaqueta Librerías ====="
		def comando = "tar cvf ${build.workspace}/entornos/"+nombreTar+".tar --format=v7 ."
		println "Ejecutando:" + comando
		def proc  = comando.execute(null, new File(path))
		proc.waitFor()
		println proc.in.text
		println proc.err.text
	}catch(Exception e){
		println "ERROR en empaquetaLibrerias"
		error = true
		e.printStackTrace()
	}
}

def creaShellScript(def stream,def esCompila, String entrada){
	try{
		def pathBaseServidorC 				= "/jenkins/entornosCompilacion/${stream}" // Siempre fijo por seguridad.
		def comandoBorrarLinkBase 			= "rm -f "+pathBaseServidorC
		def comandoCrearLinkBase			= "ln -s "+pathBaseServidorC
		def comandoEliminarPathBase 		= "rm -rf "+pathBaseServidorC
		def comandoCrearPathBase			= "mkdir -p "+pathBaseServidorC
		def comandoCopiarEntrada			= "cp "+entrada+".tar "+pathBaseServidorC
		def comandoIrAPathBase				= "cd "+pathBaseServidorC
		def comandoIrADescomprimirLibrerias	= "cd "+pathBaseServidorC
		def comandoDescomprimirLibrerias	= "for FILE in *;do tar -xvf \$FILE; done"
		def comandoCrearDirectorioInclude = "mkdir ../include"
		def comandoCopiarIncludesLibs	= "cp -R **/*/*.h ../include; cp -R **/*.h ../include; cp -R **/*/*.a . ; cp -R **/*.a ."
		def comandoEliminarTars = "rm -rf *.tar; rm -rf */"
		def buildNumber = build.getNumber()
		def creaEntorno = new File("${build.workspace}/entornos/creaEntorno${buildNumber}.sh")
		
		if (creaEntorno.exists()) {
			assert creaEntorno.delete()
			assert creaEntorno.createNewFile()
		}
		
		
		if (esCompila){
			comandoBorrarLinkBase 	+= "/compila/LAST"
			comandoCrearLinkBase 	+= "/compila/" + entrada.replace(".env","")+ " " +pathBaseServidorC+"/compila/LAST"
			comandoEliminarPathBase += "/compila/" + entrada.replace(".env","")
			comandoIrADescomprimirLibrerias +="/compila/" + entrada.replace(".env","") + "/lib/"
			comandoCrearPathBase +="/compila/" + entrada.replace(".env","")
			comandoCopiarEntrada +="/compila/" + entrada.replace(".env","")
			comandoIrAPathBase +="/compila/" + entrada.replace(".env","")
		}else{
			comandoBorrarLinkBase 	+= "/compila_sf/LAST"
			comandoCrearLinkBase 	+= "/compila_sf/" + entrada.replace(".env","")+ " " +pathBaseServidorC+"/compila_sf/LAST"
			comandoEliminarPathBase += "/compila_sf/" + entrada.replace(".env","")
			comandoIrADescomprimirLibrerias +="/compila_sf/" + entrada.replace(".env","") + "/lib/"
			comandoCrearPathBase += "/compila_sf/" + entrada.replace(".env","") 
			comandoCopiarEntrada += "/compila_sf/" + entrada.replace(".env","") 
			comandoIrAPathBase 	+= "/compila_sf/" + entrada.replace(".env","") 
		}
		def borrarSh	= "rm /jenkins/buzon/${stream}/creaEntorno${buildNumber}.sh"
		def borrarTar	= "rm /jenkins/buzon/${stream}/"+entrada+".tar"
		def borrarCarpetaBin = "rm -rf bin"
		def borrarDeployProperties= "rm -rf deploy.properties"
		def borrarLibreriasTar = "ls *.tar | xargs rm -rf"
		def result = comandoEliminarPathBase+"\n"+comandoCrearPathBase+"\n"+comandoCopiarEntrada+"\n"+comandoBorrarLinkBase+"\n"+comandoCrearLinkBase+"\n"+borrarLibreriasTar+"\n"+comandoIrAPathBase+"\n"+"tar -xvf "+entrada+".tar\n"+"rm "+entrada+".tar\n"+comandoIrADescomprimirLibrerias+"\n"+comandoDescomprimirLibrerias+"\n"+comandoCrearDirectorioInclude+"\n"+comandoCopiarIncludesLibs+"\n"+borrarDeployProperties+"\n"+comandoEliminarTars+"\n"+borrarCarpetaBin+"\n"+borrarSh+"\n"+borrarTar

		println result
			creaEntorno.setText(result)
	}catch(Exception e){
		println "ERROR en creaShellScript"
		error = true
		println e
		e.printStackTrace()
	}

}
