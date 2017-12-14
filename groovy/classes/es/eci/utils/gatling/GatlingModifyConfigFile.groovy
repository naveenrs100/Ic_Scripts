package es.eci.utils.gatling;

/**
 * Esta clase modifica el fichero de configuración scenarioGlobal en base a los parámetros
 * que vienen del job de lanzamiento. El fichero no se guarda por lo que se modifica en cada
 * lanzamiento (como debe ser).
 */

import java.io.File;

import es.eci.utils.ParameterValidator;
import es.eci.utils.base.Loggable;

/**
 * Modifica el fichero de configuración de gatling con los valores necesarios
 */
class GatlingModifyConfigFile extends Loggable {
	
	private String environment;			// Entorno de lanzamiento
	private String duration;			// Duración de las pruebas
	private String qps;					// Querys por segundo
	
	public void execute() {
		
		// Validación de obligatorios
		ParameterValidator.builder()
			.add("environment", environment)
			.add("duration", duration)
			.add("qps", qps).build().validate();
		
		File fichero = new File("src/test/resources/config/scenarioGlobal.conf")
		String configFile = fichero.getText()
		String newConfigFile = ""
		
		if (environment.equals("nft")) {
			log "--- INFO: Aplicando ${duration} minutos y ${qps} QPS para NFT..."
			newConfigFile = configFile.replaceAll("NFT_DURATION", duration)
			fichero.setText(newConfigFile.replaceAll("NFT_QPS", qps))
		} else if (environment.equals("uat")) {
			log "--- INFO: Aplicando ${duration} minutos y ${qps} QPS para UAT..."
			newConfigFile = configFile.replaceAll("UAT_DURATION", duration)
			fichero.setText(newConfigFile.replaceAll("UAT_QPS", qps))
		} else if (environment.equals("pro")) {
			log "--- INFO: Aplicando ${duration} minutos y ${qps} QPS para PRO..."
			newConfigFile = configFile.replaceAll("PRO_DURATION", duration)
			fichero.setText(newConfigFile.replaceAll("PRO_QPS", qps))
		} else {
			log "### ERROR: Ningún entorno compatible encontrado en el fichero de configuración"
			throw new Exception("Error en configuración")
		}		
	
	}

	/**
	 * @return the environment
	 */
	public String getEnvironment() {
		return environment;
	}

	/**
	 * @param environment the environment to set
	 */
	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	/**
	 * @return the duration
	 */
	public String getDuration() {
		return duration;
	}

	/**
	 * @param duration the duration to set
	 */
	public void setDuration(String duration) {
		this.duration = duration;
	}

	/**
	 * @return the qps
	 */
	public String getQps() {
		return qps;
	}

	/**
	 * @param qps the qps to set
	 */
	public void setQps(String qps) {
		this.qps = qps;
	}
	
}