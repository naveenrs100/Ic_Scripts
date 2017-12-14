import es.eci.utils.ParamsHelper
import es.eci.utils.StringUtil
import es.eci.utils.Utiles
import groovy.json.JsonSlurper
import ppm.PPMProduct
import ppm.PPMProductParser

/**
 * Este script se ejecuta como System Groovy Script
 * 
 * Si KIUWAN_CONNECTION == false, este script hace regresar al job con SUCCESS.
 * 
 * Su función es informar las variables necesarias para el lanzamiento del
 * análisis Kiuwan, partiendo de las variables propias del componente
 * que se esté construyendo.
 * 
 * parentWorkspace
 * component
 * builtVersion
 * product
 * subsystem
 * action
 * provider
 * changeRequest
 * SCM -> Se utilizará más adelante para invocar al wf apropiado: RTC / Git
 * 
 *
 * Así mismo, a este script le corresponde validar que el producto está incluido entre
 * los dados de alta en PPM.  Para ello debe validarlo contra el fichero dejado por Clarity.
 * 
 */

Boolean isKiuwanEnabled = Utiles.toBoolean(build.getEnvironment(null).get("KIUWAN_CONNECTION"))
if (isKiuwanEnabled) {
	String parentWorkspace = build.getEnvironment(null).get("parentWorkspace");
	println "parentWorkspace <-- $parentWorkspace"
	String builtVersion = build.getEnvironment(null).get("builtVersion");
	println "builtVersion <-- $builtVersion"
	// Parámetros para el lanzamiento de kiuwan
	String stream = build.getEnvironment(null).get("stream");
	String gitGroup = build.getEnvironment(null).get("gitGroup");
	// Subsistema: stream o gitGroup
	String subsystem = build.getEnvironment(null).get("subsystem");
	if (StringUtil.isNull(subsystem)) {
		if (StringUtil.notNull(stream)) {
			subsystem = StringUtil.trimStreamName(stream);
		}
		else if (StringUtil.notNull(gitGroup)) {
			subsystem = StringUtil.trimStreamName(gitGroup);
		}
		ParamsHelper.addParams(build, ["subsystem":subsystem]);
	}
	println "subsystem <-- $subsystem"
	// Producto: a partir de stream o gitgroup, casarlo con el producto a partir de la 
	//	información recogida de QUVE
	String product = build.getEnvironment(null).get("product");
	if (StringUtil.isNull(product) && StringUtil.notNull(subsystem)) {
		// La caché de productos se encuentra en 
		//	/jenkins/jobs/CacheKiuwanProjectAreas/workspace/groups
		// Por conveniencia se utiliza la variable KIUWAN_GROUPS_CACHE
		String cacheFile = build.getEnvironment(null).get("KIUWAN_GROUPS_CACHE")
		File cache = new File(cacheFile)
		def jsonCache = new JsonSlurper().parseText(cache.text)
		product = jsonCache[subsystem]
		// Eliminar acentos
		if (StringUtil.notNull(product)) {
			ParamsHelper.addParams(build, ["product": StringUtil.removeAccents(product)]);
		}
	}
	
	// Change request: se utilizará el nombre de producto, con vistas a
	//	consultar por el mismo y ver el estado de aceptación bajo la pantalla
	//	de ciclo de vida
	String changeRequest = build.getEnvironment(null).get("changeRequest");
	if (StringUtil.isNull(changeRequest) && StringUtil.notNull(product)) {
		// Se informa el producto
		ParamsHelper.addParams(build, ["changeRequest":product]);
		
	} 
	println "changeRequest <-- $changeRequest"
	String scm = null;
	if (StringUtil.notNull(stream)) {
		scm = "RTC"
	}
	else if (StringUtil.notNull(gitGroup)) {
		scm = "Git"
	}
	if (StringUtil.notNull(scm)) {
		ParamsHelper.addParams(build, ["SCM":scm]);
	}
	println "scm <-- $scm"
}
else {
	println "KIUWAN_CONNECTION <-- false"
	println "No se realiza análisis en Kiuwan"
}