import java.nio.channels.FileLock
import java.util.Map;
import java.util.logging.Logger;

import es.eci.utils.Stopwatch
import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.TmpDir
import es.eci.utils.commandline.CommandLineHelper

import git.commands.GitCloneCommand;
import rtc.RTCUtils;

/**
 * Este script clona un repositorio git en una ruta temporal y posteriormente copia
 * el contenido del clonado en un directorio final. Su objetivo es evitar que por
 * concurrencia se generen múltiples ficheros de .lock a la hora de hacer clone y
 * fallen algunas de las descargas en paralelo.
 * 
 * Así mismo, traduce la plantilla a un Dockerfile con las variables resueltas, y
 * deja la imagen construida.
 */

// Carga de variables (params de entrada)
 
SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
Map<String, String> mapaVars = propertyBuilder.getSystemParameters();

def gitUser = mapaVars["gitUser"]			// Usuario de git
def gitHost = mapaVars["gitHost"]			// Host de GitLab
def gitPath = mapaVars["gitPath"]			// Ruta del repositorio
def gitBranch = mapaVars["gitBranch"]		// Rama (normalmente develop)
def finalPath = mapaVars["finalPath"]		// Ruta donde debe copiarse al contenido que se haya clonado.
def template = mapaVars["template"]			// Nombre de la plantilla
def scriptsCore = mapaVars["scriptsCore"]	// Ruta del ScriptsCore; p. ej. /jenkins/jobs/ScriptsCore

// Lógica
 
Closure log = { println it }

log "--> Inicio de stepGitCloneDocker" 

// Sección crítica
FileLock lock = RTCUtils.getLock(template)

try {
	TmpDir.tmp { File dir ->
		GitCloneCommand comandoGit = new GitCloneCommand (gitUser, gitHost, gitPath, gitBranch, ".", 
			dir, "", "", "")
		comandoGit.initLogger(log)
		try {
			comandoGit.execute()
			builder = new AntBuilder()
			log "Copia de ficheros..."
			builder.copy(todir : "${finalPath}") {
				fileset(dir : dir.getCanonicalPath()) {
					exclude(name : ".*")
				}
			}
			log "Cambio de permisos..."
			builder.chmod(dir : "${finalPath}/scripts", perm : '775', includes : "*")
			log "Contenido del directorio temporal:"
			dir.eachFile { log it }
			log "Construyendo la imagen..."
			long millis = Stopwatch.watch {
				// Traducir la plantilla
				CommandLineHelper helper = 
					new CommandLineHelper("${scriptsCore}/workspace/bash/docker/createImage.sh");
				helper.initLogger(log);
				helper.execute(new File(finalPath));
			}
			log "Imagen Docker obtenida en ${millis} mseg."
		} 
		catch (Exception e) {
			log "Error al ejecutar clone"
			log e.getMessage()
			e.printStackTrace()		
		};
	}
}
finally {
	// Fin de la sección crítica
	lock.release();
}

log "--> Fin de stepGitCloneDocker"
