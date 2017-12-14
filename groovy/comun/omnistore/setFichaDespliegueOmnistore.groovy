import es.eci.utils.NexusHelper;
import es.eci.utils.ParamsHelper;
import es.eci.utils.StringUtil;
import es.eci.utils.TmpDir;
import es.eci.utils.ZipHelper;
import es.eci.utils.base.Loggable;
import es.eci.utils.pom.MavenCoordinates;
import groovy.json.JsonOutput;
import buildtree.BuildBean;
import buildtree.BuildTreeHelper;
import urbanCode.UrbanCodeExecutor;
import urbanCode.UrbanCodeFichaDespliegue;

import org.codehaus.groovy.runtime.StackTraceUtils;


try {
	String groupId = "es.eci.omnistore";

	BuildTreeHelper helper = new BuildTreeHelper(200);
	List<BuildBean> beanList = helper.executionTree(build);
	String builtVersion;
	beanList.each { BuildBean bean ->
		if(bean.getName().contains("-COMP-")) {
			builtVersion = bean.getBuiltVersion();
		}
	}

	List<MavenCoordinates> componentesOmnistore = [
		new MavenCoordinates(groupId, "ECIFulfilment", 	 builtVersion, "ear"),
		new MavenCoordinates(groupId, "ECILockManager",  builtVersion, "ear"),
		new MavenCoordinates(groupId, "ECIProduction", 	 builtVersion, "ear"),
		new MavenCoordinates(groupId, "ECIPublishing", 	 builtVersion, "ear"),
		new MavenCoordinates(groupId, "ECIServiceAdmin", builtVersion, "ear"),
		new MavenCoordinates(groupId, "ECIServices", 	 builtVersion, "ear"),
		new MavenCoordinates(groupId, "OmniStore-Code",  builtVersion, "zip")
	]

	String action = build.buildVariableResolver.resolve("action");
	String entornoUrban = build.buildVariableResolver.resolve("entornoUrbanCode");
	String instantanea = build.buildVariableResolver.resolve("instantanea");
	String stream = build.buildVariableResolver.resolve("stream");
	String managersMail = "josemanuel_fernandez@gexterno.es,GCSoporteplataformaIC@elcorteingles.es";
	
	if(action.trim().toLowerCase().equals("deploy")) {
		instantanea = "nightly_ATG_Omnistore"
	}
	if(instantanea == null || instantanea.trim().equals("") || instantanea.contains("\${")) {		
		instantanea = builtVersion;
		ParamsHelper pHelper = new ParamsHelper();
		String [] paramsToDelete = ["instantanea"];
		pHelper.deleteParams(build, paramsToDelete);
		Map theParams = ["instantanea":"${instantanea}"]
		pHelper.addParams(build, theParams);
	}
	String aplicacionUrbanCode = build.buildVariableResolver.resolve("aplicacionUrbanCode");
	String nexusUrl = build.getEnvironment(null).get("ROOT_NEXUS_URL");  println nexusUrl;
	String urbanGroupId = build.getEnvironment(null).get("URBAN_GROUP_ID");	println urbanGroupId;
	String nexusUser = build.buildVariableResolver.resolve("DEPLOYMENT_USER"); println nexusUser;
	String nexusPass = build.buildVariableResolver.resolve("DEPLOYMENT_PWD"); println nexusPass;
	String udClientCommand = build.getEnvironment(null).get("UDCLIENT_COMMAND");	println udClientCommand;
	String urlUrbanCode = build.getEnvironment(null).get("UDCLIENT_URL");	println urlUrbanCode;
	String urbanUser = build.getEnvironment(null).get("UDCLIENT_USER");	println urbanUser;
	String urbanPassword = build.buildVariableResolver.resolve("UDCLIENT_PASS"); println urbanPassword;
	
	
	List<Map<String,String>> versions = [];
	componentesOmnistore.each { MavenCoordinates component ->
		Map<String,String> tmp = [:];
		NexusHelper nh = new NexusHelper(nexusUrl);		
		tmp.put(component.getArtifactId(), nh.resolveSnapshot(component));
		versions.add(tmp);
	}

	Map<String,Object> fichaDespliegueMap = [:];
	fichaDespliegueMap.put("name", instantanea);
	fichaDespliegueMap.put("application", aplicacionUrbanCode);
	fichaDespliegueMap.put("description", action);
	fichaDespliegueMap.put("versions",versions);

	String json = JsonOutput.prettyPrint(JsonOutput.toJson(fichaDespliegueMap));
	TmpDir.tmp { dir ->

		File jsonFile = new File(dir,"descriptor.json")
		jsonFile.text = json;
		
		File rtcJsonFile = new File(dir,"rtc.json");
		def rtcJsonMap = [:];
		def versionsMap = [:];
		versionsMap.put("aQT0 - OmniStore - Desarrollo", builtVersion);
		rtcJsonMap.put("source", stream);
		rtcJsonMap.put("managersMail", managersMail);
		rtcJsonMap.put("versions", versionsMap);
		def rtcJson = JsonOutput.prettyPrint(JsonOutput.toJson(rtcJsonMap));
		rtcJsonFile.text = rtcJson;

		File zipFile = ZipHelper.addDirToArchive(dir);

		MavenCoordinates coord = new MavenCoordinates(urbanGroupId, StringUtil.normalize(aplicacionUrbanCode), instantanea);
		coord.setPackaging("zip");
		coord.setRepository("fichas_despliegue");

		NexusHelper nexusHelper = new NexusHelper(nexusUrl)
		nexusHelper.initLogger { println it }
		nexusHelper.setNexus_user(nexusUser)
		nexusHelper.setNexus_pass(nexusPass)

		nexusHelper.upload(coord, zipFile)
	}
	
	/**
	 * Se borra la nightly de urbancode si ya existia.
	 */
	if(action.trim().equals("deploy")) {
		UrbanCodeExecutor exec = new UrbanCodeExecutor(udClientCommand, urlUrbanCode, urbanUser, urbanPassword);
		exec.initLogger { println it }
		exec.deleteSnapshot(aplicacionUrbanCode, instantanea);
	}
	
	/**
	 * Conexión con UrbanCode para crear las versiones, dar de alta la snapshot y lanzar el despliegue si fuese necesario.
	 */
	UrbanCodeFichaDespliegue urbanFichaDesp = new UrbanCodeFichaDespliegue();
	urbanFichaDesp.initLogger { println it }
	urbanFichaDesp.setUrlNexus(nexusUrl)
	urbanFichaDesp.setUdClientCommand(udClientCommand)
	urbanFichaDesp.setUrlUrbanCode(urlUrbanCode)
	urbanFichaDesp.setUrbanUser(urbanUser)
	urbanFichaDesp.setUrbanPassword(urbanPassword)
	urbanFichaDesp.setDescriptor(json)
	urbanFichaDesp.setNombreAplicacionUrban(aplicacionUrbanCode)
	urbanFichaDesp.setInstantaneaUrban(instantanea)
	urbanFichaDesp.setEntornoUrban(entornoUrban)
	urbanFichaDesp.setServiceStop(false)

	/* Crear versión de cada componente en UrbanCode */
	urbanFichaDesp.checkAndCreateComponentVersions(json);

	urbanFichaDesp.initLogger { println it }
	println("Ejecutando la snapshot sobre UrbanCode de la instantanea: " + json);
	urbanFichaDesp.executeUrbanSnapshot(json);


	/* Creación de la snapshot en Urban */
	// Lanzamiento del deploy en Urban
	// ¿Hay entorno? - Una # indica que no se desplegará
	if ( isNull(entornoUrban) || entornoUrban.contains("#") ) {
		println("--- INFO: No está activado el despliegue automático en entorno desde QUVE");
	} else {
		println("--- INFO: Se procede a lanzar el despliegue en Urban");
		urbanFichaDesp.executeUrbanDeploy(json, false);
	}

} catch (Exception e) {
	println(" -- [WARNING] Ha habido un error creando o dando de alta el descriptor UrbanCode.");
	Loggable logger = new Loggable();
	logger.initLogger { println it };
	logger.logException(e);
	StackTraceUtils.sanitize(e).printStackTrace()
	println(e.getMessage());
	e.printStackTrace();
}



/**
 * Devuelve si un string es considerado null
 * @param s
 * @return
 */
private boolean isNull(String s) {
	return s == null || s.trim().length() == 0;
}