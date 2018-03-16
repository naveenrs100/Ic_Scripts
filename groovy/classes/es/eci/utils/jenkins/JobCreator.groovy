package es.eci.utils.jenkins;

import es.eci.utils.base.Loggable
import es.eci.utils.versioner.XmlUtils
import groovy.json.JsonSlurper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import es.eci.utils.jenkins.JobCreatorUtils;

public class JobCreator extends Loggable {

	public createJenkinsJobs(Map componentsMap, File destinationDir, String technology, String scm, File plantillas) {		
		if(scm.equals("rtc")) {
			createRTCMavenJobs(componentsMap, destinationDir, plantillas, technology);
		}
		if(scm.equals("git")) {
			createGITMavenJobs(componentsMap, destinationDir, plantillas, technology);
		}
	}

	/**
	 * Crea josb de Jenkins para proyectos Maven en RTC
	 * @param componentsMap
	 * @param destinationDir
	 * @param plantillas
	 * @return
	 */
	private createRTCMavenJobs(Map componentsMap, File destinationDir, File plantillas, String technology) {
		if(plantillas.exists()) {
			File buildTemplate = 	 		new File(plantillas, "Plantilla - RTC - build/config.xml");
			File releaseTemplate = 	 		new File(plantillas, "Plantilla - RTC - release/config.xml");
			File deployTemplate = 	 		new File(plantillas, "Plantilla - RTC - deploy/config.xml");
			File addFixTemplate = 	 		new File(plantillas, "Plantilla - RTC - addFix/config.xml");
			File addHotfixTemplate = 		new File(plantillas, "Plantilla - RTC - addHotfix/config.xml");
			File componentMavenTemplate = 	new File(plantillas, "Plantilla - RTC - Maven - Componente/config.xml");
			File componentBrokerTemplate =  new File(plantillas, "Plantilla - RTC - Maven - Componente Broker/config.xml");
			File componentNodeJSTemplate = 	new File(plantillas, "Plantilla - RTC - NodeJS - Componente/config.xml");
			File componentGradleTemplate = 	new File(plantillas, "Plantilla - RTC - Gradle - Componente/config.xml");

			File componentTemplate;
			switch(technology) {
				case "maven" :
					componentTemplate = componentMavenTemplate;
					break;
				case "broker" :
					componentTemplate = componentBrokerTemplate;
					break;
				case "node" :
					componentTemplate = componentNodeJSTemplate;
					break;
				case "gradle" :
					componentTemplate = componentGradleTemplate;
					break;
			}

			componentsMap.keySet().each { String product ->
				
				File productDirectory = destinationDir;
				productDirectory.mkdirs();

				XmlUtils xmlUtils = new XmlUtils();

				Map componentsParams = componentsMap.getAt(product);
				String workitem = componentsParams.getAt("workitem");
				String streamCargaInicial = componentsParams.getAt("streamCargaInicial");
				String aplicacionUrbanCode = componentsParams.getAt("aplicacionUrbanCode");	
				String managersMail = componentsParams.getAt("managersMail");
				String ordenacion = componentsParams.getAt("ordenacion");
				String permisoClarive = componentsParams.getAt("permisoClarive");
				

				// Creación de job de build de corriente:
				println("### Creación del job build para ${product}:");
				Document buildDoc = xmlUtils.parseXml(buildTemplate);
				
				Node[] descriptionNodes = xmlUtils.xpathNodes(buildDoc, "/project//description");
				descriptionNodes.each { Node descNode ->
					descNode.setTextContent("");
				}
				
				Node disabledNode = xmlUtils.xpathNode(buildDoc, "/project/disabled");
				if(disabledNode != null) {
					disabledNode.setTextContent("false");
				}

				Node buildParametersNode = xmlUtils.xpathNode(buildDoc, "/project/properties/hudson.model.ParametersDefinitionProperty/parameterDefinitions");

				Node getOrderedNode = JobCreatorUtils.modifyParameterNode("getOrdered", ordenacion, buildParametersNode, xmlUtils);
				
				Node streamParameterNode = JobCreatorUtils.modifyParameterNode("stream", product + " - DESARROLLO", buildParametersNode, xmlUtils);
				if(streamCargaInicial == null ||  streamCargaInicial.trim().equals("")) {
					Node streamCargaInicialNode = JobCreatorUtils.modifyParameterNode("streamCargaInicial", product + " - DESARROLLO", buildParametersNode, xmlUtils);
				} else {
					Node streamCargaInicialNode = JobCreatorUtils.modifyParameterNode("streamCargaInicial", streamCargaInicial, buildParametersNode, xmlUtils);
				}
				
				Node customWorkspaceNode = xmlUtils.xpathNode(buildDoc, "/project/customWorkspace");
				String wksNormalized = JobCreatorUtils.normalize(product + " - DESARROLLO");
				customWorkspaceNode.setTextContent("\$JENKINS_HOME/workspace/${wksNormalized}_\${action}");
				
				Node postBuildParamsNode = xmlUtils.xpathNode(buildDoc, "/project/publishers/hudson.plugins.parameterizedtrigger.BuildTrigger/configs/hudson.plugins.parameterizedtrigger.BuildTriggerConfig/configs/hudson.plugins.parameterizedtrigger.PredefinedBuildParameters/properties");
				if(postBuildParamsNode != null) {
					String postBuildParams = postBuildParamsNode.getTextContent();
					String newPostBuildParams = "";
					postBuildParams.eachLine { String line ->
						if(line.startsWith("managersMail")) {
							String newLine = "managersMail=${managersMail}";
							newPostBuildParams = postBuildParams.replaceAll(line, newLine);
						}
					}
					postBuildParamsNode.setTextContent(newPostBuildParams);
				}
				
				JobCreatorUtils.writeFinalJobFile(productDirectory, product, "DESARROLLO", "build", buildDoc, "rtc");


				// Creación de job de release de corriente:
				println("### Creación del job release para ${product}:");
				Document releaseDoc = xmlUtils.parseXml(releaseTemplate);
				
				descriptionNodes = xmlUtils.xpathNodes(releaseDoc, "/project//description");
				descriptionNodes.each { Node descNode ->
					descNode.setTextContent("");
				}
				
				disabledNode = xmlUtils.xpathNode(releaseDoc, "/project/disabled");
				if(disabledNode != null) {
					disabledNode.setTextContent("false");
				}

				Node releaseParametersNode = xmlUtils.xpathNode(releaseDoc, "/project/properties/hudson.model.ParametersDefinitionProperty/parameterDefinitions");
				
				Node getPermisoClariveNode = JobCreatorUtils.modifyParameterNode("permisoClarive", permisoClarive, releaseParametersNode, xmlUtils);
				
				getOrderedNode = JobCreatorUtils.modifyParameterNode("getOrdered", ordenacion, releaseParametersNode, xmlUtils);
				
				streamParameterNode = JobCreatorUtils.modifyParameterNode("stream", product + " - DESARROLLO", releaseParametersNode, xmlUtils);
				Node streamTargetParameterNode = JobCreatorUtils.modifyParameterNode("streamTarget", product + " - RELEASE", releaseParametersNode, xmlUtils);
				if(streamCargaInicial == null ||  streamCargaInicial.trim().equals("")) {
					Node streamCargaInicialNode = JobCreatorUtils.modifyParameterNode("streamCargaInicial", product + " - DESARROLLO", releaseParametersNode, xmlUtils);
				} else {
					Node streamCargaInicialNode = JobCreatorUtils.modifyParameterNode("streamCargaInicial", streamCargaInicial, releaseParametersNode, xmlUtils);
				}
				Node workItemNode = JobCreatorUtils.modifyParameterNode("workItem", "", releaseParametersNode, xmlUtils);

				Node aplicacionUrbanNode = JobCreatorUtils.modifyParameterNode("aplicacionUrbanCode", aplicacionUrbanCode, releaseParametersNode, xmlUtils);

				String entornoUrbanCode = componentsParams.getAt("entornoUbanRelease")
				Node entonrnoUrbanNode = JobCreatorUtils.modifyParameterNode("entornoUrbanCode", entornoUrbanCode, releaseParametersNode, xmlUtils);

				customWorkspaceNode = xmlUtils.xpathNode(releaseDoc, "/project/customWorkspace");
				wksNormalized = JobCreatorUtils.normalize(product + " - DESARROLLO");
				customWorkspaceNode.setTextContent("\$JENKINS_HOME/workspace/${wksNormalized}_\${action}");
				
				postBuildParamsNode = xmlUtils.xpathNode(releaseDoc, "/project/publishers/hudson.plugins.parameterizedtrigger.BuildTrigger/configs/hudson.plugins.parameterizedtrigger.BuildTriggerConfig/configs/hudson.plugins.parameterizedtrigger.PredefinedBuildParameters/properties");
				if(postBuildParamsNode != null) {
					String postBuildParams = postBuildParamsNode.getTextContent();
					String newPostBuildParams = "";
					postBuildParams.eachLine { String line ->
						if(line.startsWith("managersMail")) {
							String newLine = "managersMail=${managersMail}";
							newPostBuildParams = postBuildParams.replaceAll(line, newLine);
						}
					}
					postBuildParamsNode.setTextContent(newPostBuildParams);
				}
				
				JobCreatorUtils.writeFinalJobFile(productDirectory, product, "DESARROLLO", "release", releaseDoc, "rtc");


				// Creación de job de deploy de corriente:
				println("### Creación del job deploy para ${product}:");
				Document deployDoc = xmlUtils.parseXml(deployTemplate);
				
				descriptionNodes = xmlUtils.xpathNodes(deployDoc, "/project//description");
				descriptionNodes.each { Node descNode ->
					descNode.setTextContent("");
				}
				
				disabledNode = xmlUtils.xpathNode(deployDoc, "/project/disabled");
				if(disabledNode != null) {
					disabledNode.setTextContent("false");
				}

				Node deployParametersNode = xmlUtils.xpathNode(deployDoc, "/project/properties/hudson.model.ParametersDefinitionProperty/parameterDefinitions");
				
				getPermisoClariveNode = JobCreatorUtils.modifyParameterNode("permisoClarive", permisoClarive, deployParametersNode, xmlUtils);
				
				getOrderedNode = JobCreatorUtils.modifyParameterNode("getOrdered", ordenacion, deployParametersNode, xmlUtils);
				
				streamParameterNode = JobCreatorUtils.modifyParameterNode("stream", product + " - DESARROLLO", deployParametersNode, xmlUtils);
				if(streamCargaInicial == null ||  streamCargaInicial.trim().equals("")) {
					Node streamCargaInicialNode = JobCreatorUtils.modifyParameterNode("streamCargaInicial", product + " - DESARROLLO", deployParametersNode, xmlUtils);
				} else {
					Node streamCargaInicialNode = JobCreatorUtils.modifyParameterNode("streamCargaInicial", streamCargaInicial, deployParametersNode, xmlUtils);
				}			

				customWorkspaceNode = xmlUtils.xpathNode(deployDoc, "/project/customWorkspace");
				wksNormalized = JobCreatorUtils.normalize(product + " - DESARROLLO");
				customWorkspaceNode.setTextContent("\$JENKINS_HOME/workspace/${wksNormalized}_\${action}");

				aplicacionUrbanCode = componentsParams.getAt("aplicacionUrbanCode")
				aplicacionUrbanNode = JobCreatorUtils.modifyParameterNode("aplicacionUrbanCode", aplicacionUrbanCode, deployParametersNode, xmlUtils);

				entornoUrbanCode = componentsParams.getAt("entornoUbanDeploy")
				entonrnoUrbanNode = JobCreatorUtils.modifyParameterNode("entornoUrbanCode", entornoUrbanCode, deployParametersNode, xmlUtils);

				workItemNode = JobCreatorUtils.modifyParameterNode("workItem", workitem, deployParametersNode, xmlUtils);
				
				postBuildParamsNode = xmlUtils.xpathNode(deployDoc, "/project/publishers/hudson.plugins.parameterizedtrigger.BuildTrigger/configs/hudson.plugins.parameterizedtrigger.BuildTriggerConfig/configs/hudson.plugins.parameterizedtrigger.PredefinedBuildParameters/properties");
				if(postBuildParamsNode != null) {
					String postBuildParams = postBuildParamsNode.getTextContent();
					String newPostBuildParams = "";
					postBuildParams.eachLine { String line ->
						if(line.startsWith("managersMail")) {
							String newLine = "managersMail=${managersMail}";
							newPostBuildParams = postBuildParams.replaceAll(line, newLine);
						}
					}
					postBuildParamsNode.setTextContent(newPostBuildParams);
				}
				
				JobCreatorUtils.writeFinalJobFile(productDirectory, product, "DESARROLLO", "deploy", deployDoc, "rtc");


				// Creación de job de addFix de corriente:
				println("### Creación del job addFix para ${product}:");
				Document addFixDoc = xmlUtils.parseXml(addFixTemplate);
				
				descriptionNodes = xmlUtils.xpathNodes(addFixDoc, "/project//description");
				descriptionNodes.each { Node descNode ->
					descNode.setTextContent("");
				}
				
				disabledNode = xmlUtils.xpathNode(addFixDoc, "/project/disabled");
				if(disabledNode != null) {
					disabledNode.setTextContent("false");
				}

				Node addFixParametersNode = xmlUtils.xpathNode(addFixDoc, "/project/properties/hudson.model.ParametersDefinitionProperty/parameterDefinitions");
				
				getPermisoClariveNode = JobCreatorUtils.modifyParameterNode("permisoClarive", permisoClarive, addFixParametersNode, xmlUtils);
				
				getOrderedNode = JobCreatorUtils.modifyParameterNode("getOrdered", ordenacion, addFixParametersNode, xmlUtils);
				
				streamParameterNode = JobCreatorUtils.modifyParameterNode("stream", product + " - RELEASE", addFixParametersNode, xmlUtils);
				streamTargetParameterNode = JobCreatorUtils.modifyParameterNode("streamTarget", product + " - RELEASE", addFixParametersNode, xmlUtils);
				if(streamCargaInicial == null ||  streamCargaInicial.trim().equals("")) {
					Node streamCargaInicialNode = JobCreatorUtils.modifyParameterNode("streamCargaInicial", product + " - DESARROLLO", addFixParametersNode, xmlUtils);
				} else {
					Node streamCargaInicialNode = JobCreatorUtils.modifyParameterNode("streamCargaInicial", streamCargaInicial, addFixParametersNode, xmlUtils);
				}
				
				workItemNode = JobCreatorUtils.modifyParameterNode("workItem", "", addFixParametersNode, xmlUtils);

				aplicacionUrbanNode = JobCreatorUtils.modifyParameterNode("aplicacionUrbanCode", aplicacionUrbanCode, addFixParametersNode, xmlUtils);

				entornoUrbanCode = componentsParams.getAt("entornoUbanRelease")
				entonrnoUrbanNode = JobCreatorUtils.modifyParameterNode("entornoUrbanCode", entornoUrbanCode, addFixParametersNode, xmlUtils);

				customWorkspaceNode = xmlUtils.xpathNode(addFixDoc, "/project/customWorkspace");
				wksNormalized = JobCreatorUtils.normalize(product + " - RELEASE");
				customWorkspaceNode.setTextContent("\$JENKINS_HOME/workspace/${wksNormalized}_\${action}");
				
				postBuildParamsNode = xmlUtils.xpathNode(addFixDoc, "/project/publishers/hudson.plugins.parameterizedtrigger.BuildTrigger/configs/hudson.plugins.parameterizedtrigger.BuildTriggerConfig/configs/hudson.plugins.parameterizedtrigger.PredefinedBuildParameters/properties");
				if(postBuildParamsNode != null) {
					String postBuildParams = postBuildParamsNode.getTextContent();
					String newPostBuildParams = "";
					postBuildParams.eachLine { String line ->
						if(line.startsWith("managersMail")) {
							String newLine = "managersMail=${managersMail}";
							newPostBuildParams = postBuildParams.replaceAll(line, newLine);
						}
					}
					postBuildParamsNode.setTextContent(newPostBuildParams);
				}
				
				JobCreatorUtils.writeFinalJobFile(productDirectory, product, "RELEASE", "addFix", addFixDoc, "rtc");

				// Creación de job de addHotfix de corriente:
				println("### Creación del job addHotfix para ${product}:");
				Document addHotfixDoc = xmlUtils.parseXml(addHotfixTemplate);
				
				descriptionNodes = xmlUtils.xpathNodes(addHotfixDoc, "/project//description");
				descriptionNodes.each { Node descNode ->
					descNode.setTextContent("");
				}
				
				disabledNode = xmlUtils.xpathNode(addHotfixDoc, "/project/disabled");
				if(disabledNode != null) {
					disabledNode.setTextContent("false");
				}

				Node addHotfixParametersNode = xmlUtils.xpathNode(addHotfixDoc, "/project/properties/hudson.model.ParametersDefinitionProperty/parameterDefinitions");
				
				getPermisoClariveNode = JobCreatorUtils.modifyParameterNode("permisoClarive", permisoClarive, addHotfixParametersNode, xmlUtils);
				
				getOrderedNode = JobCreatorUtils.modifyParameterNode("getOrdered", ordenacion, addHotfixParametersNode, xmlUtils);
				
				streamParameterNode = JobCreatorUtils.modifyParameterNode("stream", product + " - MANTENIMIENTO", addHotfixParametersNode, xmlUtils);
				streamTargetParameterNode = JobCreatorUtils.modifyParameterNode("streamTarget", product + " - MANTENIMIENTO", addHotfixParametersNode, xmlUtils);
				if(streamCargaInicial == null ||  streamCargaInicial.trim().equals("")) {
					Node streamCargaInicialNode = JobCreatorUtils.modifyParameterNode("streamCargaInicial", product + " - DESARROLLO", addHotfixParametersNode, xmlUtils);
				} else {
					Node streamCargaInicialNode = JobCreatorUtils.modifyParameterNode("streamCargaInicial", streamCargaInicial, addHotfixParametersNode, xmlUtils);
				}
				workItemNode = JobCreatorUtils.modifyParameterNode("workItem", "", addHotfixParametersNode, xmlUtils);

				aplicacionUrbanNode = JobCreatorUtils.modifyParameterNode("aplicacionUrbanCode", aplicacionUrbanCode, addHotfixParametersNode, xmlUtils);

				entornoUrbanCode = componentsParams.getAt("entornoUbanRelease")
				entonrnoUrbanNode = JobCreatorUtils.modifyParameterNode("entornoUrbanCode", entornoUrbanCode, addHotfixParametersNode, xmlUtils);

				customWorkspaceNode = xmlUtils.xpathNode(addHotfixDoc, "/project/customWorkspace");
				wksNormalized = JobCreatorUtils.normalize(product + " - MANTENIMIENTO");
				customWorkspaceNode.setTextContent("\$JENKINS_HOME/workspace/${wksNormalized}_\${action}");
				
				postBuildParamsNode = xmlUtils.xpathNode(addHotfixDoc, "/project/publishers/hudson.plugins.parameterizedtrigger.BuildTrigger/configs/hudson.plugins.parameterizedtrigger.BuildTriggerConfig/configs/hudson.plugins.parameterizedtrigger.PredefinedBuildParameters/properties");
				if(postBuildParamsNode != null) {
					String postBuildParams = postBuildParamsNode.getTextContent();
					String newPostBuildParams = "";
					postBuildParams.eachLine { String line ->
						if(line.startsWith("managersMail")) {
							String newLine = "managersMail=${managersMail}";
							newPostBuildParams = postBuildParams.replaceAll(line, newLine);
						}
					}
					postBuildParamsNode.setTextContent(newPostBuildParams);
				}
				
				JobCreatorUtils.writeFinalJobFile(productDirectory, product, "MANTENIMIENTO", "addHotfix", addHotfixDoc, "rtc");


				// Creación de jobs de componentes:
				Map<String,String> componentMap = componentsParams.getAt("components");
				println("----- Componentes de ${product}:")
				componentMap.keySet().each { String component ->
					Map compoParams = componentMap.getAt(component);

					println "\t" + component + " : " + componentMap.getAt(component);

					Document componentDoc = xmlUtils.parseXml(componentTemplate);
					
					descriptionNodes = xmlUtils.xpathNodes(componentDoc, "/project//description");
					descriptionNodes.each { Node descNode ->
						descNode.setTextContent("");
					}
					
					disabledNode = xmlUtils.xpathNode(componentDoc, "/project/disabled");
					if(disabledNode != null) {
						disabledNode.setTextContent("false");
					}

					Node componentParametersNode = xmlUtils.xpathNode(componentDoc, "/project/properties/hudson.model.ParametersDefinitionProperty/parameterDefinitions");

					JobCreatorUtils.modifyParameterNode("workItem", workitem, componentParametersNode, xmlUtils);
					JobCreatorUtils.modifyParameterNode("stream", product + " - DESARROLLO", componentParametersNode, xmlUtils);
					JobCreatorUtils.modifyParameterNode("streamTarget", product + " - RELEASE", componentParametersNode, xmlUtils);
					JobCreatorUtils.modifyParameterNode("streamMantenimiento", product + " - MANTENIMIENTO", componentParametersNode, xmlUtils);
					JobCreatorUtils.modifyParameterNode("component", component, componentParametersNode, xmlUtils);
					

					if(!technology.equals("broker")) {
						String docker_template = compoParams.getAt("docker_template");
						JobCreatorUtils.modifyParameterNode("docker_template", docker_template, componentParametersNode, xmlUtils);
					}
					
					postBuildParamsNode = xmlUtils.xpathNode(componentDoc, "/project/publishers/hudson.plugins.parameterizedtrigger.BuildTrigger/configs/hudson.plugins.parameterizedtrigger.BuildTriggerConfig/configs/hudson.plugins.parameterizedtrigger.PredefinedBuildParameters/properties");					
					if(postBuildParamsNode != null) {
						String postBuildParams = postBuildParamsNode.getTextContent();
						String newPostBuildParams = "";					
						postBuildParams.eachLine { String line ->
							if(line.startsWith("managersMail")) {
								String newLine = "managersMail=${managersMail}";
								newPostBuildParams = postBuildParams.replaceAll(line, newLine);								
							}
						}
						postBuildParamsNode.setTextContent(newPostBuildParams);
					}
					
					
					String componenteUrbanCode = compoParams.getAt("componenteUrbanCode")
					JobCreatorUtils.modifyParameterNode("componenteUrbanCode", componenteUrbanCode, componentParametersNode, xmlUtils);
					
					String jdkSuffix = compoParams.getAt("jdkSuffix")
					JobCreatorUtils.modifyParameterNode("JdkSuffix", jdkSuffix, componentParametersNode, xmlUtils);

					customWorkspaceNode = xmlUtils.xpathNode(componentDoc, "/project/customWorkspace");
					wksNormalized = JobCreatorUtils.normalize(product + " - DESARROLLO_" + component);
					customWorkspaceNode.setTextContent("\$JENKINS_HOME/workspace/${wksNormalized}_\${action}");

					JobCreatorUtils.writeFinalComponentJobFile(productDirectory, product, "DESARROLLO", component, componentDoc, "rtc", streamCargaInicial)
				}

			}

		}
		else {
			println(" --- No se ha encontrado el directorio de plantillas para jobs Maven en GIT.");
		}
	}

	/**
	 * Crea josb de Jenkins para proyectos Maven en GIT
	 * @param componentsMap
	 * @param destinationDir
	 * @param plantillas
	 * @return
	 */
	private createGITMavenJobs(Map componentsMap, File destinationDir, File plantillas, String technology) {
		if(plantillas.exists()) {
			File buildTemplate = 	 new File(plantillas, "Plantilla - Git - build/config.xml");
			File releaseTemplate = 	 new File(plantillas, "Plantilla - Git - release/config.xml");
			File deployTemplate = 	 new File(plantillas, "Plantilla - Git - deploy/config.xml");
			File addFixTemplate = 	 new File(plantillas, "Plantilla - Git - addFix/config.xml");
			File addHotfixTemplate = new File(plantillas, "Plantilla - Git - addHotfix/config.xml");
			File componentMavenTemplate = new File(plantillas, "Plantilla - Git - Maven - Componente/config.xml");
			File componentNodeJSTemplate = new File(plantillas, "Plantilla - Git - NodeJS - Componente/config.xml");
			
			File componentTemplate;
			switch(technology) {
				case "maven" :
					componentTemplate = componentMavenTemplate;
					break;
				case "node" :
					componentTemplate = componentNodeJSTemplate;
					break;				
			}

			componentsMap.keySet().each { String product ->
								
				File productDirectory = destinationDir;
				productDirectory.mkdirs();

				XmlUtils xmlUtils = new XmlUtils();

				Map componentsParams = componentsMap.getAt(product);
				String aplicacionUrbanCode = componentsParams.getAt("aplicacionUrbanCode");				
				String managersMail = componentsParams.getAt("managersMail");				
				String ordenacion = componentsParams.getAt("ordenacion");

				// Creación de job de build de corriente:
				println("### Creación del job build para ${product}:");
				Document buildDoc = xmlUtils.parseXml(buildTemplate);

				Node disabledNode = xmlUtils.xpathNode(buildDoc, "/project/disabled");
				if(disabledNode != null) {
					disabledNode.setTextContent("false");
				}

				Node buildParametersNode = xmlUtils.xpathNode(buildDoc, "/project/properties/hudson.model.ParametersDefinitionProperty/parameterDefinitions");
				
				Node getOrderedNode = JobCreatorUtils.modifyParameterNode("getOrdered", ordenacion, buildParametersNode, xmlUtils);
				
				Node streamParameterNode = JobCreatorUtils.modifyParameterNode("gitGroup", product, buildParametersNode, xmlUtils);

				Node customWorkspaceNode = xmlUtils.xpathNode(buildDoc, "/project/customWorkspace");
				String wksNormalized = JobCreatorUtils.normalize(product);
				customWorkspaceNode.setTextContent("\$JENKINS_HOME/workspace/${wksNormalized}_\${action}");
				
				Node postBuildParamsNode = xmlUtils.xpathNode(buildDoc, "/project/publishers/hudson.plugins.parameterizedtrigger.BuildTrigger/configs/hudson.plugins.parameterizedtrigger.BuildTriggerConfig/configs/hudson.plugins.parameterizedtrigger.PredefinedBuildParameters/properties");
				if(postBuildParamsNode != null) {
					String postBuildParams = postBuildParamsNode.getTextContent();
					String newPostBuildParams = "";
					postBuildParams.eachLine { String line ->
						if(line.startsWith("managersMail")) {
							String newLine = "managersMail=${managersMail}";
							newPostBuildParams = postBuildParams.replaceAll(line, newLine);
						}
					}
					postBuildParamsNode.setTextContent(newPostBuildParams);
				}
				
				JobCreatorUtils.writeFinalJobFile(productDirectory, product, null, "build", buildDoc, "git");

				// Creación de job de release de corriente:
				println("### Creación del job release para ${product}:");
				Document releaseDoc = xmlUtils.parseXml(releaseTemplate);

				disabledNode = xmlUtils.xpathNode(releaseDoc, "/project/disabled");
				if(disabledNode != null) {
					disabledNode.setTextContent("false");
				}
				
				Node[] descriptionNodes = xmlUtils.xpathNodes(buildDoc, "/project//description");
				descriptionNodes.each { Node descNode ->
					descNode.setTextContent("");
				}
				
				Node releaseParametersNode = xmlUtils.xpathNode(releaseDoc, "/project/properties/hudson.model.ParametersDefinitionProperty/parameterDefinitions");
				
				getOrderedNode = JobCreatorUtils.modifyParameterNode("getOrdered", ordenacion, releaseParametersNode, xmlUtils);
				
				streamParameterNode = JobCreatorUtils.modifyParameterNode("gitGroup", product, releaseParametersNode, xmlUtils);

				Node aplicacionUrbanNode = JobCreatorUtils.modifyParameterNode("aplicacionUrbanCode", aplicacionUrbanCode, releaseParametersNode, xmlUtils);

				String entornoUrbanCode = componentsParams.getAt("entornoUbanRelease")
				Node entonrnoUrbanNode = JobCreatorUtils.modifyParameterNode("entornoUrbanCode", entornoUrbanCode, releaseParametersNode, xmlUtils);

				customWorkspaceNode = xmlUtils.xpathNode(releaseDoc, "/project/customWorkspace");
				wksNormalized = JobCreatorUtils.normalize(product);
				customWorkspaceNode.setTextContent("\$JENKINS_HOME/workspace/${wksNormalized}_\${action}");
				
				postBuildParamsNode = xmlUtils.xpathNode(releaseDoc, "/project/publishers/hudson.plugins.parameterizedtrigger.BuildTrigger/configs/hudson.plugins.parameterizedtrigger.BuildTriggerConfig/configs/hudson.plugins.parameterizedtrigger.PredefinedBuildParameters/properties");
				if(postBuildParamsNode != null) {
					String postBuildParams = postBuildParamsNode.getTextContent();
					String newPostBuildParams = "";
					postBuildParams.eachLine { String line ->
						if(line.startsWith("managersMail")) {
							String newLine = "managersMail=${managersMail}";
							newPostBuildParams = postBuildParams.replaceAll(line, newLine);
						}
					}
					postBuildParamsNode.setTextContent(newPostBuildParams);
				}
				
				JobCreatorUtils.writeFinalJobFile(productDirectory, product, null, "release", releaseDoc, "git");

				// Creación de job de deploy de corriente:
				println("### Creación del job deploy para ${product}:");
				Document deployDoc = xmlUtils.parseXml(deployTemplate);

				disabledNode = xmlUtils.xpathNode(deployDoc, "/project/disabled");
				if(disabledNode != null) {
					disabledNode.setTextContent("false");
				}
				
				descriptionNodes = xmlUtils.xpathNodes(releaseDoc, "/project//description");
				descriptionNodes.each { Node descNode ->
					descNode.setTextContent("");
				}
				
				Node deployParametersNode = xmlUtils.xpathNode(deployDoc, "/project/properties/hudson.model.ParametersDefinitionProperty/parameterDefinitions");
				
				getOrderedNode = JobCreatorUtils.modifyParameterNode("getOrdered", ordenacion, deployParametersNode, xmlUtils);
				
				streamParameterNode = JobCreatorUtils.modifyParameterNode("gitGroup", product, deployParametersNode, xmlUtils);

				customWorkspaceNode = xmlUtils.xpathNode(deployDoc, "/project/customWorkspace");
				wksNormalized = JobCreatorUtils.normalize(product);
				customWorkspaceNode.setTextContent("\$JENKINS_HOME/workspace/${wksNormalized}_\${action}");

				aplicacionUrbanCode = componentsParams.getAt("aplicacionUrbanCode")
				aplicacionUrbanNode = JobCreatorUtils.modifyParameterNode("aplicacionUrbanCode", aplicacionUrbanCode, deployParametersNode, xmlUtils);

				entornoUrbanCode = componentsParams.getAt("entornoUbanDeploy")
				entonrnoUrbanNode = JobCreatorUtils.modifyParameterNode("entornoUrbanCode", entornoUrbanCode, deployParametersNode, xmlUtils);
				
				postBuildParamsNode = xmlUtils.xpathNode(deployDoc, "/project/publishers/hudson.plugins.parameterizedtrigger.BuildTrigger/configs/hudson.plugins.parameterizedtrigger.BuildTriggerConfig/configs/hudson.plugins.parameterizedtrigger.PredefinedBuildParameters/properties");
				if(postBuildParamsNode != null) {
					String postBuildParams = postBuildParamsNode.getTextContent();
					String newPostBuildParams = "";
					postBuildParams.eachLine { String line ->
						if(line.startsWith("managersMail")) {
							String newLine = "managersMail=${managersMail}";
							newPostBuildParams = postBuildParams.replaceAll(line, newLine);
						}
					}
					postBuildParamsNode.setTextContent(newPostBuildParams);
				}
				
				JobCreatorUtils.writeFinalJobFile(productDirectory, product, null, "deploy", deployDoc, "git");

				// Creación de job de addFix de corriente:
				println("### Creación del job addFix para ${product}:");
				Document addFixDoc = xmlUtils.parseXml(addFixTemplate);

				disabledNode = xmlUtils.xpathNode(addFixDoc, "/project/disabled");
				if(disabledNode != null) {
					disabledNode.setTextContent("false");
				}
				
				descriptionNodes = xmlUtils.xpathNodes(addFixDoc, "/project//description");
				descriptionNodes.each { Node descNode ->
					descNode.setTextContent("");
				}
				
				Node addFixParametersNode = xmlUtils.xpathNode(addFixDoc, "/project/properties/hudson.model.ParametersDefinitionProperty/parameterDefinitions");
				
				getOrderedNode = JobCreatorUtils.modifyParameterNode("getOrdered", ordenacion, addFixParametersNode, xmlUtils);
				
				streamParameterNode = JobCreatorUtils.modifyParameterNode("gitGroup", product, addFixParametersNode, xmlUtils);

				aplicacionUrbanNode = JobCreatorUtils.modifyParameterNode("aplicacionUrbanCode", aplicacionUrbanCode, addFixParametersNode, xmlUtils);

				entornoUrbanCode = componentsParams.getAt("entornoUbanRelease")
				entonrnoUrbanNode = JobCreatorUtils.modifyParameterNode("entornoUrbanCode", entornoUrbanCode, addFixParametersNode, xmlUtils);

				customWorkspaceNode = xmlUtils.xpathNode(addFixDoc, "/project/customWorkspace");
				wksNormalized = JobCreatorUtils.normalize(product);
				customWorkspaceNode.setTextContent("\$JENKINS_HOME/workspace/${wksNormalized}_\${action}");
				
				postBuildParamsNode = xmlUtils.xpathNode(addFixDoc, "/project/publishers/hudson.plugins.parameterizedtrigger.BuildTrigger/configs/hudson.plugins.parameterizedtrigger.BuildTriggerConfig/configs/hudson.plugins.parameterizedtrigger.PredefinedBuildParameters/properties");
				if(postBuildParamsNode != null) {
					String postBuildParams = postBuildParamsNode.getTextContent();
					String newPostBuildParams = "";
					postBuildParams.eachLine { String line ->
						if(line.startsWith("managersMail")) {
							String newLine = "managersMail=${managersMail}";
							newPostBuildParams = postBuildParams.replaceAll(line, newLine);
						}
					}
					postBuildParamsNode.setTextContent(newPostBuildParams);
				}
				
				JobCreatorUtils.writeFinalJobFile(productDirectory, product, null, "addFix", addFixDoc, "git");

				// Creación de job de addHotfix de corriente:
				println("### Creación del job addHotfix para ${product}:");
				Document addHotfixDoc = xmlUtils.parseXml(addHotfixTemplate);

				disabledNode = xmlUtils.xpathNode(addHotfixDoc, "/project/disabled");
				if(disabledNode != null) {
					disabledNode.setTextContent("false");
				}
				
				descriptionNodes = xmlUtils.xpathNodes(addHotfixDoc, "/project//description");
				descriptionNodes.each { Node descNode ->
					descNode.setTextContent("");
				}
				
				Node addHotfixParametersNode = xmlUtils.xpathNode(addHotfixDoc, "/project/properties/hudson.model.ParametersDefinitionProperty/parameterDefinitions");
				
				getOrderedNode = JobCreatorUtils.modifyParameterNode("getOrdered", ordenacion, addHotfixParametersNode, xmlUtils);
				
				streamParameterNode = JobCreatorUtils.modifyParameterNode("gitGroup", product, addHotfixParametersNode, xmlUtils);

				aplicacionUrbanNode = JobCreatorUtils.modifyParameterNode("aplicacionUrbanCode", aplicacionUrbanCode, addHotfixParametersNode, xmlUtils);

				entornoUrbanCode = componentsParams.getAt("entornoUbanRelease")
				entonrnoUrbanNode = JobCreatorUtils.modifyParameterNode("entornoUrbanCode", entornoUrbanCode, addHotfixParametersNode, xmlUtils);

				customWorkspaceNode = xmlUtils.xpathNode(addHotfixDoc, "/project/customWorkspace");
				wksNormalized = JobCreatorUtils.normalize(product);
				customWorkspaceNode.setTextContent("\$JENKINS_HOME/workspace/${wksNormalized}_\${action}");
				
				postBuildParamsNode = xmlUtils.xpathNode(addHotfixDoc, "/project/publishers/hudson.plugins.parameterizedtrigger.BuildTrigger/configs/hudson.plugins.parameterizedtrigger.BuildTriggerConfig/configs/hudson.plugins.parameterizedtrigger.PredefinedBuildParameters/properties");
				if(postBuildParamsNode != null) {
					String postBuildParams = postBuildParamsNode.getTextContent();
					String newPostBuildParams = "";
					postBuildParams.eachLine { String line ->
						if(line.startsWith("managersMail")) {
							String newLine = "managersMail=${managersMail}";
							newPostBuildParams = postBuildParams.replaceAll(line, newLine);
						}
					}
					postBuildParamsNode.setTextContent(newPostBuildParams);
				}
				
				JobCreatorUtils.writeFinalJobFile(productDirectory, product, null, "addHotfix", addHotfixDoc, "git");

				// Creación de jobs de componentes:
				Map<String,String> componentMap = componentsParams.getAt("components");
				println("----- Componentes de ${product}:")
				componentMap.keySet().each { String component ->
					Map compoParams = componentMap.getAt(component);

					println "\t" + component + " : " + componentMap.getAt(component);

					Document componentDoc = xmlUtils.parseXml(componentTemplate);

					disabledNode = xmlUtils.xpathNode(componentDoc, "/project/disabled");
					if(disabledNode != null) {
						disabledNode.setTextContent("false");
					}
					
					descriptionNodes = xmlUtils.xpathNodes(componentDoc, "/project//description");
					descriptionNodes.each { Node descNode ->
						descNode.setTextContent("");
					}
					
					Node componentParametersNode = xmlUtils.xpathNode(componentDoc, "/project/properties/hudson.model.ParametersDefinitionProperty/parameterDefinitions");
					
					postBuildParamsNode = xmlUtils.xpathNode(componentDoc, "/project/publishers/hudson.plugins.parameterizedtrigger.BuildTrigger/configs/hudson.plugins.parameterizedtrigger.BuildTriggerConfig/configs/hudson.plugins.parameterizedtrigger.PredefinedBuildParameters/properties");
					if(postBuildParamsNode != null) {
						String postBuildParams = postBuildParamsNode.getTextContent();
						String newPostBuildParams = "";
						postBuildParams.eachLine { String line ->
							if(line.startsWith("managersMail")) {
								String newLine = "managersMail=${managersMail}";
								newPostBuildParams = postBuildParams.replaceAll(line, newLine);
							}
						}
						postBuildParamsNode.setTextContent(newPostBuildParams);
					}
					
					JobCreatorUtils.modifyParameterNode("gitGroup", product, componentParametersNode, xmlUtils);
					JobCreatorUtils.modifyParameterNode("component", component, componentParametersNode, xmlUtils);

					String docker_template = compoParams.getAt("docker_template")
					JobCreatorUtils.modifyParameterNode("docker_template", docker_template, componentParametersNode, xmlUtils);

					String componenteUrbanCode = compoParams.getAt("componenteUrbanCode")
					JobCreatorUtils.modifyParameterNode("componenteUrbanCode", componenteUrbanCode, componentParametersNode, xmlUtils);
					
					String jdkSuffix = compoParams.getAt("jdkSuffix")
					JobCreatorUtils.modifyParameterNode("JdkSuffix", jdkSuffix, componentParametersNode, xmlUtils);

					customWorkspaceNode = xmlUtils.xpathNode(componentDoc, "/project/customWorkspace");
					wksNormalized = JobCreatorUtils.normalize(product + " - " + component);
					customWorkspaceNode.setTextContent("\$JENKINS_HOME/workspace/${wksNormalized}_\${action}");

					JobCreatorUtils.writeFinalComponentJobFile(productDirectory, product, null, component, componentDoc, "git")
				}

			}

		}
		else {
			println(" --- No se ha encontrado el directorio de plantillas para jobs Maven en RTC.");
		}
	}

}
















