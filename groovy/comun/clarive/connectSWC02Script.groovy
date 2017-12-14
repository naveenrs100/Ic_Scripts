package clarive

import es.eci.utils.GlobalVars
import es.eci.utils.JobRootFinder
import es.eci.utils.Stopwatch;
import es.eci.utils.clarive.ClariveConnection;
import es.eci.utils.clarive.ClariveParamsHelper;
import hudson.model.*;


Long milis = Stopwatch.watch {

	GlobalVars gVars = new GlobalVars();
	def prov_id = gVars.get(build, "id_proceso");

	ClariveConnection clConn = new ClariveConnection();
	clConn.initLogger { println it };

	ClariveParamsHelper clHelper = new ClariveParamsHelper(build);
	clHelper.initLogger { println it }
	def producto = clHelper.findArea();
	producto = producto.replaceAll('\\(RTC\\)','').trim();

	def subproducto = clHelper.getSubproducto();
	def tipo_corriente = clHelper.getTipoCorriente();

	def prov_cod_release = build.buildVariableResolver.resolve("codigo_release"); // Viene de la variable "instantanea" del Job.
	def prov_cod_version = build.buildVariableResolver.resolve("codigo_version"); // Viene indicado en caso de que se llame desde componente. Si no, da igual el valor.
	def permisoClarive = build.buildVariableResolver.resolve("permisoClarive");
	def ECI_PROXY_URL = build.getEnvironment(null).get("ECI_PROXY_URL");
	def ECI_PROXY_PORT = build.getEnvironment(null).get("ECI_PROXY_PORT");
	def nakedUrl = build.getEnvironment(null).get("CLARIVE_URL");
	def api_key_clarive = build.buildVariableResolver.resolve("CLARIVE_API_KEY");
	def action = build.buildVariableResolver.resolve("action");
	def conexionClarive = build.getEnvironment(null).get("CLARIVE_CONNECTION");

	def instantanea = build.buildVariableResolver.resolve("instantanea");


	if((permisoClarive != null && permisoClarive.trim().equals("true")) && (action.equals("release") || action.equals("addFix") || action.equals("addHotfix"))) {

		if(instantanea == null || instantanea.trim().equals("")) {
			// Modelo 2 (Release lanzada mediante tag en git)
			if(build.buildVariableResolver.resolve("builtVersion") != null && !build.buildVariableResolver.resolve("builtVersion").trim().equals("")) {
				prov_cod_release = build.buildVariableResolver.resolve("builtVersion");
				prov_cod_release = build.buildVariableResolver.resolve("builtVersion");
			} else {
				println("[WARNING] El parámetro \"builtVersion\" esta vacio.");
			}

		} else if(instantanea != null && !instantanea.trim().equals("")) {
			// Modelo 1 (Release lanzada desde job de proceso tradicional)
			if(build.buildVariableResolver.resolve("instantanea") != null && !build.buildVariableResolver.resolve("instantanea").trim().equals("")) {
				prov_cod_release = build.buildVariableResolver.resolve("instantanea");
			} else {
				println("[WARNING] El parámetro \"instantanea\" no puede ser vacio.");
			}
		}

		/** COMIENZA LA CONEXIÓN CON SWC02 **/
		if(conexionClarive == "true") {
			def proceso = action.toUpperCase();
			if(proceso.equals("ADDHOTFIX")) {
				proceso = "HOTFIX";
			}

			JobRootFinder jRootFinder = new JobRootFinder();
			def rootBuild = jRootFinder.getRootBuild(build);

			// Si estamos en el job raíz lanzamos la llamada a SWC02. Si no, no.
			if(build.getCause(Cause.UpstreamCause) == null) {
				// Si el id_proceso indicado viene vacío (caso de una petición no generada desde Clarive).
				if(prov_id == null || prov_id.trim().equals("") || prov_id.trim().equals("null") || prov_id.trim().equals("\${id_proceso}")) {
					println("#### Se pide permiso a Clarive mediante SWC02 ya que el id_proceso que nos ha llegado es \"${prov_id}\"...");

					Map<String,String> ret = [:];
					ret = clConn.swc02(
							nakedUrl,
							api_key_clarive,
							ECI_PROXY_URL,
							ECI_PROXY_PORT,
							proceso,
							prov_cod_release,
							prov_cod_version,
							producto,
							subproducto,
							tipo_corriente);

					// Si una vez pedido permiso a Clarive mediante SWC02 nos devuelve un resultado "KO", abortamos la petición.
					if(ret.get("res").trim().equals("KO")) {
						throw new Exception("[ERROR DESDE CLARIVE] Clarive no ha dado permiso para ejecutar esta release debido a: \"" + ret.get("msg") + "\"");
					}

					def id_proceso = ret.get("id_proceso");
					println("Devuelto id_proceso: \"${id_proceso}\".")

					// Si el id_proceso que ha devuelto Clarive no es válido no se setea ninguno
					if(id_proceso == null || id_proceso.trim().equals("") || id_proceso.trim().equals("null")) {
						println("El id_proceso devuelto por el servicio SWC02 de Clarive no es válido. No establecemos ninguno por ahora.");
					} else {
						// Se pone como variable global el id_proceso devuelto por Clarive.
						gVars.put(build, "id_proceso","${id_proceso}");
					}

					// Seteamos la instantánea al código de release que nos devuelva el servicio SWC02
					def new_instantanea = ret.get("instantanea");
					if(new_instantanea != null && !new_instantanea.trim().equals("null") && !new_instantanea.trim().equals("")) {
						gVars.delete(build, "instantanea");
						gVars.put(build, "instantanea", "${new_instantanea}");
					}

				}
				// Si el id_proceso indicado viene relleno (caso de una petición generada desde Clarive).
				else {
					println("Se intenta ejecutar la release con el \"id_proceso\" informado \"${prov_id}\".");
				}
			}
			else {
				println("Al no estar en el job raíz no ejecutamos la llamada a SWC02.");
			}

		} else {
			println("No es necesario llamar a Clarive:");
			println("action -> ${action}")
			println("conexionClarive -> ${conexionClarive}")
			println("permisoClarive -> ${permisoClarive}")
		}

	} else {
		println("action -> ${action}. No se pasa por SWC02 a Clarive.")
	}

}

println "Execution time -> ${milis} mseg."



