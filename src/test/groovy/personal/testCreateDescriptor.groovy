import urbanCode.UrbanCodeSnapshotDeployer;
import urbanCode.UrbanCodeExecutor;


UrbanCodeExecutor exe = new UrbanCodeExecutor("","","");


UrbanCodeSnapshotDeployer deployer = new UrbanCodeSnapshotDeployer(exe, "http://nexus.elcorteingles.pre/content/groups/public");

List<Map<String, String>> componentsVersions = [];
Map<String, String> tmpDoc = [:];
tmpDoc.put("PruebaRelease - App 2.doc", "1.0.55.0-SNAPSHOT");
componentsVersions << tmpDoc;
Map<String, String> tmp = [:];
tmpDoc.put("PruebaRelease - App 2", "1.0.55.0-SNAPSHOT");


componentsVersions.each { Map compVersion ->
	compVersion.keySet().each { String componentUrbanCode ->
		String thisComponentUrbanCode = componentUrbanCode.split("\\.doc")[0];
		String builtVersion = compVersion[thisComponentUrbanCode];
		if (builtVersion.endsWith("-SNAPSHOT")) {
			println("Calculando el timestamp de ${thisComponentUrbanCode} (para el componente ${componentUrbanCode}) en version ${builtVersion}");
			isThereOpenVersion = true;
			UrbanCodeComponentInfoService service =
					new UrbanCodeComponentInfoService(exec);
			service.initLogger(this);
			MavenCoordinates coords = service.getCoordinates(thisComponentUrbanCode);
			coords.setVersion(builtVersion);
			NexusHelper nexusHelper = new NexusHelper(urlNexus);
			nexusHelper.initLogger(this);
			// Si el repo es privado
			if ( coords.getRepository() != "public") {
				nexusHelper.setNexus_user(nexus_user)
				nexusHelper.setNexus_pass(nexus_pass)
			}
			String snapshotVersion = nexusHelper.resolveSnapshot(coords);
			println "---> Resuelta versión SNAPSHOT: $componentUrbanCode <-- $snapshotVersion";
			compVersion.put(componentUrbanCode, snapshotVersion);
		}

	}
}