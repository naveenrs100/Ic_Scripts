// listado de tecnologías soportadas
// $JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/version/stepFileVersioner.groovy
import groovy.json.*
import groovy.io.FileVisitResult
import es.eci.ic.version.Versioner
import es.eci.ic.version.VersionerFactory
import es.eci.utils.Utiles;

/**
 * Este script aglutina la funcionalidad de versionado, similar a lo que hace
 * el plugin de release de maven pero adaptado a los propósitos de ECI
 * Parámetros de entrada:
 * 
 * --- OBLIGATORIOS
 * technology Tecnología de construcción
 * parentWorkspace Directorio de ejecución
 * action Acción a realizar de entre las acciones posibles:
 *  build: Se limita a construir la versión indicada sin hacer más comprobaciones
 *	addFix: Sería el equivalente al release2release, cambia el dígito 4
 *	addHotfix: Mantenimiento correctivo de emergencia, cambia el dígito 5 (o lo pone
 *		si la versión no lo tenía asignado)
 *	removeSnapshot: Paso 1 del procedimiento de release.  Retira el -SNAPSHOT de las versiones
 *		de los ficheros de construcción
 *	addSnapshot: Paso 2 del procedimiento de release.  Una vez hecha y etiquetada en RTC la release,
 *		incrementa el dígito 3 y pone el -SNAPSHOT para dejar la aplicación preparada para el
 *		desarrollo de su próxima release.
 * save Puede adoptar los siguientes valores:
 *  false: No hace nada
 *  new: Guarda la versión nueva decidida por el script en el version.txt
 *  old: Guarda la versión que tuviera el fichero de construcción en el version.txt
 * checkSnapshot Puede adoptar los siguientes valores:
 *  true: Comprueba que, si hubiera alguna dependencia en -SNAPSHOT en los 
 *  	ficheros de construcción, se refiera a artefactos incluidos en la construcción.
 *  	Para ello, se apoya en el fichero artifacts.json construido al calcular el reactor.
 *  false: Si se quisiera escribir el fichero de versión con checkSnapshot==false, pero la
 *  	versión en el fichero de construcción acaba en -SNAPSHOT, da error también
 * checkErrors Si vale 'true', el método es tolerante a errores
 *	de formato o bien a que exista más de un fichero de construcción de
 *	primer nivel
 * homeStream Directorio físico en el que se construye la corriente (y se aloja el 
 * 	artifacts.json en caso de ser necesario)
 * changeVersion Indica si el método actualiza el formato de versión
 *	antiguo (3 dígitos) al nuevo (4 + hotFix opcional).  Vale 'true' o bien 'false'
 * fullCheck 
 * 
 * -- OPCIONALES
 * releaseMantenimiento Indica si el proceso de release (en realidad, la acción 'addSnapshot')
 * 	se comporta como una release de mantenimiento, actualizando el 4º dígito.  En caso de
 *  valer false o no venir informado, se comporta como el estándar
 */


//---------------> Variables entrantes
def technology = args[0]
def parentWorkspace = args[1]
def action = args[2]
def save = args[3]
def checkSnapshot = args[4]
def checkErrors = args[5]
def homeStream = args[6]
def changeVersion = args[7]
def fullCheck = Boolean.FALSE

Boolean releaseMantenimiento = Utiles.toBoolean(Utiles.readOptionalParameter(args, 8))

//-------------------> Lógica

Versioner versioner = VersionerFactory.getVersioner({ println it },technology,action,parentWorkspace,save,checkSnapshot,checkErrors,homeStream,changeVersion,fullCheck)
versioner.setReleaseMantenimiento(releaseMantenimiento)
versioner.write()