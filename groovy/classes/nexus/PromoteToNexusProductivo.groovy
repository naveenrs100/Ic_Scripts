package nexus

import es.eci.utils.StringUtil;
import es.eci.utils.TmpDir;
import es.eci.utils.NexusHelper;
import es.eci.utils.ZipHelper
import es.eci.utils.base.Loggable;
import es.eci.utils.pom.MavenCoordinates;
import groovy.json.JsonSlurper
import urbanCode.UrbanCodeComponentInfoService;
import urbanCode.UrbanCodeExecutor

class PromoteToNexusProductivo extends Loggable {

	String instantanea;
	String nexusUrl;
	String nexusUser;
	String nexusPass;
	String fichasGroupId;
	String aplicacionUrbanCode;
	String parentWorkspace;
	
	String udClientCommand;
	String urlUrbanCode;
	String urbanUser;
	String urbanPassword;

	public void execute() {

		NexusHelper nxHelper = new NexusHelper(nexusUrl);
		nxHelper.initLogger(this);
		MavenCoordinates coordinates = new MavenCoordinates(fichasGroupId, StringUtil.normalize(aplicacionUrbanCode), instantanea);
		coordinates.setPackaging("zip");

		TmpDir.tmp { File tmpDir ->
			File descriptorFile = nxHelper.download(coordinates, tmpDir);
			ZipHelper zipHelper = new ZipHelper();
			zipHelper.unzipFile(descriptorFile, tmpDir);

			File descriptor = new File(tmpDir,"descriptor.json");

			Object descriptoObj = new JsonSlurper().parseText(descriptor.text);
			
			boolean errorFlag = false;
			
			def badArtifactsList = [];
			
			descriptoObj.versions.each { Map<String,String> versionObj ->
				versionObj.keySet().each { String key ->

					String urbanComponent = key;
					String version = versionObj.get(key);

					/* Por cada componente, buscamos sus coordenadas en Nexus y lo 
					 * copiamos al repositorio definido como "productivo" (eci-productivo) */
					UrbanCodeExecutor urbanExecutor = new UrbanCodeExecutor(udClientCommand, urlUrbanCode, urbanUser, urbanPassword);
					UrbanCodeComponentInfoService urbanService = new UrbanCodeComponentInfoService(urbanExecutor);
					urbanService.initLogger(this);
					MavenCoordinates compoCoordinates = urbanService.getCoordinates(urbanComponent);
					compoCoordinates.setVersion(version);
					
					File componentArtifactFile;
					try {
						File downloadDir = new File(tmpDir,"composArtifacts");
						downloadDir.mkdir();
						componentArtifactFile = nxHelper.download(compoCoordinates, downloadDir);
					} catch(Exception e) {
						errorFlag = true;
						log("--WARNING: El archivo  " + 
							"${compoCoordinates.getGroupId()}:${compoCoordinates.getArtifactId()}:${compoCoordinates.getVersion()} "+
							"no ha podido descargarse de Nexus. Comprobar si sus coordenadas están bien dadas de alta en UrbanCode.");
						badArtifactsList.add(compoCoordinates.getArtifactId());
					}
					
					try {
						// Establecemos las coordenadas apuntando al repositorio de eci-productivo
						if(compoCoordinates.getRepository().equals("private-all")) {
							compoCoordinates.setRepository("eci-productivo-private");
						} else {
							compoCoordinates.setRepository("eci-productivo");
						}
						
						nxHelper.setNexus_user(nexusUser);
						nxHelper.setNexus_pass(nexusPass);
						nxHelper.upload(compoCoordinates, componentArtifactFile);

					} catch(Exception e) {
						errorFlag = true;
						if (!badArtifactsList.contains(compoCoordinates.getArtifactId())) {
							badArtifactsList.add(compoCoordinates.getArtifactId());
							log "--WARNING: El archivo  ${componentArtifactFile.getName()} no ha podido subirse a Nexus correctamente."
						}												
						log(e.getMessage());
					}
				}
			}
			
			if(errorFlag) {
				throw new Exception("Ha habido errores al pasar a productivo los siguientes artefactos: ${badArtifactsList} ");
			}
			
		}
	}
}


















