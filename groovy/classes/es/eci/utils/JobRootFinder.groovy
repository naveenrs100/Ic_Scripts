package es.eci.utils

import java.util.List;

import hudson.model.AbstractBuild;
import hudson.model.Cause;
import com.cloudbees.plugins.flow.FlowCause;
import es.eci.utils.base.Loggable;
import hudson.model.*;

public class JobRootFinder extends Loggable {
	
	// Build padre (el de nivel más alto; normalmente, el frontal de corriente
	//	o bien el frontal de componente)
	private AbstractBuild root;

	/**
	 * Obtiene, a partir de una ejecución de un job en jenkins, la raíz del
	 * árbol de llamadas de esa ejecución.  Por ejemplo, para un árbol:
	 *
	 * GIS - Mi Corriente - DESARROLLO - build
	 * Trigger
	 * GIS - Mi Corriente - DESARROLLO -COMP- Mi Componente
	 * WorkflowMavenBuild
	 * Controller
	 * stepCompileMaven
	 *
	 * Si este método se llama desde cualquier altura del árbol, devuelve la
	 * ejecución de 'GIS - Mi Corriente - DESARROLLO - build' que haya desencadenado
	 * todas las demás.
	 * @param run Ejecución actual.
	 * @return Ejecución padre de la ejecución actual.
	 */
	public AbstractBuild getRootBuild(AbstractBuild run) {
		def list = []
		getRootBuildI(run, list)
		if (list == null || list.size() == 0) {
			// Caso trivial: accedemos desde la raíz del árbol de ejecución
			list.add(run)
		}
		return list!=null && list.size() > 0?list[list.size()-1]:null
	}

	/**
	 * Inmersión recursiva de la búsqueda del build padre.  Se limita a
	 *	ir obteniendo el build asociado a cada causa, acumulándolos en la lista.
	 *	El padre de todos será el último de la lista
	 * @param run Ejecución actual.
	 * @param list Lista sobre la que se acumulan los resultados
	 */
	private static void getRootBuildI(AbstractBuild run, List<AbstractBuild> list) {
		def cause = getCause(run)
		if (cause != null) {
			def father = getParentRun(cause)
			if (father != null) {
				list.add(father)
				getRootBuildI(father, list)
			}
		}
	}

	/**
	 * Devuelve la causa de una determinada ejecución.
	 * @param run Ejecución cuya causa necesitamos obtener.
	 * @return La causa de la ejecución si la tuviera, null en otro caso.
	 */
	private static Cause getCause(AbstractBuild run) {
		def cause = null;
		cause = run.getCause(Cause.UpstreamCause);
		if (cause == null) {
			cause = run.getCause(FlowCause);
		}
		return cause;
	}

	/**
	 * Dada una causa, devuelve la ejecución correspondiente.
	 * @param cause Causa de una ejecución.
	 * @return Ejecución que ha disparado dicha causa.
	 */
	private static AbstractBuild getParentRun(Cause cause) {
		def run = null;
		if (cause instanceof Cause.UpstreamCause) {
			def name = ((Cause.UpstreamCause)cause).getUpstreamProject()
			def buildNumber = ((Cause.UpstreamCause)cause).getUpstreamBuild()
			run = Hudson.instance.getJob(name).getBuildByNumber(Integer.valueOf(buildNumber))
		}
		else if (cause instanceof FlowCause) {
			run = ((FlowCause) cause).getFlowRun()
		}

		return run;
	}

	/**
	 * Construye un objeto de acceso a variables globales a partir de
	 * la ejecución actual.
	 * @param build Ejecución actual.
	 */
	public JobRootFinder(AbstractBuild build) {
		root = getRootBuild(build);
	}
	
	/** 
	 * Devuelve el árbol completo de ejecución por encima del job indicado
	 * @return Árbol completo de ejecución
	 */
	public static List getFullExecutionTree(AbstractBuild build) {
		List list = [];
		getRootBuildI(build, list)
		if (list == null || list.size() == 0) {
			// Caso trivial: accedemos desde la raíz del árbol de ejecución
			list.add(build)
		}
		return list;
	}
	
	/**
	 * Devuelve la raíz del árbol de ejecución del job inicial.
	 * @return Ancestro del job inicial.
	 */
	public AbstractBuild getRoot() {
		return root;
	}
	
}
