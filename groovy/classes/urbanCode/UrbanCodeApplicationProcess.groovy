package urbanCode

import java.util.Map;

import es.eci.utils.base.JSONBean

/**
 * Codifica un json de este aspecto
 {
		  "application": "00010-moonshine",
		  "applicationProcess": "Despliegue APP",
		  "environment": "NFT",
		  "onlyChanged": "false",
		  "snapshot": "ENTORNO-VERSION"
 }
 Utilizado para lanzar el despliegue de una instantánea
 */
class UrbanCodeApplicationProcess extends JSONBean {
	
	//--------------------------------------------------------------
	// Propiedades de la clase
	
	// Identificador de aplicación (tiene que existir en urban code) 
	private String application;
	// Identificador de proceso (definido como constante)
	private String applicationProcess;
	// Entorno de destino (tiene que existir en urban code)
	private String environment;
	// Solo si ha habido cambios
	private boolean onlyChanged;
	// Identificador de la instantánea
	private String snapshot;	
	// Propiedades del componente: [clave1:valor1,clave2:valor2,...]
	private Map<String, String> properties = [:];
	
	//--------------------------------------------------------------
	// Métodos de la clase
	
	
	/**
	 * @param application Identificador de aplicación (tiene que existir en urban code) 
	 * @param applicationProcess Identificador de proceso (definido como constante)
	 * @param environment Entorno de destino (tiene que existir en urban code)
	 * @param onlyChanged Solo si ha habido cambios
	 * @param snapshot Identificador de la instantánea
	 */
	public UrbanCodeApplicationProcess(String application,
			String applicationProcess, String environment, boolean onlyChanged,
			String snapshot, Map<String,String> properties = null) {
		super();
		this.application = application;
		this.applicationProcess = applicationProcess;
		this.environment = environment;
		this.onlyChanged = onlyChanged;
		this.snapshot = snapshot;
		this.properties = properties;
	}
	
	/**
	 * Construye el proceso a partir de un objeto UrbanCodeSnapshot y el resto
	 * de opciones		
	 * @param objSnapshot Objeto con la información de la instantánea
	 * @param applicationProcess Identificador de proceso (definido como constante)
	 * @param environment Entorno de destino (tiene que existir en urban code)
	 * @param onlyChanged Solo si ha habido cambios
	 */
	public UrbanCodeApplicationProcess(UrbanCodeSnapshot objSnapshot, 
			String applicationProcess, String environment, boolean onlyChanged) {
		this(objSnapshot.getApplication(), 
			 applicationProcess, 
			 environment, 
			 onlyChanged, 
			 objSnapshot.getName());
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getApplicationProcess() {
		return applicationProcess;
	}

	public void setApplicationProcess(String applicationProcess) {
		this.applicationProcess = applicationProcess;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public boolean isOnlyChanged() {
		return onlyChanged;
	}

	public void setOnlyChanged(boolean onlyChanged) {
		this.onlyChanged = onlyChanged;
	}

	public String getSnapshot() {
		return snapshot;
	}

	public void setSnapshot(String snapshot) {
		this.snapshot = snapshot;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
}