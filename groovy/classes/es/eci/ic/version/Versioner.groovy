package es.eci.ic.version

import java.io.File;

import es.eci.ic.logging.LogUtils;
import es.eci.utils.Utiles

abstract class Versioner {

	
	def pattern
	def parentWorkspace
	def action
	def save

	LogUtils log = null
	Boolean checkSnapshot
	Boolean checkErrors
	Boolean changeVersion = true

	File lficheroRoot
	Version lversion;
	Version lchangedVersion;

	abstract def doOnWrite()
	abstract public Version getVersion()

	/* 
	 * Este parámetro se introduce a petición de determinados grupos que desean
	 * tener un procedimiento de release que suba el 4º dígito en lugar del 3º.
	 */
	private Boolean releaseMantenimiento = Boolean.FALSE
	
	/**
	 * Indica si el comportamiento en release es alterar el 4º dígito (es decir,
	 * si la release corresponde a una versión considerada mantenimiento) o bien
	 * se altera el 3º (comportamiento por defecto)
	 * @param releaseMantenimiento Cierto si se quiere marcar la release como
	 * de mantenimiento
	 */
	public void setReleaseMantenimiento(Boolean releaseMantenimiento) {
		this.releaseMantenimiento = releaseMantenimiento;
	}
	
	/**
	 * Método principal que encadena el resto de funcionalidades en cada una de las estrategías de versionado.
	 * Mantiene compatibilidad con lo anterior, el parametro save es un tanto confuso, pero por no cambiar funcionalidad, se mantiene.
	 * @param version
	 * @param groupId
	 * @param parentWorkspace
	 * @param checkSnapshot
	 * @return
	 */
	def final write(){
		log.log "---write---"
		if (save!="false"){
			writeStandardFile()
		}
		checkSnapshotVersion(action,getVersion())
		return doOnWrite()
	}
	
	def writeStandardFile(){
		log.log "---writeStandardFile---"
		Version version = save=="old"?getVersion():getChangedVersion()
		if (checkSnapshot=="false"){
			if (version.version.toLowerCase().indexOf("snapshot")<0){
				throw new NumberFormatException("La versión del entregable (${version.version}) no es válida; debe tener el siguiente formato: X.X.X.X-SNAPSHOT")
			}
		}
		File out = new File("${parentWorkspace}/version.txt")
		if (out.exists()) {
			assert out.delete()
			assert out.createNewFile()
		}
		out << "version=\"${version.version}\"\n"
		out << "groupId=\"${version.groupId}\""

		log.log "\n>> Escribe ${version} en ${out}\n"
	}


	/**
	 * Cambia la versión dependiento de la acción que se necesite.
	 * Lanza NumberFormatException con el mensaje descriptivo del error
	 * @param version Número de versión
	 * @param action Distingue las posibles acciones a llevar a cabo por el método.
	 *  build: Se limita a construir la versión indicada sin hacer más comprobaciones
	 *	addFix: Sería el equivalente al release2release, cambia el dígito 4
	 *	addHotfix: Mantenimiento correctivo de emergencia, cambia el dígito 5 (o lo pone
	 *		si la versión no lo tenía asignado)
	 *	removeSnapshot: Paso 1 del procedimiento de release.  Retira el -SNAPSHOT de las versiones
	 *		de los ficheros de construcción
	 *	addSnapshot: Paso 2 del procedimiento de release.  Una vez hecha y etiquetada en RTC la release,
	 *		incrementa el dígito 3 y pone el -SNAPSHOT para dejar la aplicación preparada para el
	 *		desarrollo de su próxima release.
	 * @param changeVersion Indica si el método actualiza el formato de versión
	 *	antiguo (3 dígitos) al nuevo (4 + hotFix opcional)
	 */
	public Version getChangedVersion(){
		if (lchangedVersion==null){
			log.log "---getChangedVersion: obteniendo nueva versión---"
			def version = getVersion()

			if (lchangedVersion)
				version.version = setDigits(version.version,4)

			lchangedVersion = new Version(version.groupId)

			// Caso trivial, construyendo 
			if (action=='build' || action=='deploy') {
				lchangedVersion.version = version.version
			}else if (action=="addFix"){
				def tmp = addVersion(version.version,1)
				lchangedVersion.version = tmp.toString()
			}else if (action=="addHotfix"){
				def aVersion = version.version.split("-")
				def hotfix=1
				if (aVersion.length>1){
					try{
						hotfix=aVersion[1].toInteger()+1
					}catch(NumberFormatException e){
						throw new NumberFormatException("Formato de versión (${version.version}) no permite addHotfix")
					}
				}
				lchangedVersion.version="${aVersion[0]}-${hotfix}"
			}else if (action=="removeSnapshot"){
				lchangedVersion.version=removeSnapshot(version.version)
			}else if (action=="addSnapshot"){
				def tmp = addVersion(version.version, releaseMantenimiento?1:2)
				tmp << "-SNAPSHOT"
				lchangedVersion.version = tmp.toString()
			}
			log.log "\n>> Nueva versión: ${lchangedVersion}\n"
		}		
		return lchangedVersion
	}
	
	def removeSnapshot(version){
		if (changeVersion)
			version = setDigits(version,4)
		def aVersion = version.split("-")
		if ( aVersion.length!=2 || aVersion[1] != "SNAPSHOT" ) 
			throw new NumberFormatException("Formato de versión (${version}) no permite removeSnapshot")
		return aVersion[0]
	}

	File getChangedfile(){
		File changed = new File("${parentWorkspace}/changed.txt")
		if (changed.exists()) {
			assert changed.delete()
			assert changed.createNewFile()
		}
		return changed
	}

	/**
	 * Chequea que el número de digitos de la versión es el adecuado y si no lo es lo arregla automáticamente.
	 */
	def setDigits(version, digits){
		def versionRes = ""
		def tmpVersion = version.split("-")
		def aVersion = tmpVersion[0].split("\\.")
		def size = aVersion.size()
		def sizeTmp = tmpVersion.size()
		--digits
		for (i in 0..digits){
			versionRes += i<size?aVersion[i]:"0"
			versionRes += i<digits?".":""
		}

		if (sizeTmp>1){
			for (i in 1..sizeTmp-1){
				versionRes += "-${tmpVersion[i]}"
			}
		}
		return versionRes
	}

	def addVersion(version,posicion){
		def tmpVersion = version.split("-")
		def aVersion = tmpVersion[0].split("\\.")
		def z = aVersion.length-posicion
		def tmp = new StringBuffer()
		aVersion.eachWithIndex() { obj, i ->
			if (i==z) {
				try{
					tmp << "${obj.toInteger()+1}"
				}catch(NumberFormatException e){
					throw new NumberFormatException("Formato de versión (${version}) no permite addVersion")
				}
			}else if (i>z){
				tmp << "0"
			}else{
				tmp << "${obj}"
			}
			if (i!=(aVersion.length-1))
				tmp << "."
		}
		return tmp
	}

	/**
	 * Comprueba que la versión esté abierta o cerrada en función de la acción solicitada
	 * @param action  Distingue las posibles acciones a llevar a cabo por el método.
	 *	addFix: Sería el equivalente al release2release, cambia el dígito 4
	 *	addHotfix: Mantenimiento correctivo de emergencia, cambia el dígito 5 (o lo pone
	 *		si la versión no lo tenía asignado)
	 *	removeSnapshot: Paso 1 del procedimiento de release.  Retira el -SNAPSHOT de las versiones
	 *		de los ficheros de construcción
	 *	addSnapshot: Paso 2 del procedimiento de release.  Una vez hecha y etiquetada en RTC la release,
	 *		incrementa el dígito 3 y pone el -SNAPSHOT para dejar la aplicación preparada para el
	 *		desarrollo de su próxima release.
	 */
	def checkSnapshotVersion(String action, Version version) {
		if (action=="build" || action=="deploy" || action=="removeSnapshot") {
			if (!version.version.endsWith("-SNAPSHOT")) {
				throw new NumberFormatException("La versión del entregable (${version.version}) no es válida; debe tener el siguiente formato: X.X.X.X-SNAPSHOT")
			}
		}
	}
	
	public File getFicheroRoot(){
		if (lficheroRoot==null) {
			log.log "---getFicheroRoot: obteniendo fichero raíz---"
			lficheroRoot = Utiles.getRootFile(parentWorkspace, pattern);

			if (lficheroRoot==null || !lficheroRoot.exists())
				throw new Exception("No se encuentra pom raiz en ${parentWorkspace} con patron ${pattern}")
			log.log "\n>> Fichero raíz: ${lficheroRoot}\n"
		}
		return lficheroRoot;
	}
	
	@Override
	public String toString(){
		StringBuffer res = new StringBuffer();
		res << "\nparentWorkspace: ${parentWorkspace}"
		res << "\naction: ${action}"
		res << "\ncheckSnapshot: ${checkSnapshot}"
		res << "\nchangeVersion: ${changeVersion}"
		res << "\ncheckErrors: ${checkErrors}"
		res << "\nsave: ${save}"
		return res
	}

}
