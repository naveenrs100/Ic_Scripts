package buildtree

import hudson.model.Result;

/**
 * Esta clase modela la información de un build
 */
class BuildBean {
	
	//-------------------------------------------------------------------------
	// Propiedades del bean
	
	// Nombre del job en jenkins
	private String name;
	// Descripción del job en jenkins
	private String description;
	// Identificador de la ejecución
	private Integer buildNumber;
	// Resultado de la ejecución
	private Result result;
	// Medidor de profundidad (se ha calculado en una búsqueda en profundidad
	//	que podríamos querer acotar en un momento dado)
	private Integer depth;
	// Duración de la ejecución en milisegundos
	private Long duration;
	// Extracto del log
	private List<String> logTail;

	//-------------------------------------------------------------------------
	// Métodos del bean
	
	// Sobreescribe el toString estándar
	@Override public String toString() {
		return "${name} #${buildNumber} ($duration msec)"
	}

	// Compara un build a otro: son iguales si coinciden nombre del job en jenkins
	//	y número de ejecución.  Se utiliza para descartar duplicados en la lista debido
	//	a logs un poco 'tramposos' que devuelvan un mismo job a distintas alturas. 
	public boolean equals(Object obj) {
		BuildBean bb = (BuildBean) obj;
		return this.name.equals(obj.name) && this.buildNumber.equals(obj.buildNumber);
	}

	/**
	 * Constructor con la información básica.
	 * @param name Nombre del job en jenkins.
	 * @param buildNumber Número de ejecución.
	 * @param result Resultado de la ejecución.
	 * @param depth Profundidad del bean en el árbol (depende del punto de
	 * partida de la búsqueda en profundidad).
	 */
	public BuildBean(String name, Integer buildNumber, Result result, Integer depth = 0) {
		this.name = new String(name.toString()).trim();
		this.buildNumber = buildNumber;
		this.result = result;
		this.depth = depth;
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
	public Result getResult() {
		return result;
	}

	/**
	 * Asigna el resultado de la ejecución.
	 * @param result Resultado de la ejecución.
	 */
	public void setResult(Result result) {
		this.result = result;
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
	
	
}
