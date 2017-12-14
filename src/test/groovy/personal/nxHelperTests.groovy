import es.eci.utils.NexusHelper;
import es.eci.utils.pom.MavenCoordinates

String nexusUrl = "http://nexus.elcorteingles.int/content/repositories";

MavenCoordinates coordinates = new MavenCoordinates("es.eci.omnistore","OmniStore-Code","1.7.325.25-6","zip");
coordinates.setRepository("eci");

NexusHelper nh = new NexusHelper(nexusUrl);
nh.download(coordinates, new File("C:/Users/dcastro.jimenez/Desktop/tmp"));


