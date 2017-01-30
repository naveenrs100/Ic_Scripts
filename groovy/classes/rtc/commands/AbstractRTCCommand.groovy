package rtc.commands

import es.eci.utils.base.Loggable

/**
 * Clase base para los comandos RTC
 */
abstract class AbstractRTCCommand extends Loggable {

	//------------------------------------------------------------
	// Propiedades de la clase
	
	// Todos son obligatorios para todos los comandos RTC
	protected String scmToolsHome;
	protected String userRTC;
	protected String pwdRTC;
	protected String urlRTC;
	protected File parentWorkspace;
	protected Boolean light = true;
	
	//------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Asigna el directorio que contiene las herramientas de RTC.
	 * @param scmToolsHome Directorio raíz de las herramientas RTC 
	 */
	public void setScmToolsHome(String scmToolsHome) {
		this.scmToolsHome = scmToolsHome;
	}

	/**
	 * Asigna el usuario de las credenciales.
	 * @param userRTC Usuario RTC
	 */
	public void setUserRTC(String userRTC) {
		this.userRTC = userRTC;
	}

	/**
	 * Asigna el password de las credenciales.
	 * @param pwdRTC Password RTC
	 */
	public void setPwdRTC(String pwdRTC) {
		this.pwdRTC = pwdRTC;
	}

	/**
	 * Asigna la URL del repositorio RTC.
	 * @param urlRTC URL de repositorio RTC
	 */
	public void setUrlRTC(String urlRTC) {
		this.urlRTC = urlRTC;
	}
	
	/**
	 * Asigna un directorio base de ejecución.
	 * @param parentWorkspace Directorio de ejecución del script
	 */
	public void setParentWorkspace(File parentWorkspace) {
		this.parentWorkspace = parentWorkspace;
	}

	/**
	 * Indica si deseamos utilizar la versión ligera de RTC.
	 * @param light Si es cierto, activa el modo ligero del comando SCM
	 */
	public void setLight(Boolean light) {
		this.light = light;
	}
	
	@Deprecated
	public void setDaemonsConfigDir(String dir) {
		// obsoleto; se mantiene por compatibilidad con la base de scripting
	}
	
	/** Ejecuta el comando. */
	public abstract void execute();
	
}
