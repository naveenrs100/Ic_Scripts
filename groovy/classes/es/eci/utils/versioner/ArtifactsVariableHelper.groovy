package es.eci.utils.versioner

import es.eci.utils.base.Loggable

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
	public void updateArtifacts(def jsonObject, String versionerStep, String increaseIndex = null) {
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
			def index = increaseIndex.toInteger(); // En este punto increaseIndex debe venir.
			jsonObject.each {
				def isSnapshot = it.version.contains("-SNAPSHOT");
				String version = it.version.split("-SNAPSHOT")[0];
				log "stepUpdateArtifactsJson -> version: ${it.groupId}:${it.artifactId}:${it.version}"
				if (version != null && version.trim().length() > 0) {
					ArrayList versionDigits = version.split("\\.");
					log "stepUpdateArtifactsJson -> versionDigits: $versionDigits"
					boolean isHotfix = (index == 5);
		
					def newVersion;
					if(isSnapshot && !isHotfix) {
						def increasedDigit = versionDigits[index - 1].toInteger() + 1;
						versionDigits.set(index - 1, increasedDigit);
						if(index < versionDigits.size()) {
							for(int i = index; i < versionDigits.size(); i++) {
								versionDigits.set(i, 0);
							}
						}
						newVersion = versionDigits[0] + "." + versionDigits[1] + "." + versionDigits[2] + "." + versionDigits[3] + "-SNAPSHOT";						
					}
					else if(!isSnapshot && !isHotfix) {
						def increasedDigit = versionDigits[index - 1].toInteger() + 1;
						versionDigits.set(index - 1, increasedDigit);
						if(index < versionDigits.size()) {
							for(int i = index; i < versionDigits.size(); i++) {
								versionDigits.set(i, 0);
							}
						}
						newVersion = versionDigits[0] + "." + versionDigits[1] + "." + versionDigits[2] + "." + versionDigits[3];
					}
					else if(!isSnapshot && isHotfix) {
						log("Caso hotfix con versionDigits = ${versionDigits}");
						if(versionDigits[3].split("-").size() == 1) {
							versionDigits.set(3, versionDigits[3] + "-1");
							newVersion = versionDigits[0] + "." + versionDigits[1] + "." + versionDigits[2] + "." + versionDigits[3];
							log("newVersion = ${newVersion}")
						}
						else if(versionDigits[3].split("-").size() == 2) {
							def hotFixDigit = versionDigits[3].split("-")[1].toInteger() + 1;
							versionDigits.set(3, versionDigits[3].split("-")[0] + "-" + hotFixDigit);
							newVersion = versionDigits[0] + "." + versionDigits[1] + "." + versionDigits[2] + "." + versionDigits[3];
							log("newVersion = ${newVersion}")
						}
					}
		
					it.version = newVersion;
				}
				else {
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
