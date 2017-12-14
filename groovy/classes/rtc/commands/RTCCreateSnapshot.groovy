package rtc.commands

import java.io.File;
import es.eci.utils.TmpDir;
import es.eci.utils.ParameterValidator
import es.eci.utils.ScmCommand
import rtc.RTCUtils;

class RTCCreateSnapshot extends AbstractRTCCommand {

	private String stream;
	private String snapshotName;
	private String description;


	@Override
	public void execute() {
		TmpDir.tmp { File daemonsConfigDir ->
			ScmCommand command = null;
			// Validación de obligatorios
			ParameterValidator.builder()
					.add("scmToolsHome", scmToolsHome)
					.add("userRTC", userRTC)
					.add("pwdRTC", pwdRTC)
					.add("urlRTC", urlRTC)
					.add("stream", stream)
					.add("snapshotName", snapshotName)
					.add("description", description)
					.add("light", light)
					.build().validate();

			TmpDir.tmp { File dir ->
				try {
					command = new ScmCommand(light, scmToolsHome, daemonsConfigDir.getCanonicalPath());
					command.initLogger(this);
					command.ejecutarComando(
							"create snapshot \"$stream\" --name \"$snapshotName\" --description \"$description\"",
							userRTC, pwdRTC, urlRTC, dir);
					RTCUtils.exitOnError(command.getLastResult(), "Creando snapshot en corriente \"${stream}\".");
				}
				catch (Exception e) {
					e.printStackTrace();
					throw e;
				} finally {
					if (light) { command.detenerDemonio(dir); }
				}
			}
		}
	}


	public String getStream() {
		return stream;
	}


	public void setStream(String stream) {
		this.stream = stream;
	}


	public String getSnapshotName() {
		return snapshotName;
	}


	public void setSnapshotName(String snapshotName) {
		this.snapshotName = snapshotName;
	}


	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}
	
	
}
