package urbanCode;

import es.eci.utils.base.Loggable;
import urbanCode.UrbanCodeApplicationProcess;
import urbanCode.UrbanCodeExecutor;


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
		
		println("Resultado -> ${urbanDeployRequest}");
	}

	public String getUdClientCommand() {
		return udClientCommand;
	}

	public void setUdClientCommand(String udClientCommand) {
		this.udClientCommand = udClientCommand;
	}

	public String getUrlUdeploy() {
		return urlUdeploy;
	}

	public void setUrlUdeploy(String urlUdeploy) {
		this.urlUdeploy = urlUdeploy;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getApplicationProcess() {
		return applicationProcess;
	}

	public void setApplicationProcess(String applicationProcess) {
		this.applicationProcess = applicationProcess;
	}

	public String getInstantanea() {
		return instantanea;
	}

	public void setInstantanea(String instantanea) {
		this.instantanea = instantanea;
	}


	
}
