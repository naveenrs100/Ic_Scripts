package urbanCode

import es.eci.utils.NexusHelper
import es.eci.utils.base.Loggable
import es.eci.utils.pom.MavenCoordinates

/**
 * Esta clase despliega componentes en versión -SNAPSHOT en un entorno 
 */
class UrbanCodeSnapshotDeployer extends Loggable {

	//---------------------------------------------------------------
	// Propiedades de la clase
	
	// Ejecutor de comandos en Urban
	private UrbanCodeExecutor exec = null;
	// URL de Nexus
	private String urlNexus = null;
	
	//---------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Inicializa el despliegue con un ejecutor, creado apuntando al
	 * entorno correcto de Urban con su autenticación y ubicación del
	 * ejecutable.
	 * @param executor Ejecutor de Urban Code debidamente configurado
	 * @param urlNexus URL del servidor Nexus para resolver los timestamp 
	 */
	public UrbanCodeSnapshotDeployer(UrbanCodeExecutor executor, String urlNexus) {
		this.exec = executor;
		this.urlNexus = urlNexus;
	}
	
	/**
	 * Realiza el despliegue en Urban Code de las versiones seleccionadas
	 * en el entorno indicado.
	 * @param componentVersions Lista de pares componente/versión
	 * @param urbanCodeApp Aplicación urban code a la que se despliega
	 * @param urbanCodeEnv Entorno Urban en el que realizar el despliegue
	 * @param nightlyName Nombre de la snapshot nocturna, por defecto 'nightly'
	 */
	public void deploySnapshotVersions(
			List<Map<String, String>> componentsVersions, 
			String urbanCodeApp,
			String urbanCodeEnv,
			String nightlyName = "nightly") {
		log "Versiones a desplegar -> ${componentsVersions}"
		// Esta variable indica si se ha encontrado alguna versión terminada
		//	en -SNAPSHOT
		boolean isThereOpenVersion = false;
		// Resolver las versiones -SNAPSHOT si fuera necesario
		componentsVersions.each { Map compVersion ->
			compVersion.keySet().each { String componentUrbanCode ->
				String builtVersion = compVersion[componentUrbanCode];
				if (builtVersion.endsWith("-SNAPSHOT")) {
					isThereOpenVersion = true;
					UrbanCodeComponentInfoService service =
						new UrbanCodeComponentInfoService(exec);
					service.initLogger(this);
					MavenCoordinates coords = service.getCoordinates(componentUrbanCode);
					coords.setVersion(builtVersion);
					NexusHelper nexusHelper = new NexusHelper(urlNexus);
					nexusHelper.initLogger(this);
					String snapshotVersion = nexusHelper.resolveSnapshot(coords);
					println "---> Resuelta versión SNAPSHOT: $componentUrbanCode <-- $snapshotVersion";
					compVersion.put(componentUrbanCode, snapshotVersion);
				}
			}
		}
		log "Versiones definitivas a desplegar -> ${componentsVersions}"
		UrbanCodeSnapshot nightly =
			new UrbanCodeSnapshot(
				nightlyName,
				urbanCodeApp,
				nightlyName,
				componentsVersions);
		// Lanzar los createVersion correspondientes
		componentsVersions.each { Map<String, String> compVersion ->
			compVersion.keySet().each { String componentName ->
				String componentVersion = compVersion[componentName];
				try {
					UrbanCodeComponentVersion componentVersionObject =
						new UrbanCodeComponentVersion(
							componentName, componentVersion, null, null);
					exec.createVersion(componentVersionObject);
				}
				catch(Exception e) {
					log "WARNING: no se ha conseguido crear la versión $componentVersion del componente $componentName"
				}
			}
		}
		// Eliminar la nightly si existe
		try {
			exec.deleteSnapshot(urbanCodeApp, nightlyName);
			log "---> Nightly eliminada."
		}
		catch(Exception e) {
			log "---> La nightly no existía previamente; por lo tanto, no se ha conseguido eliminar."
		}
		// Enviar la nightly a Urban Code
		exec.createSnapshot(nightly);
		if (urbanCodeEnv != null && urbanCodeEnv.trim().length() > 0) {
			// Mandar el despliegue
			UrbanCodeApplicationProcess process =
				new UrbanCodeApplicationProcess(
					urbanCodeApp,
					Constants.DEPLOY_PROCESS,
					urbanCodeEnv,
					true,
					nightlyName,
					isThereOpenVersion?["ETIQUETA":"-SNAPSHOT"]:[:]);
			exec.requestApplicationProcess(process)
		}
	}
}
