import java.util.ArrayList;
import java.util.Map;


// Obtener la instancia de job en jenkins para cada ejecución de componente
// Pares componentes/versión
def componentsVersions = []

def componentUrbanCode = "PruebaRelease - App 2";
def componentDocumentation = "true";
if (componentUrbanCode != null && componentUrbanCode.trim().length() > 0) {
	println "Recuperando la información de $componentUrbanCode"
	// Obtener el parámetro de versión guardado en la ejecución del job
	def builtVersion = "1.0.55.0-SNAPSHOT";
	println "---> Recuperada versión: $componentUrbanCode <-- $builtVersion";
	def tmp = [:];
	tmp.put(componentUrbanCode, builtVersion);
	if(componentDocumentation != null && componentDocumentation.trim().equals("true")) {
		def tmpDoc = [:];
		tmpDoc.put(componentUrbanCode + ".doc", builtVersion);
		componentsVersions.add(tmpDoc);
	}
	componentsVersions.add(tmp);
}
else {
	println "Descartando al no tener despliegue en Urban"
}

deploySnapshotVersions(
		componentsVersions,
		"urbanCodeApp",
		"urbanCodeEnv");

	
/**
 * Copiado del UrbanCodeSnapshotDeployer	
 * @param componentsVersions
 * @param urbanCodeApp -> no vale en esta prueba
 * @param urbanCodeEnv -> no vale en esta prueba
 */
public void deploySnapshotVersions(
		componentsVersions,
		String urbanCodeApp,
		String urbanCodeEnv,
		String nightlyName = "nightly") {
	println "Versiones a desplegar -> ${componentsVersions}"
	// Esta variable indica si se ha encontrado alguna versión terminada
	//	en -SNAPSHOT
	boolean isThereOpenVersion = false;
	// Resolver las versiones -SNAPSHOT si fuera necesario
	componentsVersions.each { Map<String,String> compoMap ->
		compoMap.keySet().each { String componentUrbanCode ->
			println("compVersion = ${compoMap}. Obteniendo el valor para ${componentUrbanCode}...");
			String builtVersion = compoMap[componentUrbanCode];
			println("builtVersion para ${componentUrbanCode} -> ${builtVersion}");
			String thisComponentUrbanCode = componentUrbanCode.split("\\.doc")[0];
			if (builtVersion.endsWith("-SNAPSHOT")) {
				println("Es snapshot!")
			}

		}
	}
}