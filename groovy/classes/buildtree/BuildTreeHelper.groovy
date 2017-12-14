package buildtree

import java.util.EnumSet;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.concurrent.TimeUnit;

import es.eci.utils.ParamsHelper
import es.eci.utils.StringUtil
import es.eci.utils.base.Loggable
import hudson.model.AbstractBuild
import hudson.model.Hudson
import hudson.model.Result

/**
 * Esta clase implementa la lectura del árbol de ejecución de un job en jenkins.
 * Debe ejecutarse siempre en un 'System groovy script', puesto que accede
 * a la instancia de la clase Hudson para inferir información de las ejecuciones,
 * tiempos, logs y hasta la descripción de los jobs.
 */

class BuildTreeHelper extends Loggable {
	
	//-------------------------------------------------------------------------
	// Propiedades de la clase
	
	// Número de líneas (desde la última) a guardar de cada log de construcción
	private int lines = 0;
	
	//-------------------------------------------------------------------------
	// Métodos de la clase
	
	// Constructor por defecto
	public BuildTreeHelper() {
		this(200);
	}
	
	/**
	 * Constructor que indica además la profundidad del log
	 * @param lines Número de líneas (desde la última) a guardar de cada log
	 */
	public BuildTreeHelper(int lines) {
		this.lines = lines;
	}
	
	/**
	 * Este método devuelve el recorrido en profundidad de las ejecuciones de job
	 * en jenkins a partir de la pasada como parámetro.
	 * @param jobName Nombre del job cuyo árbol de ejecución queremos construir.
	 * @param buildNumber Número de ejecución del job que queremos recorrer.
	 * @return Lista de ejecuciones de jobs, ordenada en profundidad, lanzadas a partir
	 * de la que se ha pasado como parámetro. 
	 */
	public List<BuildBean> executionTree(String jobName, Integer buildNumber) {	
		def b = Thread.currentThread().executable;
		def resolver = b.buildVariableResolver;
		
		AbstractBuild build = Hudson.getInstance().getJob(jobName).getBuildByNumber(buildNumber)
		return executionTree(build);
	}
	
	/**
	 * Este método devuelve el recorrido en profundidad de las ejecuciones de job
	 * en jenkins a partir de la pasada como parámetro.
	 * @param build Objeto con la información de ejecución del job indicado
	 * @return Lista de ejecuciones de jobs, ordenada en profundidad, lanzadas a partir
	 * de la que se ha pasado como parámetro. 
	 */
	public List<BuildBean> executionTree(AbstractBuild build) {
		// Lista de ocurrencias registradas		
		def ocurrencias = []		
		// Lista con el resultado final
		List<BuildBean> resultList = [
			new BuildBean(build)	
		]
		
		log "root project name: ${build.getProject().name}"
		
		boolean keepOn = true;
		ocurrencias = findJobChildren(build, ocurrencias)
		while (keepOn) {
		  BuildBean item = head(ocurrencias);
		  if (item == null) {
		    keepOn = false;
		  }
		  else {
		    ocurrencias = tail(ocurrencias);
			String name = item.getName()
		    def number = item.getBuildNumber()
		    AbstractBuild proyecto = Hudson.getInstance().getJob(name).getBuildByNumber(number);
		    if (proyecto != null) {
		      item.setDuration(proyecto.getDuration());
		      item.setResult(proyecto.getResult().toString());
			  String description = firstLine(proyecto.project.description);
			  if (description == null || description.trim().length() == 0) {
				  description = name;
			  } 
		      item.setDescription(description);
			  item.setLogTail(proyecto.getLog(this.lines));
			  // Intentar recuperar la versión construida
			  String builtVersion = ParamsHelper.getParam(proyecto, "builtVersion");
			  if (!StringUtil.isNull(builtVersion)) {
				  item.setBuiltVersion(builtVersion);
			  }
			  else if (!StringUtil.isNull(ParamsHelper.getParam(proyecto, "version"))) {
			  	  // Algunos build legacy llevan un parámetro version
				  item.setBuiltVersion(ParamsHelper.getParam(proyecto, "version"));
			  }
			  // Intentar recuperar el componente construido
			  // Algunos wf antiguos tienen jobs que acaban en -COMP-
			  // En estos casos el nombre de componente se encuentra en 
			  //	un parámetro 'artifactId' o bien 'component'
			  if (!StringUtil.isNull(ParamsHelper.getParam(proyecto, "component"))) {
				  item.setComponent(ParamsHelper.getParam(proyecto,"component"));
			  }
			  else if (!StringUtil.isNull(ParamsHelper.getParam(proyecto,"artifactId"))) {
				  	item.setComponent(ParamsHelper.getParam(proyecto,"artifactId"));
			  }
		      ocurrencias = findJobChildren(proyecto, ocurrencias, item.getDepth())
		      
		      resultList.add(item)
		    }
		  }
		}
		return resultList;
	}
	
	/** 
	 * Convierte una lista de líneas en un texto 
	 * @param lines Lista de líneas
	 * @return Representación de la lista de líneas como cadena
	 */
	public static String toString(List<String> lines) {
		StringBuilder sb = new StringBuilder("");
		if (lines != null) {
			lines.each { String line ->
				sb.append(line);
				sb.append("\n");
			}
		}
		return sb.toString();
	}
	
	// Devuelve la primera línea de la descripción de un job
	private String firstLine(String description) {
	  String ret = "";
	  def lines = []
	  description.eachLine { line ->
	    lines << line
	  }
	  if (lines.size() > 0) {
	    ret = lines[0]
	  }
	  return ret;
	}
	
	// Devuelve la cabeza de la lista (1º elemento solamente)
	private BuildBean head(List<BuildBean> list) {
	  def ret = null;
	  if (list.size() > 0 && list != null) {
	    ret = list.get(0);
	  }
	  return ret;
	}
	
	// Devuelve la cola de la lista (elementos desde el 2º en adelante)
	private List<BuildBean> tail(List<BuildBean> list) {
	  def ret = list;
	  if (list != null && list.size() == 1) {
	    ret = []
	  }
	  else if (list != null && list.size() > 1) {
	    ret = []
	    for (int i = 1; i < list.size(); i++) {
	      ret.add(list[i])
	    }
	  }
	  return ret;
	}
	
	/**
	 * Obtiene los hijos del job cuyo log pasamos como parámetro, los añade
	 * al principio de la lista de ocurrencias y la devuelve.
	 * @param buildInvoker Construcción padre que parseamos para obtener las
	 * 	ejecuciones hijas
	 * @param ocurrencias
	 * @param parent Objeto con la profundidad del padre, se usa para determinar
	 * la profundidad de los hijos.  Si no viene informado, los hijos salen con
	 * profundidad cero.
	 * @return Lista de ocurrencias restantes, con los hijos añadidos al principio
	 * 	si hubiera habido alguno
	 */
	private List<BuildBean> findJobChildren(
			AbstractBuild buildInvoker, 
			List<BuildBean> ocurrencias, 
			Integer parentDepth = null) {
	    List<BuildBean> result = []
	    def regexp = /\[0m(.*)\s(#\d+)\scompleted/
		String log = buildInvoker.getLog();
	    log.eachLine { String line ->
	      def matcher = (line =~ regexp)
	    
	      matcher.each { match ->
	      
	        if (match != null) {
			  String childExecutionName = match[1].toString() 
			  Integer childExecutionNumber = Integer.valueOf(match[2].toString().substring(1))
			  def job = Hudson.getInstance().getJob(childExecutionName).
								  	getBuildByNumber(childExecutionNumber);
			  if (job != null) {
			      def ocurrencia = 
			            new BuildBean(childExecutionName, 
			                          childExecutionNumber,
									  job.getStartTimeInMillis(),
			                          Result.fromString(match[3].toString()).toString(), 
									  (parentDepth == null?0:parentDepth + 1));
		          if (!ocurrencias.contains(ocurrencia)) {
		          	result.add(ocurrencia)
		          }            
				  else {
					  // A veces, el log puede presentarnos una ejecución "demasiado pronto",
					  //	mostrándonosla en el log de una ejecución que no es el padre 
					  //	inmediato.  Para corregir esto, recuperamos la ocurrencia y actualizamos
					  //	su profundidad si nos la volvemos a encontrar más abajo
					  ocurrencias.findAll { BuildBean it -> 
						  it.getName().equals(ocurrencia.getName()) &&
						  	it.getBuildNumber().equals(ocurrencia.getBuildNumber())
					  }.each { BuildBean it ->
					  	// Actualizar la profundidad
					  	it.setDepth(parentDepth + 1)
					  }
				  }
			  }
			  else {
				  println "Descartando ${childExecutionName}:${childExecutionNumber} puesto que no se encuentra ya el job"
			  }
	        }
	      }
	    }
	    result.addAll(ocurrencias)
	    return result
	}
		
	/**
	 * Devuelve solo las hojas de una lista de beans	
	 * @param beans Lista de beans que representa una ejecución
	 * @return Lista que contiene solo los elementos de mayor profundidad
	 * 	de la lista original
	 */
	public List<BuildBean> leafs(List<BuildBean> beans) {
		int maxDepth = 0;
		// Añadir solo los de mayor profundidad
		return beans.findAll { it.getDepth() > 1 }
	}
}