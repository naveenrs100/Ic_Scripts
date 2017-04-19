/**
 * 
 * Este script se lanza como System Groovy Script.
 * 
 * Recopila el UUID real de AP y, en su caso, el id de grupo git,
 * usándolos para componer la projectKey del análisis lanzado.
 * 
 * Lo informa como variable productId en el job, para pasarlo a continuación
 * a la llamada a sonar.
 */

import es.eci.utils.ParamsHelper;
import git.GitUtils
import hudson.model.*;
import rtc.RTCUtils;

def build = Thread.currentThread().executable;
def resolver = build.buildVariableResolver;

def componentName = resolver.resolve("component");
def stream = resolver.resolve("stream");
def pwdRTC = resolver.resolve("pwdRTC");
def userRTC = build.getEnvironment(null).get("userRTC");
def urlRTC = build.getEnvironment(null).get("urlRTC");
def productId = resolver.resolve("productId");
def gitGroup = resolver.resolve("gitGroup");

if(stream != null && stream.trim().size() > 0) {	
   // Identificador de AP
   RTCUtils ru = new RTCUtils();
   def paUUID = ru.getProjectAreaUUID(
			"${stream}",
			"${userRTC}",
			"${pwdRTC}",
			"${urlRTC}"
			);	
   productId = paUUID;
   println "Resolviendo productId desde RTC"
}
else if (gitGroup != null && gitGroup.trim().size() > 0) {
   // Identificador de producto
   // Se le añade el grupo git para distinguirlo de los productos en ese
   //   área
   productId = productId + "_" + GitUtils.getCachedGroupId(gitGroup);
   println "Resolviendo productId desde Git"
}

println "Utilizando productId :: $productId"

if (productId != null) {
  
  def params = [:];

  componentName = componentName.replaceAll(" - ","_").
    replaceAll(" -","_").replaceAll("- ","_").replaceAll(" ","_");

  
  def projectKey = "${productId}.${componentName}";
  
  params.put("projectKey","${projectKey}");
  
  ParamsHelper.addParams(build,params);

}
else {
  println "No podemos determinar el id de producto, no se analiza"
  build.getState().setResult(NOT_BUILT);
}

