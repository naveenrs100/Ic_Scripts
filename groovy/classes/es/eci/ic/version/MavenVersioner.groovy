package es.eci.ic.version

import java.io.File;
import java.util.List;

import es.eci.utils.Utiles
import es.eci.utils.MavenUtils
import groovy.json.*
import groovy.xml.*

class MavenVersioner extends Versioner {

	Boolean fullCheck
	def homeStream
	Boolean fixMavenErrors = false

	def lartifacts
	private lficheros
	
	public MavenVersioner(log, parentWorkspace, checkSnapshot, checkErrors,changeVersion, action, save,homeStream, fullCheck){
		this.log = log
		this.pattern = "pom\\.xml"
		
		this.parentWorkspace = parentWorkspace
		this.action = action
		this.checkSnapshot = Boolean.valueOf(checkSnapshot)
		this.changeVersion = Boolean.valueOf(changeVersion)
		this.checkErrors = Boolean.valueOf(checkErrors)
		this.save = save

		this.homeStream = homeStream
		this.fullCheck = Boolean.valueOf(fullCheck)
		
		this.log.log this
	}
	
	
	/**
	 * Hace las operaciones necesarias para, según la fase de construcción, quitar los
	 * 	-SNAPSHOT, incrementar el número de versión correspondiente y/o volver a poner
	 * 	los -SNAPSHOT de la versión en los ficheros de construcción.  La versión se define
	 *	con el formato 1.2.3.4(-5)(-SNAPSHOT), siendo:
	 *	1 y 2: definidos por el usuario
	 *	3: release
	 *	4: fix (antiguo release2release)
	 *	5: (opcional) hotfix
	 *	-SNAPSHOT: se mantiene mientras la versión se encuentre en desarrollo, se retira
	 *		al realizar una release
	 * Por ejemplo: 21.0.0.0-SNAPSHOT, al hacer una release se sube a RTC los pom.xml con
	 *	versión 21.0.0.0, y acto seguido se vuelven a modificar para dejar en desarrollo
	 *	la siguiente versión: 21.0.1.0-SNAPSHOT.  Este método opera sobre ficheros pom.xml
	 * (tecnología maven solamente)
	 * @param ficheros Lista de ficheros de construcción de la aplicación (en este caso,
	 * 	ficheros pom.xml solamente)
	 * @param version Versión original declarada en el pom raíz
	 * @param newVersion Versión calculada como nueva versión dependiendo de la acción que se
	 * 	esté llevando a cabo
	 * @param action Distingue las posibles acciones a llevar a cabo por el método.
	 *	addFix: Sería el equivalente al release2release, cambia el dígito 4
	 *	addHotfix: Mantenimiento correctivo de emergencia, cambia el dígito 5 (o lo pone
	 *		si la versión no lo tenía asignado)
	 *	removeSnapshot: Paso 1 del procedimiento de release.  Retira el -SNAPSHOT de las versiones
	 *		de los ficheros de construcción
	 *	addSnapshot: Paso 2 del procedimiento de release.  Una vez hecha y etiquetada en RTC la release,
	 *		incrementa el dígito 3 y pone el -SNAPSHOT para dejar la aplicación preparada para el
	 *		desarrollo de su próxima release.
	 * @param parentWorkspace Ubicación del workspace del job padre del que nos ha invocado,
	 *	donde el código groovy asume que va a encontrar los fuentes
	 * @param homeStream Corriente RTC que aloja el código
	 * @param changeVersion Indica si el método actualiza el formato de versión
	 *	antiguo (3 dígitos) al nuevo (4 + hotFix opcional).  Vale 'true' o bien 'false'
	 * @param fullCheck Booleano que indica que la comprobación de los artefactos debe hacerse sobre el
	 * 	total de componentes desplegados en el homestream
	 */
	@Override
	def doOnWrite() {
		log.log "---doOnWrite---"
		def changed = getChangedfile()
		def ficheroRoot = getFicheroRoot()
		def changedVersion = getChangedVersion()
		
		// El pom raíz define una versión.  Si está definida como una propiedad, se guarda
		//	en esta variable
		def mainImplicitVersion = null
		
		getFicheros().each { fichero ->
			def hayCambios = false
			try {
				def pom = new XmlParser().parse(fichero)
				// Se debe determinar si la versión está explícita o bien
				//	hace referencia a una propiedad definida en el padre.
				//	En tal caso, solo se actualiza la propiedad, y en caso de
				//	que sea sustituible de forma sencilla, es decir:
				//  <version>${tpvVersion}</version>
				// 	...
				// 	<properties><tpvVersion>21.0.0.0</tpvVersion></properties>
				//  Sería fácilmente sustituible: en el pom.xml donde está tpvVersion,
				//	se reemplaza la propiedad, y en todos los demás no se hace nada.
				//	Sin embargo, algo así:
				//	<version>${tpvVersion}-${calificador}</version>
				//	Daría una excepción al no tener una propiedad que se pueda actualizar
				//	fácilmente (por mucho que el build funcionase en este caso)
				boolean explicita = MavenUtils.isExplicitVersion(pom);
				if (!explicita) 
					MavenUtils.validateNotExplicitVersion(pom);
	
				if (action=="removeSnapshot") 
					pom = this.removeSnapshotDependencies(pom)
	
				if (ficheroRoot.getCanonicalPath()==fichero.getCanonicalPath()){
					if (pom.version != null && pom.version.size() > 0 && pom.version[0]!=null) {
						if (explicita){
							pom.version[0].setValue(changedVersion.version)
							hayCambios = true
							log.log("Write versión raíz explícita $changedVersion.version en $fichero")
						}else{
							String laVersion = pom.version[0].text().substring(2, pom.version[0].text().length() - 1);
							mainImplicitVersion = laVersion
							if (MavenUtils.lookupProperty(pom, laVersion) != null) {
								// Si el pom contiene la propiedad, sustituirla
								MavenUtils.setProperty(pom, laVersion, changedVersion.version);
								hayCambios = true
								log.log("Write versión raíz implícita $changedVersion.version en la propiedad $mainImplicitVersion en $fichero")
							}
						}
					}else{
						throw new Exception("El pom raiz no tiene tag version");
					}
				}else{
					if (pom.version != null && pom.version.size() > 0 && pom.version[0]!=null) {
						pom.remove(pom.version[0])
						hayCambios = true
					}
					if (explicita) {
						pom.parent.version[0].setValue(changedVersion.version)
						hayCambios = true
						log.log "Write ${changedVersion.version} in ${fichero}"
					}
				}
				if (hayCambios) {
					XmlUtil.serialize(pom, new FileWriter(fichero))
					changed << "${fichero}\n"
				}
			}catch(Exception e) {
				log.log("AVISO: el fichero $fichero no es parseable -> " + e.getMessage())
			}
		}
		
		if (action=="removeSnapshot" || action=="addSnapshot" || action=="addFix") {
			// El fichero pom raíz puede tener dependencias a otros módulos de la
			//	aplicación definidas como propiedades.  En este caso, se debe distinguir
			//	qué propiedades del pom raíz son dependencias a otros módulos y
			//	aplicarles la acción necesaria (sea quitarle el snapshot o volver a ponérselo
			//	subiendo el tercer dígito)
			def versionesModulos = getImplicitMavenModuleDependencies()
			if (versionesModulos != null && versionesModulos.size() > 0) {
				log.log("Versiones de los módulos definidas como propiedad:")
				versionesModulos.each { propiedad -> log.log(propiedad + (mainImplicitVersion == propiedad?" (PRINCIPAL)":" (DEPENDENCIA)")) }
				def pomRaiz = new XmlParser().parse(ficheroRoot)
				versionesModulos.each { propiedad ->
					if ('parent.version' != propiedad && 'project.version' != propiedad && mainImplicitVersion != propiedad) {
						def versionOriginal = MavenUtils.lookupProperty(pomRaiz, propiedad)
						log.log("Actualizando la propiedad $propiedad al valor $changedVersion.version")
						MavenUtils.setProperty(pomRaiz, propiedad, changedVersion.version)
						XmlUtil.serialize(pomRaiz, new FileWriter(ficheroRoot))
					}
				}
			}
		}
		
		log.log "\n>> doOnWrite Terminado!!\n"
	}

	/**
	 * Este método recupera la versión del pom.xml que hemos determinado es el
	 * padre de todo el proyecto.  Se determina que el pom padre del proyecto es
	 * aquel que encontremos a la altura del componente, o el que se encuentre como
	 * máximo una altura por debajo.  Si el primer nivel estuviera vacío y hubiera
	 * varios candidatos en el segundo nivel, se devuelve un error.
	 * @param ficheros Listado de ficheros maven obtenidos desde el origen
	 * @param parentWorkspace Ubicación del workspace del job invocante, donde
	 *	asumimos que encontraremos los fuentes
	 * @param checkSnapshot Si es cierto, se procede a comprobar que las
	 *	dependencias no apunten a ningún proyecto en desarrollo (con -SNAPSHOT)
	 * @param checkErrors Si vale 'true', el método es tolerante a errores
	 *	de formato o bien a que exista más de un fichero de construcción de
	 *	primer nivel
	 * @param homeStream Corriente RTC que aloja el código
	 * @param fullCheck Booleano que indica que la comprobación de los artefactos debe hacerse sobre el
	 * 	total de componentes desplegados en el homestream
	 */
	@Override
	public Version getVersion() {
		if (lversion==null){
			log.log "---getVersion: obteniendo versión---"
			def versionCandidate = null
			def baseDepth = Utiles.getDepth(parentWorkspace)
			def err = new StringBuffer()
			def n = 0
			def l = -1
	
			getFicheros().each { fichero ->
				try {
					def pom = new XmlParser().parse(fichero)
					if (checkSnapshot) {
						err = mavenSnapshotCheck(fichero,err)
					}
					def depth = Utiles.getDepth(fichero)
	
					if (depth == baseDepth) {
						// Me quedo con éste
						lversion = new Version();
						lversion.version = MavenUtils.solve(pom, pom.version.text())
						lversion.groupId = pom.groupId.text()
						log.log "Version from: ${fichero} -> ${version.version}"
					}else if (depth == baseDepth + 1) {
						// Considerar el candidato
						versionCandidate = new Version()
						versionCandidate.version = MavenUtils.solve(pom, pom.version.text())
						versionCandidate.groupId = pom.groupId.text()
						log.log "Version candidate from: ${fichero} -> ${versionCandidate.version}"
						n++
					}
				}catch(Exception e) {
					log.log "Error parseando ${fichero} -> ${e.getMessage()}"
				}
			}
			if (lversion == null && versionCandidate == null)
				err << "There is no root pom.xml in this component!!"
				
			if (lversion == null && versionCandidate != null && n>1) 
				err << "There are more than one root pom.xml in this component!!!"
			
			if (err.length()>0 && checkErrors)
				throw new NumberFormatException(err.toString())
			
			// Si no hay versión en el primer nivel y sí tenemos un candidato en el
			//	segundo, tomar la versión
			if (lversion == null && versionCandidate != null && n == 1)
				lversion = versionCandidate
	
			mavenVersionCheck(lversion)
			
			log.log "\n>> Versión: ${version}\n"
		}
		return lversion
	}
	
	
	/**
	* Recorre el pom buscando las dependencias del módulo.  Si la dependencia
	* alude a un artefacto que forma parte del propio proyecto, y se refiere al
	* mismo en una versión abierta, la reemplaza por una versión cerrada de la misma.
	* @param pom Fichero pom.xml expresado como XML parseado por groovy
	* @param artifacts Lista de artefactos deducida de leer el conjunto de ficheros pom.xml
	* @param changeVersion Indica si el método actualiza el formato de versión
	*	antiguo (3 dígitos) al nuevo (4 + hotFix opcional).  Vale 'true' o bien 'false'
	*/
	private removeSnapshotDependencies(pom){
		def artifacts = getArtifacts()
		pom.dependencies.dependency.each { dependency ->
			def version = dependency.version.text()
			if (version!=null){
				if (version.toLowerCase().indexOf("snapshot")>0){
					if (artifacts.find {a -> a.artifactId == dependency.artifactId.text() && a.groupId == dependency.groupId.text()}!=null){
						dependency.version[0].setValue(removeSnapshot(version))
					}
				}
			}
		}
		return pom
	}
	
	private mavenVersionCheck(version){
		getFicheros().each { fichero ->
			try {
				def pom = new XmlParser().parse(fichero)
				def testVersion = pom.packaging.text()=="pom"?pom.version.text():pom.parent.version.text()
				if (testVersion!=version.version){
					log.log "WARNING: The parent version in ${fichero} is not the parent version of this component: ${version.groupId} - ${version.version}\n"
					fixMavenErrors = true
				}
			}
			catch(Exception e) {
				log.log("AVISO: el fichero $fichero no es parseable -> " + e.getMessage())
			}
		}
	}
	
	/**
	 * Comprueba que no hay ninguna versión snapshot
	 */
	private mavenSnapshotCheck(fichero,err){
		def pom = new XmlParser().parse(fichero)
		def artifacts = getArtifacts()
		pom.dependencies.dependency.each { dependency ->
			def version = dependency.version.text()
			if (version!=null){
				if (version.toLowerCase().indexOf("snapshot")>0){
					if (artifacts.find {a -> a.artifactId == dependency.artifactId.text() && a.groupId == dependency.groupId.text()}==null){
						log.log "${dependency.artifactId.text()} - ${dependency.groupId.text()} ---> ${version}"
						//err << "There is a snapshot version in ${dependency.artifactId.text()} inside ${fichero}\n"
						err << "ERROR: la dependencia a ${dependency.groupId.text()}:${dependency.artifactId.text()}:${version} dentro de ${fichero} no puede contener -SNAPSHOT\n"
					}
				}
			}
		}
		return err
	}
 	
	private getArtifactsStream(home){
		def artifactsFile = new File("${home}/artifacts.json")
		def artifacts = []
		if (artifactsFile.exists()){
			def text = new StringBuffer()
			artifactsFile.eachLine { line -> text << line}
			artifacts = new JsonSlurper().parseText(text.toString())
		}
		return artifacts
	}

	private getArtifacts(){
		if (lartifacts==null){
			log.log "---getArtifacts: obteniendo artifacts---"
			lartifacts = []
			// Si el proceso se lanza desde componente, nunca existe el directorio homeStream
			if (new File(homeStream).exists()) {
				def ficheros = fullCheck?getFicheros():Utiles.getAllFiles(homeStream, pattern)
				lartifacts = getArtifactsStream(homeStream)
			}
			if (lartifacts.size()==0) lartifacts = getArtifactsComponent(ficheros,parentWorkspace)
			log.log "\n>> Número artifacts: ${lartifacts.size()}\n"
		}
		return lartifacts
	}

	private getArtifactsComponent(ficheros,home){
		MavenUtils.processSnapshotMaven(ficheros,home)
		return getArtifactsStream(home)
	}
	
	
	private getFicheros(){
		if (lficheros==null){
			log.log "---getFicheros: obteniendo ficheros---"
			lficheros = Utiles.getAllFiles(parentWorkspace, pattern)
			if (lficheros.size()==0)
				throw new NumberFormatException("There is no pom.xml file in ${parentWorkspace}")
			log.log "\n>> Número ficheros: ${lficheros.size()}\n"
		}
		return lficheros
	}
	

	/**
	 * Este método recorre la lista de ficheros pom.xml buscando aquellas dependencias
	 * que estén definidas de forma implícita, si corresponden a módulos dentro del mismo
	 * proyecto.
	 * @param ficheros Lista de ficheros pom.xml que compone la aplicación
	 * @param artifacts Lista de artefactos incluídos en la aplicación
	 */
	def List<String> getImplicitMavenModuleDependencies() {
		def ret = []
		def artifacts = getArtifacts()
		getFicheros().each { fichero ->
			try {
				def pom = new XmlParser().parse(fichero)
				pom.dependencies.dependency.each { dependency ->
					// ¿Es un artefacto de la aplicación?
					if (artifacts.find {a -> a.artifactId == dependency.artifactId.text() && a.groupId == dependency.groupId.text()}!=null){
						def version = dependency.version.text()
						if (version.startsWith('${') && version.endsWith('}')) {
							def propiedad = version.substring(2, version.length() - 1)
							if (!propiedad.startsWith("project.")) {
								// Apuntamos la propiedad para cerrarla e incrementarla luego
								if (!ret.contains(propiedad)) {
									ret << propiedad
								}
							}
						}
					}
				}
			}catch(Exception e) {
				log.log("AVISO: el fichero $fichero no es parseable -> " + e.getMessage())
			}
		}
		return ret;
	}
	
	@Override
	public String toString(){
		StringBuffer res = new StringBuffer();
		res << "\n---- MAVEN VERSIONER ----"
		res << "\nfullCheck: ${fullCheck}"
		res << "\nhomeStream: ${homeStream}"
		res << "\nfixMavenErrors: ${fixMavenErrors}"
		res << super.toString()
		res << "\n-------------------------\n"
		return res
	}
}
