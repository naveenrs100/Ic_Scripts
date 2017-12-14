package portal

@GrabResolver(name='nexuseci', root='http://nexus.elcorteingles.int/content/groups/public/')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')
@Grab(group='org.apache.httpcomponents', module='httpmime', version='4.4')
@Grab(group='org.apache.poi', module='poi-ooxml', version='3.10.1')

//$JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/portal/QuveRtcUpdater.groovy
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import groovy.json.*
import groovyx.net.http.HTTPBuilder

// Parametros de entrada

quveurl = args[0]
jenkinsHome = args[1]
workspace = args[2]

//------ funciones --------

def getToken(){
  def sessionKeyFile = new File(jenkinsHome, "portalSessionKey")
  def sessionKey = "";
  sessionKeyFile.eachLine { line ->
	sessionKey = line
	return
  }
  def token = "{\"sessionKey\":\"${sessionKey}\"}"
  return token;
}

def sendQuve(baseurl,path,entity,contentType){
  def query = [:]
  query.put("quvetoken", getToken())
  def http = new HTTPBuilder(baseurl)
  http.request( POST, TEXT ) { req ->
	headers.Accept = 'application/json'
	uri.path = path
	uri.query = query
	requestContentType = contentType
	req.entity = entity
	response.failure = { resp, reader ->
	  if (reader!=null)
		println reader.text
	  throw new Exception("Server Error: ${resp.status}")
	}
	response.success = { resp, reader ->
		return reader.text
	}
  }
}

//-------------------------

def base = new File(workspace)
if (base.exists() && base.isDirectory()){
	def files = []
	base.listFiles().sort{ it.name }.reverse().each { file ->
	  println "Processing ${file.name}..."
		if ("all.json".equals(file.name)){
			def all = new JsonSlurper().parseText(file.getText("UTF-8"))
			def json = new JsonOutput().toJson(all);
			def entity = new StringEntity(json, ContentType.APPLICATION_JSON)
			sendQuve("${quveurl}/","scm",entity,"multipart/form-data")
			// Traducir el fichero all.json a all.xlsx
			File excelFile = new File(base, "all.xlsx")
			Workbook book = new SXSSFWorkbook(); 
			Sheet sheet = book.createSheet("PA");
			Row headers = sheet.createRow(0);
			Cell paHeader = headers.createCell(0);
			Cell streamHeader = headers.createCell(1);
			paHeader.setCellValue("Área de proyecto");
			streamHeader.setCellValue("Corrientes");
			int rowCount = 1;
			all.each { projectArea ->
				// Para cada área de proyecto, buscar los componentes
				String name = projectArea.name;
				String uuid = projectArea.uuid;
				File projectAreaDetailsFile = 
					new File(base, uuid);
				def projectAreaDetails = new JsonSlurper().
					parseText(projectAreaDetailsFile.getText("UTF-8"));
				if (projectAreaDetails != null) {
					projectAreaDetails.each { stream ->
						String streamName = stream.name;
						Row row = sheet.createRow(rowCount++);
						Cell paCell = row.createCell(0);
						paCell.setCellValue(name);
						Cell stCell = row.createCell(1);
						stCell.setCellValue(streamName);
					}
				}
			}
			OutputStream os = new FileOutputStream(excelFile)
			try {
				book.write(os)
			}
			finally {
				if (os != null) {
					os.close();
				}
			}
		}
		else if (file.name.startsWith("_")){
			def one = new JsonSlurper().parseText(file.getText("UTF-8"))
			def json = new JsonOutput().toJson(one);
			def entity = new StringEntity(json, ContentType.APPLICATION_JSON)
			sendQuve("${quveurl}/","scm/${file.name}",entity,"multipart/form-data")
		}
	}
}