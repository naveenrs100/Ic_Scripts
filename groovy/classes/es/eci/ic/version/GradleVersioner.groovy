package es.eci.ic.version

import es.eci.utils.Utiles

class GradleVersioner extends Versioner {
	
	public GradleVersioner(log, parentWorkspace, checkSnapshot, checkErrors, changeVersion, action, save){
		this.log = log
		this.pattern = "build\\.gradle"
		this.parentWorkspace = parentWorkspace
		this.action = action
		this.checkSnapshot = Boolean.valueOf(checkSnapshot)
		this.changeVersion = Boolean.valueOf(changeVersion)
		this.checkErrors = Boolean.valueOf(checkErrors)
		this.save = save
		
		this.log.log this
	}

	@Override
	def doOnWrite() {
		log.log "---doOnWrite---"
		File fichero = getFicheroRoot()
		
		def fileText = fichero.text
		fileText = fileText.replaceAll("${this.getVersion().version}","${this.getChangedVersion().version}")
		fichero.write(fileText);
		
		File changed = getChangedfile()
		changed << "${fichero}\n"
		
		log.log "\n>> doOnWrite Terminado!!\n"
	}

	@Override
	public Version getVersion() {
		if (lversion==null){
			log.log "---getVersion: obteniendo versión---"
			lversion = new Version()
			getFicheroRoot().eachLine { line ->
				def mVersion = line =~ /.*version\s?=\s?["|'](.*)["|']/
				def mGroupId = line =~ /.*group\s?=\s?["|'](.*)["|']/
				if (mGroupId.matches()) version.groupId = mGroupId[0][1]
				   if (mVersion.matches()) version.version = mVersion[0][1]
			}
			log.log "\n>> Versión: ${version}\n"
		}
		return lversion
	}
	
	@Override
	public String toString(){
		StringBuffer res = new StringBuffer();
		res << "\n---- GRADLE VERSIONER ----"
		res << super.toString()
		res << "\n-------------------------\n"
		return res
	}

}
