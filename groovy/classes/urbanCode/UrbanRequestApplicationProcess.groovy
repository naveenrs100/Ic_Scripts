package urbanCode;

import es.eci.utils.base.Loggable;
import urbanCode.UrbanCodeApplicationProcess;
import urbanCode.UrbanCodeExecutor;

/**
 * Esta clase ejecuta una petición de proceso de Urban Code mediante
 * AUTOMATIC_DEPLOY.  Esta es una aplicación implementada en Urban para
 * atender procesos tales como los de moonshine, que requieren de un
 * preproceso para construir la instantánea y atacar con ella la aplicación
 * real (que aquí se informa en el parámetro applicationProcess).
 * <br/>
 * Notar que es <b>imprescindible</b> contar con una instalación local del cliente.  Se puede provisionar
 * desde:<br/>
 * <a href="http://nexus.elcorteingles.int/service/local/repositories/GC/content/ibm/urbanCode/udclient/6.1.0/udclient-6.1.0.zip">Cliente udclient en Nexus</a>
 * <br/> 
 * @see <a href="https://www-01.ibm.com/support/knowledgecenter/SS4GSP_6.1.2/com.ibm.udeploy.reference.doc/topics/cli_commands.html">Documentación del cliente udclient en IBM</a>
 */
public class UrbanRequestApplicationProcess extends Loggable {

	String udClientCommand;
	String urlUdeploy;
	String user;
	String password;
	String applicationProcess;
	String instantanea;

	public void execute() {

		Map<String,String> props = ["ETIQUETA":""];
		props.put("FICHA", this.instantanea);
		
//		{
//			"application": "AUTOMATIC_DEPLOY",
//			"applicationProcess": "XXXXXXXXXX",
//			"environment": "Automatic_Deploy",
//			"onlyChanged": "false",
//			"snapshot": "Automatic_Deploy",
//			"properties": {
//							"FICHA": "YYYYYYYYYY"}
//		   }
		
		UrbanCodeApplicationProcess urbanAppProcess =
				new UrbanCodeApplicationProcess(
				"AUTOMATIC_DEPLOY",
				this.applicationProcess,
				"Automatic_Deploy",
				false,
				"Automatic_Deploy",
				props);

		UrbanCodeExecutor exe =
				new UrbanCodeExecutor(
				this.udClientCommand,
				this.urlUdeploy,
				this.user,
				this.password);

		def urbanDeployRequest = exe.requestApplicationProcess(urbanAppProcess);
		
		log("Resultado -> ${urbanDeployRequest}");
	}

	/**
	 * @return Ruta del cliente udclient
	 */
	public String getUdClientCommand() {
		return udClientCommand;
	}

	/**
	 * @param udClientCommand Ruta del cliente udclient
	 */
	public void setUdClientCommand(String udClientCommand) {
		this.udClientCommand = udClientCommand;
	}

	/**
	 * @return URL de Urban Code
	 */
	public String getUrlUdeploy() {
		return urlUdeploy;
	}

	/**
	 * @param urlUdeploy URL de Urban Code
	 */
	public void setUrlUdeploy(String urlUdeploy) {
		this.urlUdeploy = urlUdeploy;
	}

	/**
	 * @return Usuario de Urban Code
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @param user Usuario de Urban Code
	 */
	public void setUser(String user) {
		this.user = user;
	}
	
	/** 
	 * @return Contraseña de Urban Code
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password Contraseña de Urban Code
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return Proceso a lanzar (en este caso, será la aplicación real)
	 */
	public String getApplicationProcess() {
		return applicationProcess;
	}

	/**
	 * @param applicationProcess Proceso a lanzar (en este caso, será la aplicación real)
	 */
	public void setApplicationProcess(String applicationProcess) {
		this.applicationProcess = applicationProcess;
	}

	/**
	 * @return Nombre de la instantánea a lanzar
	 */
	public String getInstantanea() {
		return instantanea;
	}

	/**
	 * @param instantanea Nombre de la instantánea a lanzar
	 */
	public void setInstantanea(String instantanea) {
		this.instantanea = instantanea;
	}
	
}
