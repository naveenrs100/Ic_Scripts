package es.eci.utils.versioner

import es.eci.utils.base.Loggable
import es.eci.utils.versioner.XmlUtils;

/**
 * Esta clase actualiza el contenido de la variable artifacts según sea necesario
 */
class ArtifactsVariableHelper extends Loggable {

	/**
	 * Este método actualiza el objeto jsonObject con el resultado de aplicar
	 * el paso de versionado correspondiente
	 * @param jsonObject Objeto resultado de parsear el contenido de la variable
	 * 	con el json de artefactos.  Variable de entrada/salida: el método lo actualiza.
	 * @param versionerStep Paso de versionado: relleno, quitar snapshot, poner 
	 * 	snapshot, incrementar versión
	 * @return
	 */
	public void updateArtifacts(def jsonObject, String versionerStep, String action, String releaseMantenimiento) {
		if(versionerStep == "fillVersion") {
			log "stepUpdateArtifactsJson -> fillVersion"
			jsonObject.each {
				def versionText =it.version;
				def isSnapshot = versionText.contains("-SNAPSHOT");
				def numericPart = versionText.split("-SNAPSHOT")[0];
				ArrayList numericPartArray = numericPart.split("\\.");
				if(numericPartArray.size() < 4) {
					log("La version de ${it.artifactId} tiene menos de 4 dígitos.");
					def digitsToFill = 4 - numericPartArray.size();
					for(int i=0; i<digitsToFill; i++) {
						numericPartArray.add("0");
					}
					numericPart = numericPartArray[0] + "." + numericPartArray[1] + "." + numericPartArray[2] + "." +numericPartArray[3];
					if(isSnapshot) {
						def finalVersion = numericPart + "-SNAPSHOT";
						it.version = finalVersion;
					} else {
						def finalVersion = numericPart;
						it.version = finalVersion;
					}
				}
			}
		}
		else if(versionerStep == "removeSnapshot") {
			log "stepUpdateArtifactsJson -> removeSnapshot"
			jsonObject.each {
				String version = it.version;
				log "stepUpdateArtifactsJson -> version: ${it.groupId}:${it.artifactId}:${it.version}"
				if (version != null && version.trim().length() > 0) {
					it.version = it.version.split("-SNAPSHOT")[0]
				}
				else {
					log ("[WARNING] El contenido de version era '$version', por lo tanto no se trata")
				}
			}
		}
		else if(versionerStep == "increaseVersion") {
			log "stepUpdateArtifactsJson -> increaseVersion"			
			jsonObject.each {
				XmlUtils utils = new XmlUtils();
				if(it.version != null && it.version.trim().length() > 0) {
					def newVersion = utils.increaseVersionDigit(it.version, action, releaseMantenimiento);
					it.version = newVersion;
				} else {
					log ("[WARNING] El contenido de version era '$version', por lo tanto no se trata")
				}
			}
		}
		else if(versionerStep == "addSnapshot") {
			log "stepUpdateArtifactsJson -> addSnapshot"
			jsonObject.each {
				String version = it.version;
				log "stepUpdateArtifactsJson -> version: ${it.groupId}:${it.artifactId}:${it.version}"
				if (version != null && version.trim().length() > 0) {
					if(!it.version.endsWith("-SNAPSHOT")) {
						def newVersion = it.version + "-SNAPSHOT";
						it.version = newVersion;
					}
				}
				else {
					log ("[WARNING] El contenido de version era '$version', por lo tanto no se trata")
				}
			}
		}
		else {
			throw new Exception("Se necesita un versionerStep valido");
		}
	}
}
