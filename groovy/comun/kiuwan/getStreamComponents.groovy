package kiuwan;

import es.eci.utils.ParamsHelper
import es.eci.utils.StringUtil
import rtc.RTCHelper
import rtc.commands.RTCgetComponentsCommand

/**
 * Este script se ejecuta como System Groovy Script
 * 
 * Salida:
 * Recoge los componentes de una corriente y los informa en una variable 
 * 
 * kiuwanComponents
 * 
 * Esta variable recoge los componentes separados por líneas
 */

String stream = build.getEnvironment(null).get("stream")
String userRTC = build.getEnvironment(null).get("userRTC");
String pwdRTC = build.buildVariableResolver.resolve("pwdRTC");
String urlRTC = build.getEnvironment(null).get("urlRTC");
String workspaceParam = build.getEnvironment(null).get("parentWorkspace");
File parentWorkspace = new File(build.workspace.toString())

RTCHelper helper = new RTCHelper(userRTC, pwdRTC, urlRTC)
helper.initLogger { println it }

List<String> components = helper.listComponents(stream, parentWorkspace);

StringBuilder sb = new StringBuilder();
components.each { String component -> 
	sb.append(component); 
	sb.append(System.getProperty("line.separator")) 
};

ParamsHelper.addParams(build, ["kiuwanComponents":sb.toString()]);
