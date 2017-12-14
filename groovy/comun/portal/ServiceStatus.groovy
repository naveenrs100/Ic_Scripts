package portal

//$JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/portal/ServiceStatus.groovy
@GrabResolver(name='nexuseci', root='http://nexus.elcorteingles.int/content/groups/public/')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')
@Grab(group='org.apache.httpcomponents', module='httpmime', version='4.4')

//imports
import javax.net.ssl.*;
import javax.xml.bind.DatatypeConverter

import java.security.cert.X509Certificate;

import groovy.io.FileType
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.*
import static groovyx.net.http.ContentType.*
import groovy.json.*
import java.util.UUID

import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.entity.StringEntity

// Parametros de entrada

servicesName = args[0]
quveurl = args[1]
jenkinsHome = args[2]
workspace = args[3]

// Configuration ---------------

TIMEOUT = 25000;
PROXY_HOST = "proxycorp.geci";
PROXY_PORT = 8080;
PROXY_USERNAME = "T0000104";
PROXY_PASSWORD = "12345678";

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

def getServicesJson(){
  def servicesJson = new File("${workspace}/services");
  def back = new File("${workspace}/services_back.json");
  if (servicesJson.exists() && servicesJson.length()>0){
	if (back.exists())
		back.delete();
	boolean renamed = servicesJson.renameTo(back);
	servicesJson = back;
	println "renamed: ${renamed}"
  }else if (back.exists()){
	servicesJson = back;
  }else{
	println "servicesJson doesn't exists!!"
	servicesJson = null;
  }
  return servicesJson;
}

def getHttpsConnection(uri, proxy){
  
  TrustManager[] trustAllCerts = [new X509TrustManager() {
	public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	  return null;
	}
	public void checkClientTrusted(X509Certificate[] certs, String authType) {
	}
	public void checkServerTrusted(X509Certificate[] certs, String authType) {
	}
  }]
  // Install the all-trusting trust manager
  final SSLContext sc = SSLContext.getInstance("SSL");
  sc.init(null, trustAllCerts, new java.security.SecureRandom());
  HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
  // Create all-trusting host name verifier
  HostnameVerifier allHostsValid = new HostnameVerifier() {
	public boolean verify(String hostname, SSLSession session) {
	  return true;
	}
  };
  // Install the all-trusting host verifier
  HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
  HttpsURLConnection con = new URL(uri).openConnection(proxy);
  return con;
}

def getHttpConnection(uri){
  Authenticator proxyauth = new Authenticator() {
	public PasswordAuthentication getPasswordAuthentication() {
	  return (new PasswordAuthentication(PROXY_USERNAME,PROXY_PASSWORD.toCharArray()));
	}
  }
  Authenticator.setDefault(proxyauth);
  
  Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_HOST, PROXY_PORT));

  HttpURLConnection con = null;
  
  if (uri.toLowerCase().indexOf("https")!=-1){
	con = getHttpsConnection(uri,proxy);
  }else{
	con = new URL(uri).openConnection(proxy);
  }
  return con;
}

def testGet(environment){
  def uri = environment.uri
  def latency = 0;
  
  def result = [:];
  result.put("uri",uri);
  int responseCode = 0;
  try{

	long startTime = System.nanoTime();
	HttpURLConnection con = getHttpConnection(uri);
	con.setConnectTimeout(TIMEOUT);
	
	if (environment.username!=null){
	  String userPassword = environment.username + ":" + environment.password;
	  String encoding = DatatypeConverter.printBase64Binary(userPassword.getBytes());
	  con.setRequestProperty("Authorization", "Basic " + encoding);
	  }

	con.setRequestMethod("GET");
	con.setRequestProperty("User-Agent", "Mozilla/5.0");
 
	responseCode = con.getResponseCode();
	long endTime = System.nanoTime();
	latency = Math.round((endTime - startTime) / 1000000);
	
	BufferedReader inn = new BufferedReader(new InputStreamReader(con.getInputStream()));
	String inputLine;
	StringBuffer response = new StringBuffer();
 
	while ((inputLine = inn.readLine()) != null) {
		response.append(inputLine);
	}
	inn.close();
	result.put("description","");

  }catch(Exception e){
	result.put("description",e.toString());
	if (responseCode==0)
		responseCode = 500;
  }
  if (environment.httpOkCodes!=null && environment.httpOkCodes.find { it == responseCode }!=null)
	  result.put("result","OK");
  result.put("httpCode",responseCode);
  result.put("latency",latency);
  return result;
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

def getHttpResults(environments){
  def results = [:];
  environments.each() { environment ->
	println "${environment.name} : ${environment.uri}";
	results.put(environment.name, testGet(environment));
  }
  return results;
}

//-------------------------

def servicesJson = getServicesJson();
if (servicesJson!=null && servicesJson.exists()){
  def services = new JsonSlurper().parseText(servicesJson.getText("UTF-8"))
  def date = new Date();
  def servicesResult = [];
  
  services.each(){ service ->
	def serviceResult = [:];

	serviceResult.put("id",UUID.randomUUID());
	serviceResult.put("name",service.name);
	serviceResult.put("description",service.description);
	serviceResult.put("date", date);
	//serviceResult.put("tests", []);
	serviceResult.put("results", getHttpResults(service.environments));
	servicesResult.add(serviceResult);

  }
  def json = new JsonOutput().toJson([services:servicesResult]);
  println json
  def entity = new StringEntity(json, ContentType.APPLICATION_JSON)
  sendQuve("${quveurl}/","services",entity,"multipart/form-data")
}else{
  throw new Exception("fichero '${servicesJson}' no entrontrado");
}