package clarive

import com.cloudbees.plugins.flow.FlowCause
import es.eci.utils.GlobalVars
import es.eci.utils.JobRootFinder;
import groovy.json.JsonSlurper
import hudson.model.*;
import es.eci.utils.clarive.ClariveConnection;
import es.eci.utils.clarive.ClariveParamsHelper;
import es.eci.utils.Stopwatch;

Long milis = Stopwatch.watch {

	GlobalVars gVars = new GlobalVars();
	def id_proceso = gVars.get(build, "id_proceso");

	ClariveConnection clConn = new ClariveConnection();
	clConn.initLogger { println it };

	def permisoClarive = build.buildVariableResolver.resolve("permisoClarive");

	if(permisoClarive != null && permisoClarive.trim().equals("true")) {

		def action = build.buildVariableResolver.resolve("action");
		if(!action.equals("build")) {
			def intentosComparativa = 60;
			def ECI_PROXY_URL = build.getEnvironment(null).get("ECI_PROXY_URL");
			def ECI_PROXY_PORT = build.getEnvironment(null).get("ECI_PROXY_PORT");
			def nakedUrl = build.getEnvironment(null).get("CLARIVE_URL");
			def api_key_clarive = build.buildVariableResolver.resolve("CLARIVE_API_KEY");
			def nombre_componente = build.buildVariableResolver.resolve("componentName");
			def version = build.buildVariableResolver.resolve("builtVersion");
			if(version == null || version.trim().equals("")) {
				version = build.buildVariableResolver.resolve("version");
			}
			def proceso = build.buildVariableResolver.resolve("tipo_proceso").toUpperCase();
			if(proceso.equals("ADDHOTFIX")) {
				proceso = "HOTFIX";
			}

			ClariveParamsHelper clHelper = new ClariveParamsHelper(build);
			clHelper.initLogger { println it }
			def nombre_producto = build.buildVariableResolver.resolve("aplicacionUrbanCode");
			//		def nombre_producto = clHelper.findArea();
			//		nombre_producto = nombre_producto.replaceAll('\\(RTC\\)','').trim();

			def nombre_subproducto = clHelper.getSubproducto();
			def tipo_corriente = clHelper.getTipoCorriente();
			def resultado = clHelper.getResultado(build);
			Map<String, String> mapMetricas = clHelper.getMetricas(build);
			def metrica_PU = mapMetricas.get("metricaPU");
			def metrica_PC = mapMetricas.get("metricaPC");

			JobRootFinder jRootFinder = new JobRootFinder();
			def rootBuild = jRootFinder.getRootBuild(build);
			def rootResult = rootBuild.getResult();
			println("Resultado del padre: " + rootResult);

			def cause = build.getCause(Cause.UpstreamCause)
			AbstractBuild run = null;
			if (cause instanceof Cause.UpstreamCause) {
				def name = ((Cause.UpstreamCause)cause).getUpstreamProject()
				def buildNumber = ((Cause.UpstreamCause)cause).getUpstreamBuild()
				run = Hudson.instance.getJob(name).getBuildByNumber(Integer.valueOf(buildNumber));
			}
			else if (cause instanceof FlowCause) {
				run = ((FlowCause) cause).getFlowRun()
			}
			println("Invocado por -> " + run.getProject().getName());

			if(rootResult != Result.ABORTED) {
				if(!run.getProject().getName().contains("-COMP-") && !run.getProject().getName().contains("- COMPNew")) {
					// Se lanza desde job de corriente.
					def cCount = gVars.get(build, "cCount");
					if(cCount != null && !cCount.trim().equals("") && !cCount.trim().startsWith("\$")) {
						cCount = cCount.toInteger();
					} else { cCount = 0; }

					String jobs = build.buildVariableResolver.resolve("jobs");
					int jobCount = 0;

					if(jobs != null && !jobs.trim().equals("")) {
						try {
							JsonSlurper js = new JsonSlurper();
							def jobsJson = js.parseText(jobs);
							jobsJson.each { subArray ->	subArray.each {	jobCount++;	} }
						} catch (Exception e) {
							println("[WARNING] Error al parsear el parámetro \"jobs\".")
							// Se intenta parsear el parámetro jobs de la forma antigua.
							try {
								jobCount = jobs.split(",").size()
							} catch (Exception ex) {
								println("[WARNING] Error al parsear el parámetro \"jobs\" de la forma antigua.")
							}
						}
					} else {
						String job = build.buildVariableResolver.resolve("job");
						if(job != null && !job.trim().equals("")) {
							jobCount = 1;
						}
					}

					int tryCount = 0;
					println("Esperando un máximo de ${intentosComparativa} segundos a que terminen todas las notificaciones a SWC03 que faltan.")
					while((cCount < jobCount) && (tryCount < intentosComparativa)) {
						tryCount++;
						println("Todavía no han acabado. cCount = ${cCount} y jobCount = ${jobCount} (${tryCount})");
						cCount = gVars.get(build, "cCount");
						if(cCount != null && !cCount.trim().equals("") && !cCount.trim().startsWith("\$")) {
							cCount = cCount.toInteger();
						} else {
							cCount = 0;
						}
						Thread.sleep(1000);
					}
				}

				/**
				 * Ejecución del servicio SWC03 de Clarive el cual
				 * informa del fin del proceso asociado al "id_proceso".
				 */
				clConn.swc03(
						nakedUrl,
						api_key_clarive,
						ECI_PROXY_URL,
						ECI_PROXY_PORT,
						proceso,
						nombre_producto,
						nombre_subproducto,
						tipo_corriente,
						nombre_componente,
						version,
						id_proceso,
						resultado,
						metrica_PU,
						metrica_PC
						);

				// Si el job raíz es el propio job de componente se
				// llama de nuevo a SWC03 para notificar el fin de proceso.
				if(rootBuild.getFullDisplayName().contains("-COMP-") || rootBuild.getFullDisplayName().contains("- COMPNew")) {
					println("Como el job raíz es el propio de componente notificaremos también el final de proceso:");
					clConn.swc03(
							nakedUrl,
							api_key_clarive,
							ECI_PROXY_URL,
							ECI_PROXY_PORT,
							action.toUpperCase(),
							nombre_producto,
							nombre_subproducto,
							tipo_corriente,
							nombre_componente,
							version,
							id_proceso,
							resultado,
							metrica_PU,
							metrica_PC
							);
				}

				if(run.getProject().getName().contains("-COMP-") || run.getProject().getName().contains("- COMPNew")) {
					String components_count = gVars.get(build, "cCount");
					if(components_count != null && !components_count.trim().equals("") && !components_count.trim().startsWith("\$")) {
						Integer cCount = components_count.toInteger();
						cCount = cCount + 1;
						gVars.put(build, "cCount", cCount.toString());
					} else {
						gVars.put(build, "cCount", "1");
					}
				}


			} else {
				println("No se ha construido el job padre.");
			}
		}
	} else {
		println("permisoClarive -> ${permisoClarive}. No llevamos a cabo la llamada al servicio SWC03.");
	}
}

println "Execution time -> ${milis} mseg."


