package omnistore

import es.eci.utils.NexusHelper;
import es.eci.utils.ZipHelper;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import es.eci.utils.commandline.CommandLineHelper;
import es.eci.utils.pom.MavenCoordinates
import es.eci.utils.TmpDir;


String pathNexus = build.getEnvironment(null).get("MAVEN_REPOSITORY") + "/"; println("pathNexus = ${pathNexus}");
String version = build.buildVariableResolver.resolve("version"); println("version = ${version}");
String groupId = build.buildVariableResolver.resolve("groupId"); println("groupId = ${groupId}");
String parentWorkspace = build.buildVariableResolver.resolve("parentWorkspace"); println("parentWorkspace = ${parentWorkspace}");
String artifactId = "OmniStore-Code";
String extension = "zip";

println("Se baja el zip...");
def pathDestino = "${parentWorkspace}/${artifactId}.${extension}";

NexusHelper helper = new NexusHelper(pathNexus);
helper.initLogger { println it }
MavenCoordinates coords = new MavenCoordinates(groupId, artifactId, version, extension);
File downloadedFile = helper.download(coords, new File(parentWorkspace)); 

println("Se descomprime el zip...");
ZipHelper.unzipFile(downloadedFile,	new File("${parentWorkspace}/OmniStore"));

