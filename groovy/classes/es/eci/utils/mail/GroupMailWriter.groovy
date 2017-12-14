package es.eci.utils.mail

import org.jaxen.function.StartsWithFunction

import buildtree.BuildBean
import hudson.model.Result

/**
 * Esta clase implementa la escritura de líneas de correo
 * para el mail de grupos
 */
class GroupMailWriter extends MailWriter {

	@Override
	public void addHTML(
			Map resultsTable,
			StringBuilder html, 
			BuildBean build, 
			int linesNumber, 
			boolean sendAlways, 
			boolean addLog) {		
		// Pinta componentes y pasos, excepto los de notificación
		if (build.getName().contains("-COMP-")
			|| build.getName().contains("COMPNew")
			|| (build.getName().startsWith("step")
				&& !build.getName().startsWith("stepNotifier"))) {
			writeLine(resultsTable, html, build, linesNumber, sendAlways, addLog);
		}
	}

}
