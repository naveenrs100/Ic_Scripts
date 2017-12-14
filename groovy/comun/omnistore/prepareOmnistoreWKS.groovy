package omnistore

import es.eci.utils.NexusHelper;
import es.eci.utils.ZipHelper;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import es.eci.utils.commandline.CommandLineHelper;
import es.eci.utils.TmpDir;


String pathNexus = build.getEnvironment(null).get("MAVEN_REPOSITORY") + "/"; println("pathNexus = ${pathNexus}");
String version = build.buildVariableResolver.resolve("version"); println("version = ${version}");
String groupId = build.buildVariableResolver.resolve("groupId"); println("groupId = ${groupId}");
String parentWorkspace = build.buildVariableResolver.resolve("parentWorkspace"); println("parentWorkspace = ${parentWorkspace}");
String artifactId = "OmniStore-Code";
String extension = "zip";

def wgetCommand(String urlOrigen, String pathDestino) {
	def returnCode;
	TmpDir.tmp { File dir ->
		def command = "wget ${urlOrigen} -O ${pathDestino}";
		println("Ejecutando comando: \"${command}\"")
		CommandLineHelper buildCommandLineHelper = new CommandLineHelper(command);
		returnCode = buildCommandLineHelper.execute(dir);
	}
	return returnCode;
}

println("Se baja el zip...");
def urlOrigen = pathNexus + groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + version + "/" + "${artifactId}-${version}.${extension}"
def pathDestino = "${parentWorkspace}/${artifactId}.${extension}";
wgetCommand(urlOrigen, pathDestino);

println("Se descomprime el zip...");
ZipHelper.unzipFile(new File("${parentWorkspace}/OmniStore-Code.zip"),
		new File("${parentWorkspace}/OmniStore"));

