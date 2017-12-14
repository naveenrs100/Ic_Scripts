package aix

import hudson.model.Result

TARGET_CHECK = "check"
 
multiProject = false



def markVesion(def fileIC, def dir, def version, def target) {  
  if (target.equals(TARGET_CHECK)) return;    
  fileIC << " cd ${dir} \n"
  fileIC << " for f in \$(ls) \n"
  fileIC << "  do \n"
  fileIC << "   echo \"Fichero a firmar: \$f \"\n"  
  fileIC << "   version -a \"${version}\" \$f \$f executable; echo \$f-resultVersion=\$? >> ../${target}.log 2>&1 \n" 
  fileIC << "  done \n"
  fileIC << " cd .. \n"
}

def markStrip(def fileIC, def dir, def version, def target) {  
  if (target.equals(TARGET_CHECK)) return;    
  fileIC << " cd ${dir} \n"
  fileIC << " for f in \$(ls) \n"
  fileIC << "  do \n"
  fileIC << "   echo \"Fichero a stripear: \$f \"\n"  
  fileIC << "   strip \$f executable; echo \$f-resultStrip=\$? >> ../${target}.log 2>&1 \n" 
  fileIC << "  done \n"
  fileIC << " cd .. \n"
}

def errorManagement(def fileIC, def makefile,def target) {
	fileIC << 'ret_code=$? \n'
	fileIC << 'if [ $ret_code -gt 1 ]; then \n';
	fileIC << "printf \"Error compilando $makefile \n\" \n";
	fileIC << "cat ${target}.log \n";
	fileIC << 'exit $ret_code \n';
	fileIC << 'fi \n';
}

def makeICFile(def apiSH,def build,def env,def stream,def componente,def maquina,def target, def version, def filesOutCompile) {
     def fileIC = new File("${build.workspace}/ic.sh")

	componente = componente.replaceFirst(".*-","")

     if (fileIC.exists()) {
         fileIC.delete()
     }     
     apiSH.getenvSh(fileIC,stream,componente,maquina,env)     
     if (!fileIC.text.contains("GROUPID_EXT=")) {
        throw UndefinedEnvironment ("No existe GROUPID_EXT definido para el stream")     
     }
     fileIC << "export GROUPID=\${GROUPID}.\${GROUPID_EXT} \n"
     fileIC << "rm GROUPDID.info | tee tee.log \n"   	 
     fileIC << "echo \${GROUPID} >> GROUPID.info \n"                     
        	 
	 if (!target.equals(TARGET_CHECK)) {        	 
     	fileIC << " mkdir ic/libexec \n"
     	fileIC << " cp \${COMPILA}/lib/*.so ic/libexec \n"
     	fileIC << " cp \${COMPILA_SF}/lib/*.so ic/libexec \n"
     }
        	 
	 println "*************************************"
	 println ""
	 println "${build.workspace}"
	 println "${build.getWorkspace()}"
	 println "*************************************"
     fileIC << "echo \"<PROJECTS>\" >> ${target}.log\n"
     multiProject = false        	 
     // componente multiproyecto
     if (new File("${build.workspace}/makeParent.mak").exists()) {
        multiProject = true
     	fileIC << "echo \"Tratando proyecto multiproyecto ...\"\n"
		// Asegurar la ordenación de los directorios
		List<File> dirs = []
		new File("${build.workspace}").eachDir()  { file ->
			dirs << file
        }
		Collections.sort(dirs)
		dirs.each { file ->
     	//new File("${build.workspace}").eachDir()  { file ->
     		def makeFile = new File("${build.workspace}/${file.name}/${file.name}.mak")
     		println "Makefile: ${makeFile}"
     		println "Makefile exists: " + makeFile.exists()
     		println "File name: ${file.name}"
     		println "Target: ${target}"
     		if (!file.name.startsWith(".") &&
     		!file.name.equals("ic") && makeFile != null && makeFile.exists()) { 
   		    	if (!target.equals(TARGET_CHECK)) { // si no es test, se regenera el binario entero
   		    		fileIC << "mkdir ./${file.name}/bin | tee tee.log \n"
   		    		fileIC << "rm -rf ./${file.name}/bin/* | tee tee.log \n"
   		    		fileIC << "mkdir ./${file.name}/lib | tee tee.log \n"
   		    		fileIC << "rm -rf ./${file.name}/lib/* | tee tee.log \n"   	
   		    		fileIC << "chmod 777 ./${file.name}/lib ./${file.name}/bin | tee tee.log \n"	    		
   		    	} 		   
   		    	   		    	
   		    	fileIC << "echo \"Invocando make ./${file.name}/${file.name}.mak ...\"\n"
   		    	fileIC << "echo \"<PROJECT name='${file.name}'>\" >> ${target}.log\n"   		    	
   		    	fileIC << "cd ${file.name} \n"
				fileIC << "if [[ -e ${file.name}.mak ]]; then \n"
   				fileIC << "make -s -f ${file.name}.mak ${target} >> ../${target}.log 2>&1 \n"
				fileIC << "fi \n"
				errorManagement(fileIC, "${file.name}.mak","../${target}")
   				markStrip(fileIC, "bin", version, target)
   				markStrip(fileIC, "lib", version, target)
   				markVesion(fileIC, "bin", version, target)
   				markVesion(fileIC, "lib", version, target)
   				fileIC << "cd .. \n"
   				fileIC << "echo \"</PROJECT>\" >> ${target}.log\n"
   			}else{
   				filesOutCompile.add(file.name)
   			}
   		}
     } else {
      	if (!target.equals(TARGET_CHECK)) { // si no es test, se regenera el binario entero   		   
       		fileIC << "mkdir bin | tee tee.log \n"
       		fileIC << "rm -rf bin/* | tee tee.log \n"
   	   		fileIC << "mkdir lib | tee tee.log \n"
   	   		fileIC << "rm -rf lib/* | tee tee.log \n"   
   	   		fileIC << "chmod 777 lib bin | tee tee.log \n"
   	   	}
   	   	   	   	 
       	if (!(fileIC.text.contains("MAKEFILE="))) {
         	fileIC << "MAKEFILE=${componente}.mak\n"         	
     	}
     
     	fileIC << "echo \"Invocando make ${componente}.mak ...\"\n"
   		fileIC << "echo \"<PROJECT name='${componente}'>\" >> ${target}.log\n"
		fileIC << 'if [[ -e $MAKEFILE ]]; then \n'
     	fileIC << "make -s -f \$MAKEFILE ${target} >> ${target}.log 2>&1 \n"
		errorManagement(fileIC, "${componente}.mak","${target}")
		fileIC << 'fi \n'
     	markStrip(fileIC, "bin", version, target)
   		markStrip(fileIC, "lib", version, target)
     	markVesion(fileIC, "bin", version, target)
   		markVesion(fileIC, "lib", version, target)
   		fileIC << "echo \"</PROJECT>\" >> ${target}.log\n"     	
     }
     fileIC << "echo \"</PROJECTS>\" >> ${target}.log\n"
     println ""
     println "*** Contenido del fichero ic.sh"     
     println fileIC.text
     println "*** Fin del fichero ic.sh"   
}

def generateSlaveSH(def build, String stream, String componente, def target, def filesOutCompile) {
  def jobName = build.getProject().name
  def buildNumber= build.getNumber()  
  //def fileSlave = new File("${build.workspace}/slave${buildNumber}.sh")
  def fileSlave = new File("${build.workspace}/slave.sh")
  if (fileSlave.exists()) {
  	fileSlave.delete()
  }
  
  def jenkinsHome = build.getEnvironment().get("JENKINS_HOME")
  def jenkinsBuzon = build.getEnvironment().get("JENKINS_BUZON") + "/${stream}"
  fileSlave << "if [ ! -d \"${jenkinsHome}/${stream}\" ]; then \n"
  fileSlave << " mkdir  \"${jenkinsHome}/${stream}\" \n"
  fileSlave << "fi \n"
  fileSlave << "rm -rf  \"${jenkinsHome}/${stream}/${componente}\" | tee log.txt\n" 
  fileSlave << "mkdir  \"${jenkinsHome}/${stream}/${componente}\" | tee log.txt\n"
  //fileSlave << "mv ${jenkinsBuzon}/${jobName}${buildNumber}.tar  \"${jenkinsHome}/${stream}/${componente}\"\n"
  fileSlave << "mv ${jenkinsBuzon}/${componente}.tar  \"${jenkinsHome}/${stream}/${componente}\"\n"
  
  fileSlave << "cd \"${jenkinsHome}/${stream}/${componente}\"\n"
  fileSlave << "tar -xvf  ${componente}.tar  | tee tar.txt\n" 
  // Limpieza del tar
  fileSlave << "rm  \"${jenkinsHome}/${stream}/${componente}/${componente}.tar\" \n" 
  //fileSlave << "cd \"${componente}\"\n"
 
  fileSlave << "tr -d \"\\015\\032\" < ic.sh > icExec.sh\n"
  fileSlave << "chmod 755 *.sh\n"
  fileSlave << ". ./icExec.sh \n"

  if (!target.equals(TARGET_CHECK)) {
  	//fileSlave << "tar cvf ${jobName}${buildNumber}_LIBEXEC.tar ic/libexec | tee tarLibexec.txt\n"   
  	//fileSlave << "tar cvf ${jobName}${buildNumber}_LOG.tar all.log GROUPID.info | tee tarResultLog.txt\n"
    fileSlave << "tar cvf ${componente}_LIBEXEC.tar ic/libexec | tee tarLibexec.txt\n"   
    fileSlave << "tar cvf ${componente}_LOG.tar all.log GROUPID.info | tee tarResultLog.txt\n"
  	if (multiProject) {
  		def extraFiles = ""
  		if (filesOutCompile != null && filesOutCompile.size() > 0){
  			filesOutCompile.size().times{
  				if (!filesOutCompile[it].equals("ic")){
  					extraFiles += filesOutCompile[it]
  					extraFiles += " "
  				}
  			}
  		}
    	//fileSlave << "tar cvf ${jobName}${buildNumber}_RESULT.tar deploy.properties ${extraFiles}./**/bin ./**/include ./**/lib | tee tarResult.txt\n"
    	fileSlave << "mkdir ../tempIC\n"
    	fileSlave << "cp -r * ../tempIC\n"
    	fileSlave << "rm -rf ../tempIC/**/fuentes\n"
    	fileSlave << "rm -rf ../tempIC/**/all.log\n"
    	fileSlave << "rm -rf ../tempIC/**/*.mak\n"
    	fileSlave << "cp -r ../tempIC ../tempStream\n"
    	fileSlave << "cd ../tempIC/\n"
    	fileSlave << "rm -rf ic\n"
    	fileSlave << "tar cvf ${componente}_RESULT.tar deploy.properties ./**/* | tee tarResult.txt\n"
    	//fileSlave << "tar cvf ${jobName}${buildNumber}_RESULT.tar deploy.properties ${extraFiles}./**/* | tee tarResult.txt\n"
    	fileSlave << "mv ${componente}_RESULT.tar \"${jenkinsHome}/${stream}/${componente}\"\n"
    	fileSlave << "cd \"${jenkinsHome}/${stream}/${componente}\"\n"
    	fileSlave << "rm -rf ../tempIC\n"
    	//fileSlave << "tar cvf ${jobName}${buildNumber}_RESULT.tar deploy.properties ${extraFiles}./**/* | tee tarResult.txt\n"
  	} else {    
    	//fileSlave << "tar cvf ${jobName}${buildNumber}_RESULT.tar deploy.properties bin include lib | tee tarResult.txt\n"
    	//fileSlave << "tar cvf ${jobName}${buildNumber}_RESULT.tar deploy.properties * | tee tarResult.txt\n"
    	fileSlave << "mkdir ../tempIC\n"
    	fileSlave << "cp -r * ../tempIC\n"
    	fileSlave << "rm -rf ../tempIC/ic\n"
    	fileSlave << "rm -rf ../tempIC/fuentes\n"
    	fileSlave << "rm -rf ../tempIC/*.log\n"
    	fileSlave << "rm -rf ../tempIC/*.mak\n"
    	fileSlave << "rm -rf ../tempIC/*.txt\n"
    	fileSlave << "rm -rf ../tempIC/*.sh\n"
    	fileSlave << "rm -rf ../tempIC/*.url\n"
    	fileSlave << "rm -rf ../tempIC/*.tar\n"
    	 //fileSlave << "cp -r ../tempIC ../tempStream\n"
    	fileSlave << "cd ../tempIC/\n"
    	fileSlave << "tar cvf ${componente}_RESULT.tar deploy.properties ./* | tee tarResult.txt\n"
    	fileSlave << "mv ${componente}_RESULT.tar \"${jenkinsHome}/${stream}/${componente}\"\n"
    	fileSlave << "cd \"${jenkinsHome}/${stream}/${componente}\"\n"
    	fileSlave << "rm -rf ../tempIC\n"
  	}
  } else {
    fileSlave << "sed -e 's/'\$(echo \"\033\")'/ /g' < check.log > check_temp.log | tee tee.log\n"
	fileSlave << "mv check_temp.log check.log | tee tee.log \n"
	fileSlave << "tar cvf ${componente}_LOG.tar check.log | tee tarResultLog.txt\n"
	
	fileSlave << "tar cvf ${componente}_JUNIT.tar bin/*.xml **/bin/*.xml | tee tarResultJunit.txt\n"
  }   
}


def generateIC(def build,String stream, String componente, def maquina, def target, def version, def filesOutCompile) {
     def env
     def apiSH = new C_API_envSH()
     apiSH.initLogger({ println it })
     if (maquina != null && maquina.length()==0) maquina = null
     try {
         
         env =  apiSH.getEnvSHByComponent(stream, componente)
         println "Entorno calculado: $env"
         makeICFile(apiSH,build,env,stream,componente,maquina,target,version,filesOutCompile)
     } catch (UndefinedEnvironment e) {
         println "" 
         println e.msg
         build.getExecutor().interrupt(Result.FAILURE)  
     }
}

def getVersionComponent(def build) {
  def fileBaseline = new File("${build.workspace}/baseline.log")
  def fileVersion = new File("${build.workspace}/ic/deploy/VERSION.info")
  def bline = null
  def version = null
  
  fileBaseline.withReader{ reader->
  	reader.eachLine{
  		if (it.contains("Baseline")){
			println  "Baseline encontrado"
			bline=  it
  		}
  	}
  }	

  println "bline: $bline"
  if (bline != null){
	def out = bline.split("\"")
	version = out[1].trim()
  }
  if (fileVersion.exists()) fileVersion.delete()
  fileVersion << version  
  return version
}

def version = getVersionComponent(build)
def filesOutCompile = []
generateIC(build, build.buildVariableResolver.resolve("stream"),
                  build.buildVariableResolver.resolve("componente"),
                  build.buildVariableResolver.resolve("slave"), 
                  build.buildVariableResolver.resolve("target"),
                  version,
                  filesOutCompile)

generateSlaveSH(build,  build.buildVariableResolver.resolve("stream"),
						build.buildVariableResolver.resolve("componente"), 
						build.buildVariableResolver.resolve("target"),
						filesOutCompile)
						

						