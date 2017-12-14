package checking

import groovy.net.xmlrpc.XMLRPCServerProxy
import hudson.model.*
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date

//Obtiene parametros pasados al Job
def urlChecking =  build.getEnvironment(null).get("urlChecking")
def userChecking = build.getEnvironment(null).get("userChecking")
def IHSurl = build.getEnvironment(null).get("IHS_URL")
def IHSBaseDir =  build.getEnvironment(null).get("CHECKING_REPORTS_PATH")
def pwdChecking	= build.buildVariableResolver.resolve("pwdChecking")
//---------- Parametros pasados directamente
def jobInvoker = build.buildVariableResolver.resolve("jobInvoker")
def workspaceRTC = build.buildVariableResolver.resolve("workspaceRTC")
def component = build.buildVariableResolver.resolve("component")
def stream = build.buildVariableResolver.resolve("stream")
def recrear = build.buildVariableResolver.resolve("recrear")
def parentWorkspace = build.buildVariableResolver.resolve("parentWorkspace")
if (parentWorkspace==null) parentWorkspace = build.getEnvironment(null).get("WORKSPACE")
def template = build.buildVariableResolver.resolve("template")
def checkingModel = build.buildVariableResolver.resolve("checkingModel")
def cadenaChecking = build.buildVariableResolver.resolve("cadenaChecking")
def tecnology = build.buildVariableResolver.resolve("tecnology")
def retries = build.buildVariableResolver.resolve("retries")
def publishReportChecking = build.buildVariableResolver.resolve("publishReportChecking")
def compJobNumber = build.buildVariableResolver.resolve("compJobNumber")

/**
 * Obtiene una conexión contra el servicio RPC
 */
def getLogin(urlChecking,userChecking,pwdChecking){
	def proxyLogin = new XMLRPCServerProxy(urlChecking+ "/xmlrpc/login")
	def login = proxyLogin.login.login(userChecking,pwdChecking)
	println "login ok: ${login!=null}"
	return login
}


/**
 * Comprueba que el proyecto existe, si recrear = true, lo borra y lo vuelve a crear.
 * Si no existe lo crea nuevo.
 */
def checkProject(login,urlChecking,recrear,nombre,checkingModel,workspaceRTC,component,template){
	def crear = true
	def proxyProject  = new XMLRPCServerProxy(urlChecking+ "/xmlrpc/project")
	def propChecking = proxyProject.project.load( login , template)
	def existe = proxyProject.project.exists( login , nombre)

	println "El proyecto ${nombre} existe: $existe"
	
	if (existe) {
		if (recrear){
			def borrado = proxyProject.project.delete( login , nombre, true)
			println "Proyecto borrado  con: $borrado"
		}else{
			crear=false
			println "Proyecto ${nombre} YA EXISTE, NO SE RECREA"
		}
	}

	if (crear){
		def mapResult = new Hashtable()
		mapResult.put("path","\${CHECKING_SRC}${nombre}")
		mapResult.put("relativeName", nombre)
		mapResult.put("qModel",checkingModel)
		mapResult.put("description","Proyecto creado desde Jenkins")
		mapResult.put("scm",propChecking.get("scm"))
		mapResult.get("scm").put("scmPath","${workspaceRTC}' '${component}")
		mapResult.put("buildInfo",propChecking.get("buildInfo"))
		mapResult.put("type",propChecking.get("type"))
		println "Creando proyecto con parámetros: ${mapResult}"
		def creado = proxyProject.project.create( login , nombre, mapResult)
		println "Proyecto creado con: $creado"
	}
}

/**
 * Lanza la ejecución de la cadena en Checking para realizar el análisis de
 * Código estático.
 *
 * Esta operación pueda tardar mucho tiempo, y ser cortada por un elemento de red indeterminado que está
 * entre medias de los servidores. Si esto pasa la ejecución en el servidor de checking continua.
 *
 * A la hora de obtener los informes hay que esperar, por eso se hacen los reintentos
 */
def launchStaticCodeCheck(login,urlChecking,cadenaChecking,nombre){
	def proxyExec  = new XMLRPCServerProxy(urlChecking + "/xmlrpc/scriptExec")
	def mapParam = ["project":nombre]
	try{
		return proxyExec.scriptExec.executeChain(login, cadenaChecking, mapParam)
	}catch(Exception e){
		//De momento nunca da error.
		println "WARNING: ${e.getMessage()}"
		return [ result:"OK", out:""]
	}
}

/**
 * Obtiene los informes generados por checking.
 *
 * Realiza un numero de intentos por si la ejecución de la cadena no ha terminado.
 */
def getReport(login,urlChecking,nombre,tecnology,retries,isZip){
	def proxyExec  = new XMLRPCServerProxy(urlChecking + "/xmlrpc/scriptExec")
	def resultado = null
	for (i in 0..retries.toInteger()){
		try{
			sleep 100000
			println "execution: n: $i"
			if (isZip){
				resultado = proxyExec.scriptExec.reportFile(login,"qaking/${nombre}/report.zip","UTF-8",true);
			}else{
				resultado = proxyExec.scriptExec.reportFile(login,"qaking/${nombre}/report${tecnology}.xml","UTF-8",false)
			}
			if (resultado!=null){
				break
			}
		}catch(Exception ex){
			println "Execution ${i} failed: (${new Date()}); Error: ${ex.getMessage()}"
		}
	}
	return resultado
}

/**
 * Publica el informe comprimido en ZIP en una carpeta pública de un IHS en la misma máquina que Jenkins.
 */
def makeZipPublic(reportZip,urlFile,zipDirPath,zipName,urlZip){
	zipDestDir = new File(zipDirPath)
	if (!zipDestDir.exists()){
		zipDestDir.mkdirs()
	}
	def zipFile = zipDirPath + "/" + zipName
	def file = new File(zipFile)
	println "Se deja el archivo en: "+zipFile

	if (file.exists()) {
		assert file.delete()
		assert file.createNewFile()
	}
	file.append(reportZip.decodeBase64())

	File outIHS = new File(urlFile)
	if (outIHS.exists()) {
		assert outIHS.delete()
		assert outIHS.createNewFile()
	}
	outIHS << urlZip
}

def formatDate(patron,date){
	DateFormat dateFormat = new SimpleDateFormat(patron);
	return dateFormat.format(date)
}

def formatNumber(def text) {
	println text
	if (text.length == 0) {
		return new DecimalFormat("##.###").format("0".toFloat())
	} else {
		return new DecimalFormat("##.###").format(text.toFloat())
	}
}

def formatNombre(def nombre){
	nombre = nombre.replace(' ','')
	nombre = nombre.replace(')','')
	nombre = nombre.replace('(','')
	return nombre
}

def writeHtmlReport(reportXml,fileName){
	File out = new File(fileName)
	if (out.exists()) {
		assert out.delete()
		assert out.createNewFile()
	}

	def ruleSet = new XmlSlurper().parseText(reportXml)
	def categorias= ['-':'']
	def cat
	ruleSet.ResumenGenerico.Confianza.each() {
		cat = it.@Categoria
		categorias.put("$cat",formatDate(it))
	}
	
	println categorias
	
	out << 		"<html><body align='center'>"
	
			
	out << "<table border=1 align='center'>"
	out << "<tr style='background-color:#00AB47; color:white' border='1'>"
	categorias.each() {
		if (it.getKey()!='-') {
			out << 		"<td>" +  it.getKey() + "</td>"
		}
	}
	out << "</tr>"
	out << "<tr>"
	categorias.each() {
		if (it.getKey()!='-') {
			out << 		"<td>" +  it.getValue() + "</td>"
		}
	}
	out << "</tr>"
	out << "</table>"
	
	out <<		"<p style='text-indent:100px'>Objetivo: "+ruleSet.ResumenGenerico.Target.text()+"</p>"
	out <<		"<p style='text-indent:100px'>Analizador: "+ruleSet.ResumenGenerico.Analyzer.text()+"</p>"
	out <<		"<p style='text-indent:100px'>Conjunto de reglas: "+ruleSet.ResumenGenerico.RuleSet.text()+"</p>"
	out <<		"<p style='text-indent:100px'>Fecha: "+ruleSet.ResumenGenerico.Date.text()+"</p>"
	out <<		"<p style='text-indent:100px'>Duraci\u00F3n: "+ruleSet.ResumenGenerico.ElapsedTime.text()+"</p>"
	out <<		"<p style='text-indent:100px'>Clases Analizadas: "+ruleSet.ResumenGenerico.NClasesAnalizadas.text()+"</p>"
	out <<		"<p style='text-indent:100px'>Violaciones: "+ruleSet.ResumenGenerico.NViolaciones.text()+"</p>"
	out <<		"<p style='text-indent:100px'>Violaciones Suprimidas: "+ruleSet.ResumenGenerico.NViolacionesSuprimidas.text()+"</p>"
	out <<		"<p style='text-indent:100px'>Reglas Activadas: "+ruleSet.ResumenGenerico.NReglasActivadas.text()+"</p>"
	out <<		"<p style='text-indent:100px'>Fecha Ruleset: "+ruleSet.ResumenGenerico.FechaRuleset.text()+"</p>"
	out <<		"<p style='text-indent:100px'>Violaciones Clase: "+ruleSet.ResumenGenerico.NViolacionesClase.text()+"</p>"
	out <<		"<p style='text-indent:100px'>Configuraci\u00F3n Global: "+ruleSet.ResumenGenerico.GlobalConf.text()+"</p>"
	out << 		"</body></html>"
}

//Ejecución
try{
	
	def nombre = "/${formatNombre(jobInvoker)}"
	// Conexión a RPC
	def login = getLogin(urlChecking,userChecking,pwdChecking)
	// Comprueba que el proyecto existe y lo recrea si es necesario
	checkProject(login,urlChecking,recrear,nombre,checkingModel,workspaceRTC,component,template)
						  
	
	// Lanza el análisis de código estático en el servidor de Checking
	println "------------ STATIC CODE CHECK BEGIN (${new Date()}) ----------------"
	def result = launchStaticCodeCheck(login,urlChecking,cadenaChecking,nombre)
	if (result['err']==null || result['err'].length() == 0){

		// obtiene el informe del servidor de checking
		def reportXml = getReport(login,urlChecking,nombre,tecnology,retries,false)
		writeHtmlReport(reportXml,"${parentWorkspace}/checking_report.html")
	
		if (publishReportChecking=="true"){
			def reportZip = getReport(login,urlChecking,nombre,tecnology,retries,true)
			def zipRelativePath = "${tecnology}/${stream.replaceAll(' ','_')}/${component.replaceAll(' ','_')}"
			def zipDirPath = "${IHSBaseDir}/${zipRelativePath}"
			def zipName = "${compJobNumber}_${formatDate('ddMMyyyy_HHmmss',new Date())}.zip"
			def urlZip = "${IHSurl}/${zipRelativePath}/${zipName}"
			makeZipPublic(reportZip,"${parentWorkspace}/ihs.url",zipDirPath,zipName,urlZip)
		}
		
	}else{
		println result['err'];
	}
	println "------------ STATIC CODE CHECK ENDS (${new Date()}) ----------------"
}catch(Exception ex){
	println "Exception: (${new Date()}); Error: ${ex.getMessage()}"
}