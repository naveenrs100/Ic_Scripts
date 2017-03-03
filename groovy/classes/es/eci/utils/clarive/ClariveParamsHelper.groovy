package es.eci.utils.clarive;

import hudson.model.*;
import rtc.RTCUtils;
import urbanCode.UrbanCodeComponentInfoService;
import urbanCode.UrbanCodeExecutor;
import es.eci.utils.JobRootFinder;
import es.eci.utils.NexusHelper;
import es.eci.utils.pom.MavenCoordinates;
import groovy.json.*;

import com.cloudbees.plugins.flow.FlowCause;

class ClariveParamsHelper {

	def build;
	def resolver;
	def gitGroup;
	def stream;

	def rtcUser;
	def rtcPass;
	def rtcUrl;

	def udClientCommand;
	def urlUrbanCode;
	def userUrban;
	def passUrban;

	def componenteUrbanCode;
	def nexusUrl;
	def component;
	def action;
	def projectArea;
	def given_result;

	public ClariveParamsHelper(build) {
		this.build = build;
		resolver = build.buildVariableResolver;
		gitGroup = resolver.resolve("gitGroup");
		stream = resolver.resolve("stream");
		rtcUser = build.getEnvironment(null).get("userRTC");
		rtcPass = resolver.resolve("pwdRTC");
		rtcUrl = build.getEnvironment(null).get("urlRTC");
		udClientCommand = build.getEnvironment(null).get("UDCLIENT_COMMAND");
		urlUrbanCode = build.getEnvironment(null).get("UDCLIENT_URL");
		userUrban = build.getEnvironment(null).get("UDCLIENT_USER");
		passUrban = resolver.resolve("UDCLIENT_PASS");
		componenteUrbanCode = resolver.resolve("componenteUrbanCode");
		nexusUrl = build.getEnvironment(null).get("ROOT_NEXUS_URL");
		component = resolver.resolve("component");
		action = resolver.resolve("action");
		projectArea = resolver.resolve("projectAreaUUID");
		given_result = resolver.resolve("resultado");

	}


	/**
	 * Define el parámetro "tipoCorriente" dependiedo
	 * de la acción.
	 * @param action
	 * @return tipoCorriente
	 */
	public String getTipoCorriente() {
		def tipoCorriente;
		if(action.trim().equals("build") || action.trim().equals("release")) {
			tipoCorriente = "DESARROLLO";

		} else if(action.trim().equals("addFix")) {
			tipoCorriente = "RELEASE";

		} else if(action.trim().equals("addHotfix")) {
			tipoCorriente = "MANTENIMIENTO";
		}
		return tipoCorriente;
	}

	/**
	 * Calcula el área de proyecto RTC
	 * @return projectArea
	 */
	public String getProjectArea() {
		def ret;
		if(gitGroup == null || gitGroup.trim().equals("")) {
			if(projectArea == null || projectArea.trim().equals("") || projectArea.trim().equals("\${projectArea}")) {
				RTCUtils ru = new RTCUtils();
				def pa = ru.getProjectArea(
						"${stream}",
						"${rtcUser}",
						"${rtcPass}",
						"${rtcUrl}");						
				ret = pa;
			} else {
				ret = projectArea;
			}
		} else {
			ret = gitGroup.trim();
		}
	}

	/**
	 * Calcula el subproducto a partir del gitGroup o de la stream.
	 * @return subproducto
	 */
	public String getSubproducto() {
		def subproducto;
		def streamSuffixes = ["DESARROLLO","RELEASE","MANTENIMIENTO","DEVELOPMENT"];
		if(gitGroup != null && !gitGroup.trim().equals("") && !gitGroup.trim().equals("\${gitGroup}")) {
			subproducto = gitGroup.trim();

		} else if(stream != null && !stream.trim().equals("") && !stream.trim().equals("\${stream}")) {
			subproducto = stream;
			for(suffix in streamSuffixes) {
				subproducto = subproducto.split("- ${suffix}")[0].split("-${suffix}")[0];
			}
		}
		return subproducto;
	}

	/**
	 * Recoge builtVersion
	 * @param build
	 * @return builtVersion
	 */
	public String getBuiltVersion() {
		// Calculamos el parámetro "builtVersion" que en este momento ya estará seteado en el build de componente.
		// Buscar si es posible el componente
		List<AbstractBuild> fullTree = JobRootFinder.getFullExecutionTree(build);
		AbstractBuild componentBuild = build;
		// Buscar el job de componente
		for (AbstractBuild ancestor: fullTree) {
			if (ancestor.getProject().getName().contains("-COMP-")) {
				componentBuild = ancestor;
			}
		}

		def compoResolver = componentBuild.buildVariableResolver;
		def builtVersion = compoResolver.resolve("builtVersion");

		return builtVersion;
	}

	/**
	 * Calcula la version de maven a partir del GAV devuelto por UrbanCode
	 * @return version_maven
	 */
	public String getVersionMaven(String builtVersion) {
		def version_maven;
		try {
			UrbanCodeExecutor urbExe = new UrbanCodeExecutor(udClientCommand,urlUrbanCode,userUrban,passUrban);
			UrbanCodeComponentInfoService compoInfo = new UrbanCodeComponentInfoService(urbExe);
			compoInfo.initLogger { println it };

			if(componenteUrbanCode != null && !componenteUrbanCode.trim().equals("")) {
				MavenCoordinates mvnCoord = compoInfo.getCoordinates("${componenteUrbanCode}");
				if(!builtVersion.endsWith("-SNAPSHOT")) {
					mvnCoord.setVersion("${builtVersion}-SNAPSHOT");
				} else {
					mvnCoord.setVersion("${builtVersion}");
				}
				NexusHelper nexusHelper = new NexusHelper(nexusUrl);
				version_maven = nexusHelper.resolveSnapshot(mvnCoord);
				println("La \"version_maven\" calculada desde Nexus es \"${version_maven}\"");

			} else {
				println("La variable \"componenteUrbanCode\" no viene indicada para el componente \"${component}\". Usamos \"${builtVersion}\".");
				version_maven = "${builtVersion}";
			}

		} catch(Exception e) {
			println("No se ha podido calcular el timestamp de Nexus para la version ${builtVersion}.");
			version_maven = "${builtVersion}";
		}

		return version_maven;
	}

	/**
	 * Saca las métricas de Pruebas Unitarias.
	 * @param build
	 * @return metricas
	 */
	public Map<String,String> getMetricas() {
		Map<String,String> metricas = [:];
		def causa = build.getCause(Cause.UpstreamCause);
		if(causa == null) {
			causa = build.getCause(FlowCause);
		}

		def jobName = causa.getUpstreamProject();
		def buildNumber = causa.getUpstreamBuild().toInteger();

		println("Sacamos métrica para el job \"${jobName}\" y build \"${buildNumber}\" si procede...");

		def job = hudson.model.Hudson.instance.getJob(jobName);

		// Sólo sacamos métrica si se trata de un job de componente.
		if(jobName.toString().contains("-COMP-")) {
			println("... sí procede sacar métricas al ser el job padre de componente.");
			if(job != null) {
				def buildInvoker = job.getBuildByNumber(buildNumber);
				def actions = buildInvoker.getActions();
				def testMap = [:];
				def jacocoMap = [:];
				def coberturaMap = [:];
				actions.each { action ->
					def claseAction = action.getClass();
					def nombreClaseAction = claseAction.getName();
					if(nombreClaseAction == 'hudson.tasks.junit.TestResultAction') {
						testMap.put('junittotal',action.getTotalCount());
						testMap.put('junitfailed',action.getFailCount());
						testMap.put('junitskiped',action.getSkipCount());
						testMap.put('junithealthScaleFactor',action.getHealthScaleFactor());

					} else if(nombreClaseAction == 'hudson.plugins.jacoco.JacocoBuildAction') {
						def jacocoResult = action.getResult();
						println("JacocoResult:");
						println(jacocoResult);
						jacocoMap.put('branchCoverage', jacocoResult.branch.percentage);
						jacocoMap.put('complexityScore', jacocoResult.complexity.percentage);
						jacocoMap.put('instructionCoverage', jacocoResult.instruction.percentage);
						jacocoMap.put('methodCoverage', jacocoResult.method.percentage);
						jacocoMap.put('lineCoverage', jacocoResult.line.percentage);
						jacocoMap.put('lineTotal', jacocoResult.line.total);
						jacocoMap.put('classCoverage', jacocoResult.clazz.percentage);

					} else if(nombreClaseAction == 'hudson.plugins.cobertura.CoberturaBuildAction') {
						// TODO: Sacar los datos de Cobertura (Sólo lo usa Omnistore)
					}
				}

				def jsonTest = JsonOutput.toJson(testMap);
				def jsonJacoco = JsonOutput.toJson(jacocoMap);

				metricas.put("metricaPU", "${jsonTest}");
				metricas.put("metricaPC", "${jsonJacoco}");
			}

		} else {
			println("El job padre es de corriente. No mostramos métricas.");
		}
		return metricas;
	}

	/**
	 * Recoge el resultado del job padre lo setea a los resultados entendibles por Clarive.
	 * @param build
	 * @return resultado
	 */
	public String getResultado() {
		def causa = build.getCause(Cause.UpstreamCause);
		if(causa == null) {
			causa = build.getCause(FlowCause);
		}

		def jobName = causa.getUpstreamProject();
		def buildNumber = causa.getUpstreamBuild().toInteger();

		def resultado = causa.getUpstreamRun().getResult();


		if(resultado == null || !resultado.toString().trim().equals("SUCCESS")) {
			resultado = "INCORRECTO";
		} else if(resultado.toString().trim().equals("SUCCESS")) {
			resultado = "CORRECTO";
		}

		if(given_result != null && !given_result.trim().equals("")) {
			println("El resultado del job padre ya viene indicado: ${given_result}");
			resultado = given_result;
		} else {
			resultado = resultado;
		}
		return resultado;
	}

}
