package checking

//$JENKINS_HOME/jobs/ScriptsStore/workspace/workFlowChecking/AnalisisCheckingRPC.groovy
import groovy.net.xmlrpc.*  
import java.text.DecimalFormat

def parseResp(def text) {
    println text
    if (text.length == 0) {
		return new DecimalFormat("##.###").format("0".toFloat())
	} else {
		return new DecimalFormat("##.###").format(text.toFloat())	
	}
}

/**
 * args[0] - Nombre del proyecto.
 * args[1] - Nombre de la cadena a ejecutar previamente creada en checking.
 * args[2] - url Repositorio RPC
 * args[3] - UserChecking
 * args[4] - PasswordChecking
 * args[5] - publishReportChecking
 * args[6] - publishReportPath
 * args[7] - publishReportURL
 * args[8] - publishReportZip
 * args[9] - technology
 * args[10] - encoding (opcional)
  */

try{
	
	def inicio = new Date()
	def proxyLogin = new XMLRPCServerProxy(args[2] + "/xmlrpc/login");
	def proxyExec  = new XMLRPCServerProxy(args[2] + "/xmlrpc/scriptExec");
	def encoding = args[10]
	def mapResult = null
	def resultadoReportXML = null
	
	login = proxyLogin.login.login(args[3], args[4]);
	println "login ok: ${login!=null}"
	mapParam = ["project":args[0],"encoding":encoding];
	try {
		mapResult = proxyExec.scriptExec.executeChain(login, args[1], mapParam);
	}catch (Exception e){
		/*El número de reintentos y el tiempo de espera es conveniente que sea configurable
		 *Se hace de esta manera por el modo en el que estaba progamado este groovy que admite 
		 *parámetros con la variable args en lugar de usar parámetros. 
		 */
	
		for (i in 0..25){
			try{
				sleep 100000
				println "Ejecución nº: $i"				
				resultadoReportXML = proxyExec.scriptExec.reportFile(login,"qaking/${args[0]}/report${args[9]}.xml",encoding,false);
				if (resultadoReportXML!=null){
					println "Termina, obtiene XML"
					//Suponemos que es OK
					mapResult = [ result:"OK", out:""]
					break
				}
			}catch(Exception ex){
				println "Ejecución: ${i}(${new Date()}); Error: ${ex.getMessage()}"
			}
		}
	}
	//println "ERR: " + mapResult['err'].length()
	println "mapResult: ${mapResult}"
	println "Cadena ejecutada DONE"
	if (mapResult['err']==null || mapResult['err'].length() == 0){  //-> TODO: Cuando Optimyth nos diga porque siempre devuelve OK la ejecucion.
	
		println "login ok: ${login!=null}"
		println "args[0]: "+args[0]
		if (resultadoReportXML==null)
			resultadoReportXML = proxyExec.scriptExec.reportFile(login,"qaking/"+args[0]+"/report"+args[9]+".xml",encoding,false);
		println "va a por el zip --------------"
		resultado = proxyExec.scriptExec.reportFile(login,"qaking/"+args[0]+"/report_${args[9]}.zip",encoding,true);
		
		if ( args.size()>5 && args[5].equals("true")) {
		
			def zipDirPath = args[6]
			def zipName = args[8]
			zipDestDir = new File(zipDirPath)
			if (!zipDestDir.exists()){
				zipDestDir.mkdirs()
			}
			def zipFile = zipDirPath + "/" + zipName
			def file = new File(zipFile)
			println "Se deja el archivo en:"+zipFile
		
			if (file.exists()) {
				assert file.delete()
				assert file.createNewFile()
			}
			file.append(resultado.decodeBase64())
			File outIHS = new File('ihs.url')
			if (outIHS.exists()) {
				assert outIHS.delete()
				assert outIHS.createNewFile()
			}
			outIHS << args[7]
		}
		
		
		File out = new File('checking_report.html')
		if (out.exists()) {
			assert out.delete()
			assert out.createNewFile()
		}

		def ruleSet = new XmlSlurper().parseText(resultadoReportXML)
		def categorias= ['-':'']
		def cat
	    ruleSet.ResumenGenerico.Confianza.each() {
	    	cat = it.@Categoria
	    	categorias.put("$cat",parseResp(it))	    	
	    } 
	    
	    println categorias
	    
		out<< 		"<html><header><body align='center'>"
		
				
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
		
		out<<		"<p style='text-indent:100px'>Objetivo: "+ruleSet.ResumenGenerico.Target.text()+"</p>"
		out<<		"<p style='text-indent:100px'>Analizador: "+ruleSet.ResumenGenerico.Analyzer.text()+"</p>"
		out<<		"<p style='text-indent:100px'>Conjunto de reglas: "+ruleSet.ResumenGenerico.RuleSet.text()+"</p>"
		out<<		"<p style='text-indent:100px'>Fecha: "+ruleSet.ResumenGenerico.Date.text()+"</p>"
		out<<		"<p style='text-indent:100px'>Duraci\u00F3n: "+ruleSet.ResumenGenerico.ElapsedTime.text()+"</p>"
		out<<		"<p style='text-indent:100px'>Clases Analizadas: "+ruleSet.ResumenGenerico.NClasesAnalizadas.text()+"</p>"
		out<<		"<p style='text-indent:100px'>Violaciones: "+ruleSet.ResumenGenerico.NViolaciones.text()+"</p>"
		out<<		"<p style='text-indent:100px'>Violaciones Suprimidas: "+ruleSet.ResumenGenerico.NViolacionesSuprimidas.text()+"</p>"
		out<<		"<p style='text-indent:100px'>Reglas Activadas: "+ruleSet.ResumenGenerico.NReglasActivadas.text()+"</p>"
		out<<		"<p style='text-indent:100px'>Fecha Ruleset: "+ruleSet.ResumenGenerico.FechaRuleset.text()+"</p>"
		out<<		"<p style='text-indent:100px'>Violaciones Clase: "+ruleSet.ResumenGenerico.NViolacionesClase.text()+"</p>"
		out<<		"<p style='text-indent:100px'>Configuraci\u00F3n Global: "+ruleSet.ResumenGenerico.GlobalConf.text()+"</p>"
		
		

	
		/*if (ruleSet.ResumenGenerico.Confianza.size()==0) {
			println 'Resultado NOK'
			System.exit(1);
		} else {
			println 'Resultado OK'
		}*/	 
		return 0;
	}else{
		println 'Resultado NOK'
		println mapResult['err'];
		//System.exit(1);	
		return 1;
	}	

}catch(ArrayIndexOutOfBoundsException array){
	println 'Consulte los parámetros de entrada al script. \n   Argumento 1: Nombre del proyecto \n   Argumento 2: Nombre de la cadena a ejecutar.'
	//System.exit(1);
	return 1;
}