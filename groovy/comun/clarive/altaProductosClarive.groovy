/*********************************************/

/** Utilidad para dar de alta jobs en Clarive **/ 
/*
 * Requiere una estructura de ficheros bajo "baseDir" del tipo;
 * 
 * clariveNode.xml
 * jenkins-cli.properties
 * jenkins_int.json
 * Productos_Clarive_GIT.txt
 * Productos_Clarive_RTC.txt
 * 
 */

/*********************************************/

package clarive
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

import org.w3c.dom.Document
import org.w3c.dom.Node
import es.eci.utils.commandline.CommandLineHelper
import es.eci.utils.versioner.XmlUtils
import es.eci.utils.versioner.XmlWriter
import groovy.json.JsonSlurper

File baseDir = new File("/home/dcjimenez/Desktop/Productos Clarive");

File productosRTCFile = new File(baseDir,"Productos_Clarive_RTC.txt");
File productosGITFile = new File(baseDir,"Productos_Clarive_GIT.txt");

File jobsFile = new File(baseDir,"jobs.txt");
File jobsDir = new File(baseDir,"jobs");
limpiezaInicial(baseDir,jobsFile,jobsDir);

productosRTCFile.eachLine { String line ->
	String releaseJob = 		line + " - DESARROLLO - release"
	String deployJob = 		line + " - DESARROLLO - deploy"
	String addFixJob = 		line + " - RELEASE - addFix"
	String addHotfixJob = 	line + " - MANTENIMIENTO - addHotfix"

	jobsFile.append(releaseJob + "\n");
	jobsFile.append(deployJob + "\n");
	jobsFile.append(addFixJob + "\n");
	jobsFile.append(addHotfixJob + "\n");

}

productosGITFile.eachLine { String line ->
	String releaseJob = 		line + " - release"
	String deployJob = 		line + " - deploy"
	String addFixJob = 		line + " - addFix"
	String addHotfixJob = 	line + " - addHotfix"
	
	jobsFile.append(releaseJob + "\n");
	jobsFile.append(deployJob + "\n");
	jobsFile.append(addFixJob + "\n");
	jobsFile.append(addHotfixJob + "\n");
	
}

downloadJobs("int", false, baseDir, jobsFile);

createJobsBackup(baseDir, jobsDir);

jobsDir.eachFile { File dir ->
	if(dir.isDirectory()) {
		File configFile = new File(dir,"config.xml");

		XmlUtils xu = new XmlUtils();
		Document doc = xu.parseXml(configFile);

		Node[] paramsNodes = xu.xpathNodes(doc, 
			"/project/properties/hudson.model.ParametersDefinitionProperty/parameterDefinitions/hudson.model.BooleanParameterDefinition");

		boolean encontrado = false;
		paramsNodes.each { Node node ->
			Node nameNode = xu.getChildNode(node, "name");
			if(nameNode.getTextContent().equals("permisoClarive")) {
				// Si encontramos el parámetro "permisoClarive" lo hacemos "true".
				encontrado = true;
				Node valueNode = xu.getChildNode(node, "defaultValue");
				valueNode.setTextContent("true");
			}
		}
		
		if(!encontrado) {
			// Si no hemos encontrado el parámetro "permisoClarive" lo añadimos.
			File nodeFile = new File(baseDir,"clariveNode.xml");
			Document nodeDoc = xu.parseXml(nodeFile);
			Node clariveNode = xu.xpathNode(nodeDoc, "/hudson.model.BooleanParameterDefinition");
			
			Node paramsNode = xu.xpathNode(doc,"/project/properties/hudson.model.ParametersDefinitionProperty/parameterDefinitions");
			
			Node newClariveNode = doc.importNode(clariveNode,true);
			
			paramsNode.appendChild(newClariveNode);
			
		}
	
		XmlWriter writer = new XmlWriter();
		writer.transformXml(doc, configFile);		
		
	}
}


/************************************** Utilidades **********************************************/

public void downloadJobs (String env, boolean all, File baseDir, File jobsFile) {
	try {
		File jenkinsProperties = new File(baseDir,"jenkins-cli.properties");
		Properties p = new Properties();
		p.load(new FileInputStream(jenkinsProperties));
		String nodeCommand = p.getProperty("node");
		String jenkins_cli = p.getProperty("jenkins-cli");
		File jenkinsConfigFile = new File(baseDir,"jenkins_" + env + ".json");

		File jobsDir = new File(baseDir,"jobs");
		jobsDir.mkdirs();

		String command = "\""+ nodeCommand +"\" \"" + jenkins_cli + "\" -c \"" +  jenkinsConfigFile.getCanonicalPath() 	+ "\" -f \""+ jobsFile.getCanonicalPath() + "\" backup -j";
		executeCommand(command);

	} catch (Exception e) {
		e.printStackTrace();
	}
}


// Ejecuta un comando mediante el helper
private void executeCommand(String command) {
	System.out.println(command);
	CommandLineHelper helper = new CommandLineHelper(command);
	helper.execute();
	System.out.println(helper.getStandardOutput());
}

private limpiezaInicial(File baseDir, File jobsFile, File jobsDir) {
	if(jobsFile.exists()) {
		jobsFile.delete();
		jobsFile.createNewFile();
	}
	if(jobsDir.exists()) {
		jobsDir.deleteDir();
		jobsDir.mkdirs();
	}
}


private void createJobsBackup(File baseDir, File jobsDir) {
	File bckDir = new File(baseDir,"jobs_bck");		
	new AntBuilder().copy( todir:bckDir.getCanonicalPath() ) {
		fileset( dir:jobsDir.getCanonicalPath() )
	}
	
}


println("###### Procedemos a subir los jobs a Jenkins..... \n")
uploadJobs ("int", false, baseDir);

/**
 * Se conecta a Jenkins mediante le herramienta local jenkins-cli para subir los
 * jobs recién creados.
 *
 * Se reaprovecha el fichero de configuración del entorno, y se crea un fichero temporal
 * apuntando al directorio de instalación.
 * @param env
 */
public void uploadJobs (String env, boolean all, File baseDir) {
	try {
		//getJenkinsProfileFile(env);
		File jenkinsProperties = new File(baseDir,"jenkins-cli.properties");
		Properties p = new Properties();
		p.load(new FileInputStream(jenkinsProperties));
		String nodeCommand = p.getProperty("node");
		String jenkins_cli = p.getProperty("jenkins-cli");
		File originalJenkinsConfigFile = new File(baseDir, "jenkins_" + env + ".json");
		File finalJobs = new File(baseDir, "jobs");
		Path tempDir = null;
		try {
			tempDir = Files.createTempDirectory("tmp");
			File jenkinsConfigFile = new File(tempDir.toFile(), "jenkins.json");
			Files.copy(originalJenkinsConfigFile.toPath(),
					jenkinsConfigFile.toPath());
			updateJenkinsConfigFile(jenkinsConfigFile, finalJobs);
			
			String command = "\""+ nodeCommand +"\" \"" + jenkins_cli + "\" -c \""  + jenkinsConfigFile.getCanonicalPath()	+ "\" -f " + new File(baseDir, "jobs.txt").getCanonicalPath() + " install -j";
			if(all) {
				command = "\""+ nodeCommand +"\" \"" + jenkins_cli + "\" -c \"" + jenkinsConfigFile.getCanonicalPath() + "\" install -j";
			}
			
			executeCommand(command);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		
	} catch (Exception e) {
		System.out.println("Ha habido algún problema subiendo los jobs. " +
				"Intentelo de nuevo a mano mediante \"jenkins-cli\" si el problema persiste.");
		e.printStackTrace();
	}
}

// Actualiza el campo pipelineDirectory
private void updateJenkinsConfigFile(File file, File uploadDirectory) {
	try {
		String content = new String(
			Files.readAllBytes(file.toPath()),
			Charset.forName("UTF-8"));
		
		JsonSlurper slurper = new JsonSlurper();
		def jsonObject = slurper.parseText(file.getText());
		jsonObject.pipelineDirectory = uploadDirectory.getCanonicalPath();
				
		file.setText(jsonObject.toString());
		
	}
	catch(Exception e) {
		e.printStackTrace();
	}
}









