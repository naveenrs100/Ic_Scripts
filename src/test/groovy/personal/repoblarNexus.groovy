import es.eci.utils.NexusHelper;
import es.eci.utils.pom.MavenCoordinates;

String nexusUrl = "http://nexus.elcorteingles.pre/content/repositories";
File file = new File("C:/Users/dcastro.jimenez/Downloads/lib1.jar");
int count = 10;

NexusHelper nh = new NexusHelper(nexusUrl);
nh.setNexus_user("U_ICNEXUS");
nh.setNexus_pass("zru3y4Yf");

for(int i=1; i <= count; i++) {
	String versionRelease = "55.${i+1}.${i}.0";
	String versionfix = "44.0.0.${i}";
	String versionHotfix = "44.0.0.8-${i}";
	
	//////// RELEASES
	MavenCoordinates coordinates = new MavenCoordinates(
		"es.eci.release.prueba",
		"lib1",
		versionRelease);
	coordinates.setRepository("eci");
	coordinates.setPackaging("jar");
	nh.upload(coordinates, file);
	
	/////// FIX
	coordinates = new MavenCoordinates(
		"es.eci.release.prueba",
		"lib1",
		versionfix);
	coordinates.setRepository("eci");
	coordinates.setPackaging("jar");
	nh.upload(coordinates, file);
	
	//////// HOTFIX
	coordinates = new MavenCoordinates(
		"es.eci.release.prueba",
		"lib1",
		versionHotfix);
	coordinates.setRepository("eci");
	coordinates.setPackaging("jar");	
	nh.upload(coordinates, file);
}

