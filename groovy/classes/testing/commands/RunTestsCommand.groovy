package testing.commands

import es.eci.utils.NexusHelper;
import es.eci.utils.TmpDir;
import es.eci.utils.StringUtil;
import es.eci.utils.base.Loggable
import es.eci.utils.commandline.CommandLineHelper;
import es.eci.utils.pom.MavenCoordinates;
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;

import java.util.zip.ZipFile;

class RunTestsCommand extends Loggable {

	public String aplicacion;
	public String instantanea;
	public String nexusUrl;
	public String testParams;
	public String volumePath;
	public String dockerRegistry;
	public String urbanGroupId;
	public String gitGroup;


	public RunTestsCommand(String aplicacion, String instantanea,
	String nexusUrl, String testParams, String volumePath, 
	String dockerRegistry, String urbanGroupId, String gitGroup) {
		super();
		this.aplicacion = aplicacion;
		this.instantanea = instantanea;
		this.nexusUrl = nexusUrl;
		this.testParams = testParams;
		this.volumePath = volumePath;
		this.dockerRegistry = dockerRegistry;
		this.urbanGroupId = urbanGroupId;
		this.gitGroup = gitGroup;
	}

	public void execute() {
		TmpDir.tmp { File dir ->
			NexusHelper	nxHelper = new NexusHelper(nexusUrl);

			def theGroupId = "${urbanGroupId}";
			def theArtifactId = StringUtil.normalize(aplicacion)
			def theVersion = instantanea;

			def testingComponentName;
			def testingComponentVersion;

			MavenCoordinates coord = new MavenCoordinates(theGroupId, theArtifactId, theVersion);
			coord.setPackaging("zip");

			File downloadedFile = nxHelper.download(coord, dir)

			def zipFile = new ZipFile(downloadedFile)

			String contenidoFicha;

			zipFile.entries().each {
				contenidoFicha = zipFile.getInputStream(it).text;
				log("Ficha de despliegue descargada:\n" + JsonOutput.prettyPrint(contenidoFicha));
			}

			if(contenidoFicha != null) {
				JsonSlurper slurper = new JsonSlurper();
				def jsonFicha = slurper.parseText(contenidoFicha);
				jsonFicha.versions.each { Map<String,String> version ->
					version.keySet().each { String key ->
						if(key.endsWith("testing")) {
							testingComponentName = key;
							testingComponentVersion = version.get(key);
						}
					}
				}
			}

			if(testingComponentName != null) {

				// Example:
				// docker run -d -e AAA=BBB -e BBB=WWW --name testing_ic_03 192.168.56.97:5000/supermercado-2016/api-super-testing:0.0.0.0-snapshot
				// docker run -d -P --name web -v (outside) /src/webapp:/webapp (inside) training/webapp python app.py
				
				// Creamos el directorio para los datos de aplicación
				CommandLineHelper permVolume = new CommandLineHelper("mkdir -p /storage/docker/${volumePath} && sudo chown -R testuser:testuser /storage/docker/${volumePath}");
				permVolume.initLogger(this);
				permVolume.execute();
				
				def timeMilis = System.currentTimeMillis().toString();
				def test_container_name = StringUtil.normalize(testingComponentName).toLowerCase() + "_${timeMilis}";
				def test_image = ("${dockerRegistry}/${gitGroup}/${testingComponentName}:${testingComponentVersion}").toLowerCase();
				log("Imagen de testing -> ${test_image}");

				def dockerCommand = "docker run -d ${testParams} --name ${test_container_name} -v /storage/docker/${volumePath}:/storage ${test_image}";

				log("Se levanta el contenedor docker: ${dockerCommand}")

				CommandLineHelper clHelper = new CommandLineHelper(dockerCommand);
				clHelper.initLogger(this);
				
				clHelper.execute();
				
//				if (ret == 0)
//					log "--- INFO: OK"
//				else {
//					log "### ERROR: Error al levantar el contenedor: ${ret}"
//					throw new Exception("Error en construccion")
//				}

			} else {
				throw new Exception("No existe componente de testing para esta ficha de despliegue.");
			}
		}
	}
}
