package buildtree

import es.eci.utils.ParamsHelper
import hudson.model.AbstractBuild

/**
 * Esta clase modela la información de un build
 */
class BuildBean implements Comparable {
	
	//-------------------------------------------------------------------------
	// Propiedades del bean
	
	// Nombre del job en jenkins
	private String name;
	// Descripción del job en jenkins
	private String description;
	// Identificador de la ejecución
	private Integer buildNumber;
	// Versión construida
	private String builtVersion;
	// Nombre del componente
	private String component;
	// Resultado de la ejecución
	private String result;
	// Medidor de profundidad (se ha calculado en una búsqueda en profundidad
	//	que podríamos querer acotar en un momento dado)
	private Integer depth;
	// Duración de la ejecución en milisegundos
	private Long duration;
	// Extracto del log
	private List<String> logTail;
	// Momento de la ejecución
	private Long timestamp;
	// Lista de hijos calculada para mostrarla en el correo
	private List<BuildBean> children;

	//-------------------------------------------------------------------------
	// Métodos del bean
	
	// Sobreescribe el toString estándar
	@Override public String toString() {
		return "${name} #${buildNumber} ($duration msec)"
	}

	// Compara un build a otro: son iguales si coinciden nombre del job en jenkins
	//	y número de ejecución.  Se utiliza para descartar duplicados en la lista debido
	//	a logs un poco 'tramposos' que devuelvan un mismo job a distintas alturas. 
	@Override public boolean equals(Object obj) {
		BuildBean bb = (BuildBean) obj;
		return this.name.equals(bb.name) && this.buildNumber.equals(bb.buildNumber);
	}

	/**
	 * Constructor con la información básica de un build de jenkins.
	 * @param build Ejecución de un job en jenkins
	 */
	public BuildBean(AbstractBuild build) {
		this(build.getParent().getName(), 
			build.getNumber(), 
			build.getTimestamp().getTimeInMillis(), 
			build.getResult().toString())
		this.builtVersion = ParamsHelper.getParam(build, "builtVersion");
		this.description = null;
		this.duration = build.getDuration();
	}
	
	/**
	 * Constructor con la información básica.
	 * @param name Nombre del job en jenkins.
	 * @param buildNumber Número de ejecución.
	 * @param timestamp Instante del inicio del job en un ejecutor.
	 * @param result Resultado de la ejecución.
	 * @param depth Profundidad del bean en el árbol (depende del punto de
	 * partida de la búsqueda en profundidad).
	 */
	public BuildBean(
			String name, Integer buildNumber,
			long timestamp,
			String result, 
			Integer depth = 0) {
		this.name = new String(name.toString()).trim();
		this.buildNumber = buildNumber;
		this.timestamp = timestamp;
		this.result = result;
		this.depth = depth;
		this.children = []
	}

	/**
	 * @return Nombre del job en jenkins.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Resultado de la ejecución.
	 */
	public String getResult() {
		return result;
	}

	/**
	 * Asigna el resultado de la ejecución.
	 * @param result Resultado de la ejecución.
	 */
	public void setResult(String result) {
		this.result = result.toString();
	}

	/**
	 * Devuelve la descripción del job en jenkins. 
	 * @return Descripción del job en jenkins.
	 */
	public String getDescription(){
		return description;
	}

	/**
	 * Asigna la descripción del job en jenkins. 
	 * @param description Descripción del job en jenkins.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Devuelve el identificador de la ejecución.
	 * @return Identificador de la ejecución.
	 */
	public Integer getBuildNumber() {
		return buildNumber;
	}

	/**
	 * Devuelve la profundidad del bean en el árbol (depende del punto de
	 * partida de la búsqueda en profundidad).
	 * @return Profundidad del bean en el árbol.
	 */
	public Integer getDepth() {
		return depth;
	}
	
	/**
	 * Actualiza la profundidad de la ocurrencia
	 * @param depth Profundidad del bean en el árbol
	 */
	public void setDepth(Integer depth) {
		this.depth = depth;
	}

	/**
	 * Asigna la duración de la ejecución en milisegundos.
	 * @param duration Duración de la ejecución en milisegundos.
	 */
	public void setDuration(Long duration) {
		this.duration = duration;
	}

	/**
	 * Devuelve la duración de la ejecución en milisegundos.
	 * @return Duración de la ejecución en milisegundos.
	 */
	public Long getDuration() {
		return duration;
	}

	/**
	 * @return Últimas X líneas del log
	 */
	public List<String> getLogTail() {
		return logTail;
	}

	/**
	 * @param logTail Últimas X líneas del log
	 */
	public void setLogTail(List<String> logTail) {
		this.logTail = logTail;
	}
	
	/**
	 * Asigna la versión construida de cada build
	 * @param builtVersion Versión construida
	 */
	public void setBuiltVersion(String builtVersion) {
		this.builtVersion = builtVersion;
	}
	
	/**
	 * @return Versión construida del job
	 */
	public String getBuiltVersion() {
		return builtVersion;
	}

	/**
	 * @return the component
	 */
	public String getComponent() {
		return component;
	}

	/**
	 * @param component the component to set
	 */
	public void setComponent(String component) {
		this.component = component;
	}

	/**
	 * @return the timestamp
	 */
	public Long getTimestamp() {
		return timestamp;
	}
	
	@Override
	public int compareTo(Object o) {
		return this.timestamp - o.timestamp;
	}
	
	@Override
	public int hashCode() {
		String id = name + "#" + buildNumber;
		return id.hashCode();
	}

	/**
	 * @return the children
	 */
	public List<BuildBean> getChildren() {
		return children;
	}

	/**
	 * @param children the children to set
	 */
	public void setChildren(List<BuildBean> children) {
		this.children = children;
	}
	
}
