package rtc

import hudson.model.AbstractBuild
import hudson.model.ParametersAction
import hudson.model.StringParameterValue

import java.io.File;

import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeLogWriter
import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet
import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSetList

import components.ComponentsParser
import es.eci.utils.LogUtils
import es.eci.utils.ScmCommand

/**
 * Busca las dos ultimas lineas base del usuario JENKINS_RTC para mostrar 
 * los cambios de la release note.
 */
class HelperGenerateReleaseNotes {

	private LogUtils log = new LogUtils(null);
	private ScmCommand command = null;
	private boolean compSnapshot = false;

	public initLogger(Closure closure) {
		log = new LogUtils(closure)
	}

	def public generateReleaseNotes (n,stream,component,userRTC,pwdRTC,urlRTC,parentWorkspace,scmToolsHome,daemonConfigDir,build,buildInvoker,ultBL,pultBL) {
		def alias = "aliasLogin"
		String fragmentoConfig = null
		command = new ScmCommand(ScmCommand.Commands.SCM,scmToolsHome,daemonConfigDir);
		if (component == null || component != null && component == "")
			compSnapshot = true;
		try {
			command.initLogger(log.logger)
			fragmentoConfig = command.iniciarSesion(userRTC,pwdRTC,urlRTC,parentWorkspace,alias);
			def baselinesList = this.getLastsBaselinesJenkins(ultBL,pultBL,n,stream,component,parentWorkspace,fragmentoConfig,parentWorkspace,alias)
			def ret = this.compareBaselines(baselinesList,stream,component,parentWorkspace,build,buildInvoker,fragmentoConfig,parentWorkspace,alias,userRTC)
			this.deployReleaseNotes(build,stream,component,parentWorkspace)
		} catch (Exception e) {
			log.logger "Error generando ReleaseNotes: ${e.toString()}"
		}finally {
			command.cerrarSesion(fragmentoConfig,parentWorkspace,alias)
		}
	}

	def private deployReleaseNotes(build,stream,component,parentWorkspace){
		log.log "Inicio del despliegue en Nexus de release notes..."
		def groupId = build.getEnvironment(null).get("PATH_RELEASE_NOTES_NEXUS")
		groupId += "." + stream.replaceAll(" ","_")
		def artifactId = component.replaceAll(" ","_")
		def artifactPath = "${build.workspace}/releaseNotes/changesetReleaseNotes.txt"
		def urlNexus = build.getEnvironment(null).get("NEXUS_RELEASES_URL")
		def mavenEjecutable = build.getEnvironment(null).get("MAVEN_HOME") + "/bin/mvn";
		def versionFile = new File("${build.workspace}/version.txt")
		if (versionFile.exists()) {
			def config = new ConfigSlurper().parse(versionFile.toURL())
			def version = config.version
		}
		def versionFileReleaseNotes = new File("${build.workspace}/releaseNotes/versionReleaseNotes.txt")
		if (versionFileReleaseNotes.exists()){
			versionFileReleaseNotes.append("version=${version}\n")
			versionFileReleaseNotes.append("groupId=${groupId}\n")
			versionFileReleaseNotes.append("artifactId=${artifactId}\n")
			versionFileReleaseNotes.append("artifactPath=${artifactPath}\n")
			versionFileReleaseNotes.append("urlNexus=${urlNexus}\n")
			versionFileReleaseNotes.append("repositoryId=eci\n")
		}
	}

	def private writeReleaseNotes(resultCompare,buildInvoker,build,userRTC){
		log.log "buildInvoker.getRootDir(): ${buildInvoker.getRootDir()}"
		File releaseNotesFile = new File("${build.workspace}/changesetReleaseNotes.txt")
		//releaseNotesFile.getParentFile().mkdirs()
		releaseNotesFile.createNewFile()
		releaseNotesFile.write(resultCompare)
		if (releaseNotesFile!=null && releaseNotesFile.exists()){
			ComponentsParser parser = new ComponentsParser()
			parser.initLogger { log.log it }
			Map changeSetCompare  = parser.parseJazz(new BufferedReader(new InputStreamReader(new FileInputStream(releaseNotesFile),System.getProperty("file.encoding"))))
			List<JazzChangeSet> ret = new ArrayList<JazzChangeSet>()
			// detecta cambios en este componente y los mete en ret
			for (Map.Entry  entry : changeSetCompare.entrySet()) {
				JazzChangeSet chgset1 = ( JazzChangeSet ) entry.getValue()
				if (chgset1.getUser() != userRTC)
					ret.add(chgset1)
			}
			
			if (!ret.isEmpty()) {
				JazzChangeLogWriter writer = new JazzChangeLogWriter()
				writer.write (ret , new OutputStreamWriter(new FileOutputStream("${buildInvoker.getRootDir()}/releaseNotesLog.xml"), "UTF-8"))
				def params = []
				params.add(new StringParameterValue("rutaFicheroReleaseNotesLog","${buildInvoker.getRootDir()}/releaseNotesLog.xml"))
				updateBuildParams(params,build)
			}
		}
		else{
			log.log "WARNING: NO ENCUENTRA FICHERO ${releaseNotesFile}"
		}
		//releaseNotesFile.delete()
	}

	/**
	 * Método que compara dos líneas base (obtenidas en el método getLastsBaselinesJenkins) e invoca
	 * al writeReleaseNotes 
	 * @param baselinesList
	 * @return
	 */
	def private compareBaselines (baselinesList,stream,component,parentWorkspace,build,buildInvoker,fragmentoConfig,baseDir,alias,userRTC) throws Exception{
		def lastBaseline = null
		def penultimateBaseline = null
		def params = []
		def ret = null

		if (baselinesList != null && baselinesList.size() == 2){
			lastBaseline = baselinesList[0]
			penultimateBaseline = baselinesList[1]
			params.add(new StringParameterValue("lastBaseline",lastBaseline.name.toString()))
			params.add(new StringParameterValue("penultimateBaseline",penultimateBaseline.name.toString()))
			updateBuildParams(params,build)
		}else if (baselinesList != null && baselinesList.size() >= 1){
			lastBaseline = baselinesList[0]
			params.add(new StringParameterValue("lastBaseline",lastBaseline.toString()))
			//build?.actions.add(new ParametersAction(params))
			build.addAction(new ParametersAction(params))
		}

		def resolver = build.buildVariableResolver
		log.log "baseline del build: ${resolver.resolve("lastBaseline")}"
		log.log "Last baseline: ${lastBaseline.toString()}"
		log.log "Penultimate baseline: ${penultimateBaseline.toString()}"

		if (lastBaseline != null && penultimateBaseline != null && !compSnapshot){
			def resultCompare = command.ejecutarComando ("compare baseline \"${lastBaseline.identifier}\" baseline \"${penultimateBaseline.identifier}\" -c \"${component}\" -C \"|{name}|{email}|\" -D \"|yyyy-MM-dd-HH:mm:ss|\"",fragmentoConfig,alias,baseDir)
			log.log "${resultCompare}"
			ret = this.writeReleaseNotes(resultCompare,buildInvoker,build,userRTC)
		}else if (lastBaseline != null && penultimateBaseline != null && compSnapshot){
			def resultCompare = command.ejecutarComando ("compare snapshot \"${lastBaseline.identifier}\" snapshot \"${penultimateBaseline.identifier}\" -C \"|{name}|{email}|\" -D \"|yyyy-MM-dd-HH:mm:ss|\"",fragmentoConfig,alias,baseDir)
			log.log "${resultCompare}"
			ret = this.writeReleaseNotes(resultCompare,buildInvoker,build,userRTC)
		}
		
		log.log "***** Termina de generar release notes"
		return ret
	}

	/**
	 * Metodo que obtiene las ultimas n lineas base a partir de un repositorio, una corriente y un componente dados como parï¿½metro. 
	 * @param n
	 * @param repositorio
	 * @param stream
	 * @param componente
	 * @return
	 */
	def private getLastsBaselinesJenkins(String ultBL,String pultBL,String n,String stream,String component,File parentWorkspace,String fragmentoConfig,File baseDir,String alias) throws Exception{
		def lastBaseline = null
		def penultimateBaseline = null
		def fileTmp = null
		log.log "Comapracion entre Snapshots: \"${compSnapshot}\" "
		if (compSnapshot && stream != null){
			fileTmp = command.ejecutarComando ("list snapshots -m \"${n}\" \"${stream}\" ",fragmentoConfig,alias,baseDir)
		} else {
			fileTmp = command.ejecutarComando ("list baselines -m \"${n}\" -C \"${component}\" -w \"${stream}\" ",fragmentoConfig,alias,baseDir)
		}
		def fileBaselines = []
		fileTmp.eachLine { line -> fileBaselines << line }
		def line = 0
		// Reagrupar las líneas ya que a veces RTC las parte
		List<StringBuilder> lineas = new LinkedList<StringBuilder>()
		// Lista que reagrupa las lineas cuando se están comparando snapshots en vez de baselines (cuando component está vacío)
		List<StringBuilder> lineasCompSnapshot = new LinkedList<StringBuilder>()
		int index = 0
		StringBuilder lineaActual = null
		while (index < fileBaselines.size()) {
			String lTemp = fileBaselines.get(index)
			if (lTemp.startsWith("  Baseline:") || lTemp.startsWith("Component:")) {
				lineaActual = new StringBuilder(lTemp)
				lineas << lineaActual
			}else if (compSnapshot && stream != null && lTemp != null){
				lineaActual = new StringBuilder(lTemp)
				lineasCompSnapshot << lineaActual
			}else if (lineaActual != null) {
				// Le añade el texto
				lineaActual.append(lTemp)
			}
			index++
		}
		
		if (lineas!= null)
			log.log "tamaño lineas: ${lineas.size}"
		if (lineasCompSnapshot!= null)
			log.log "tamaño lineasCompSnapshot: ${lineasCompSnapshot.size}"
		if (lineas != null && lineas.size() > 0){
			lineas.each { sb ->
				def baselineRTC = sb.toString()
				log.log "Linea ${line}: $baselineRTC"
				line++
				if (lastBaseline == null || (lastBaseline != null && lastBaseline.user == null)){
					lastBaseline = obtainInformationBaseline(ultBL,baselineRTC,lastBaseline,penultimateBaseline)
				}else if (penultimateBaseline == null || (penultimateBaseline != null && penultimateBaseline.user == null)){
					penultimateBaseline = obtainInformationBaseline(pultBL,baselineRTC,lastBaseline,penultimateBaseline)
				}
			}
		}else if (lineasCompSnapshot != null && lineasCompSnapshot.size() > 0){
			lineasCompSnapshot.each { sb ->
				log.logger "***** ENTRO"
				def baselineRTC = sb.toString()
				log.log "Linea ${line}: $baselineRTC"
				line++
				if (lastBaseline == null){
					lastBaseline = obtainInformationBaseline(ultBL,baselineRTC,lastBaseline,penultimateBaseline)
				}else if (penultimateBaseline == null){
					penultimateBaseline = obtainInformationBaseline(pultBL,baselineRTC,lastBaseline,penultimateBaseline)
				}
			}
		}
		def baselinesList = []
		baselinesList.add(lastBaseline)
		baselinesList.add(penultimateBaseline)
		return baselinesList
	}

	def private obtainInformationBaseline(nameBL,baselineRTC,lastBaseline,penultimateBaseline){
		def resultFindIdentifier = null
		def resultFindDescription = null
		def baseline = null
		if (baselineRTC != null && baselineRTC.startsWith("  Baseline:")) {
			def contentBaseline = baselineRTC.replaceFirst("  Baseline: ","")
			def resultFindNumberBaseline = contentBaseline.find(" \\d+ ")
			log.log "${resultFindNumberBaseline}"
			def resultFindIdentifierAndDescription = contentBaseline.findAll("\".*?\"")
			log.log "${resultFindIdentifierAndDescription}"
			if (resultFindIdentifierAndDescription != null && resultFindIdentifierAndDescription.size() >= 2){
				resultFindIdentifier = resultFindIdentifierAndDescription[0]
				resultFindDescription = resultFindIdentifierAndDescription[1]
			}
			def resultFindUser = (contentBaseline.indexOf("JENKINS_RTC")!=-1)?"JENKINS_RTC":null
			log.log "${resultFindUser}"
			def resultFindDate = null
			log.log "${resultFindDate}"
			if (contentBaseline != null && contentBaseline.size() >= 0){
				baseline = new Baseline()
				baseline.number = resultFindNumberBaseline.replaceFirst(" ","")
				log.log "Number baseline: ${baseline.number}"
				if (resultFindIdentifier != null){
					baseline.name = resultFindIdentifier.replaceAll("^\"","")
					baseline.name = baseline.name.replaceAll("\"\$","")
					log.log "Obteniendo el identificador a partir de la línea base -> " + baselineRTC
				}
				// Obtener el identificador ï¿½nico
				def patron = /Baseline: \(([\d]+)\) [^"]+"([^"]+)"[^"]+"([^"]*)".*/
				def cadena = baselineRTC.trim()
				def matcher = ( cadena =~ patron )
				if (matcher.matches()) {
					String idUnico = matcher[0][1]
					log.log "Se cumple el patr�n, asignando identificador <- ${idUnico}"
					baseline.identifier = idUnico
				}
				if (resultFindDescription != null){
					baseline.description = resultFindDescription
					log.log "Description baseline: ${baseline.description}"
					baseline.user = resultFindUser
					log.log "User baseline: ${baseline.user}"
				}
				if (resultFindDate != null){
					componentsDate = resultFindDate.split()
					baseline.month = componentsDate[0]
					log.log "Month baseline: ${baseline.month}"
					baseline.day = componentsDate[1].replaceFirst(",","")
					log.log "Day baseline: ${baseline.day}"
					baseline.year = componentsDate[2]
					log.log "Year baseline: ${baseline.year}"
					baseline.hour = componentsDate[3]
					log.log "Hour baseline: ${baseline.hour}"
					baseline.typeHour = componentsDate[4]
					log.log "Type hour baseline: ${baseline.typeHour}"
				}

				if (nameBL != null && nameBL != "" && nameBL != baseline.name){
					log.log "Al no coincidir con la linea base introducida se ignora."
					baseline = null
				}else {
					if (baseline.user != null && !baseline.user.equals("JENKINS_RTC")){
						log.log "Al no ser la linea base del usuario JENKINS_RTC se ignora."
						baseline = null
					}
				}
			}
		}else if (baselineRTC != null && !baselineRTC.startsWith("Component:") && !compSnapshot) {
			String contentBaseline = baselineRTC.replaceFirst("  Baseline: ","").split()
			log.log "Components baseline: ${contentBaseline}"
			def descriptionBaseline
			def userBaseline
			def monthBaseline
			def dayBaseline
			def yearBaseline
			def hourBaseline
			def typeHourBaseline
			if (contentBaseline != null && contentBaseline.size() >= 0){
				descriptionBaseline = contentBaseline.find(".*?\"")
				log.log "${descriptionBaseline}"
				userBaseline = contentBaseline.find("\"  [a-zA-Z����������\\d _-]*  ")
				userBaseline = userBaseline.replaceFirst("\"","")
				userBaseline = userBaseline.replaceAll("  ","")
				log.log "${resultFindUser}"
				def resultFindDate = contentBaseline.find("[a-zA-Z]{3} \\d*\\d, \\d{4} \\d*\\d:\\d{2}:\\d{2} [A-Z]{2}")
				log.log "${resultFindDate}"
				if (resultFindDate != null){
					log.log "User baseline: ${userBaseline}"
					def componentsDate = resultFindDate.split()
					monthBaseline = componentsDate[0]
					log.log "Month baseline: ${monthBaseline}"
					dayBaseline = componentsDate[1].replaceFirst(",","")
					log.log "Day baseline: ${dayBaseline}"
					yearBaseline = componentsDate[2]
					log.log "Year baseline: ${yearBaseline}"
					hourBaseline = componentsDate[3]
					log.log "Hour baseline: ${hourBaseline}"
					typeHourBaseline = componentsDate[4]
					log.log "Type hour baseline: ${typeHourBaseline}"
				}
			}
			if (penultimateBaseline == null && lastBaseline != null && lastBaseline.user == null){
				lastBaseline.description += descriptionBaseline
				lastBaseline.user = userBaseline
				lastBaseline.month = monthBaseLine
				lastBaseline.day = dayBaseLine
				lastBaseline.year = yearBaseLine
				lastBaseline.hour = hourBaseLine
				lastBaseline.typeHour = typeHourBaseLine
				if (baseline.user != null && !baseline.user.equals("JENKINS_RTC")){
					log.log "Al no ser la linea base del usuario JENKINS_RTC se ignora."
					lastBaseline = null
				}
				return lastBaseline
			}else if (penultimateBaseline != null && penultimateBaseline.user == null){
				penultimateBaseline.description += descriptionBaseline
				penultimateBaseline.user = userBaseline
				penultimateBaseline.month = monthBaseLine
				penultimateBaseline.day = dayBaseLine
				penultimateBaseline.year = yearBaseLine
				penultimateBaseline.hour = hourBaseLine
				penultimateBaseline.typeHour = typeHourBaseLine
				if (baseline.user != null && !baseline.user.equals("JENKINS_RTC")){
					log.log "Al no ser la linea base del usuario JENKINS_RTC se ignora."
					penultimateBaseline = null
				}
				return penultimateBaseline
			}
		}else if (baselineRTC != null && !baselineRTC.startsWith("Component:") && compSnapshot){
			String contentBaseline = baselineRTC
			def descriptionBaseline
			def name
			def monthBaseline
			def dayBaseline
			def yearBaseline
			def hourBaseline
			def typeHourBaseline
			if (contentBaseline != null && contentBaseline.size() >= 0){
				descriptionBaseline = contentBaseline.find(".*?\"")
				descriptionBaseline = descriptionBaseline.trim()
				descriptionBaseline = descriptionBaseline.substring(1,descriptionBaseline.length()-3)
				descriptionBaseline = descriptionBaseline.trim()
				log.log "${descriptionBaseline}"
				def resultFindIdentifierAndDescription = contentBaseline.findAll("\".*?\"")
				log.log "${resultFindIdentifierAndDescription}"
				if (resultFindIdentifierAndDescription != null){
					resultFindIdentifier = resultFindIdentifierAndDescription[0]
				}
				if (resultFindIdentifier != null){
					name = resultFindIdentifier.replaceAll("^\"","")
					name = name.find("\\d{2}.\\d{2}.\\d{2}.\\d{2}")
				}
				log.logger "${name}"
				def resultFindDate = contentBaseline.find("[a-zA-Z]{3} \\d*\\d, \\d{4} \\d*\\d:\\d{2} [A-Z]{2}")
				log.log "${resultFindDate}"
				if (resultFindDate != null){
					def componentsDate = resultFindDate.split()
					monthBaseline = componentsDate[0]
					log.log "Month baseline: ${monthBaseline}"
					dayBaseline = componentsDate[1].replaceFirst(",","")
					log.log "Day baseline: ${dayBaseline}"
					yearBaseline = componentsDate[2]
					log.log "Year baseline: ${yearBaseline}"
					hourBaseline = componentsDate[3]
					log.log "Hour baseline: ${hourBaseline}"
					typeHourBaseline = componentsDate[4]
					log.log "Type hour baseline: ${typeHourBaseline}"
				}

				baseline = new Baseline()
				baseline.name = name
				baseline.identifier = descriptionBaseline
				baseline.month = monthBaseline
				baseline.day = dayBaseline
				baseline.year = yearBaseline
				baseline.hour = hourBaseline
				baseline.typeHour = typeHourBaseline

				if (nameBL != null && nameBL != "" && nameBL != baseline.name){
					log.log "Al no coincidir con la linea base introducida se ignora."
					baseline = null
				}
			}

		}
		return baseline
	}

	def private updateBuildParams (params,build){
		def paramsIn = build?.actions.find{ it instanceof ParametersAction }?.parameters
		def index = build?.actions.findIndexOf{ it instanceof ParametersAction }
		def paramsTmp = []
		if (paramsIn!=null){
			//No se borra nada para compatibilidad hacia atrás.
			paramsTmp.addAll(paramsIn)
			//Borra de la lista los paramaterAction
			build?.actions.remove(index)
		}
		paramsTmp.addAll(params)
		build?.actions.add(new ParametersAction(paramsTmp))
	}
}
