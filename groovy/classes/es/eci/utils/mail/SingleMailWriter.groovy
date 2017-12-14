package es.eci.utils.mail

import buildtree.BuildBean
import hudson.model.Result

/**
 * Implementación de línea de correo para construcciones de componente
 */
class SingleMailWriter extends MailWriter {
	
	@Override
	public void addHTML(
			Map resultsTable,
			StringBuilder html, 
			BuildBean build, 
			int linesNumber, 
			boolean sendAlways, 
			boolean addLog) {
		// Excluye todo menos los pasos
		if (build.getName().startsWith("step")) {
			writeLine(resultsTable, html, build, linesNumber, sendAlways, addLog);
		}
	}
}
