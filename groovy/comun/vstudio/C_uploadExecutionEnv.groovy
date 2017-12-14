package vstudio

import es.eci.utils.NexusHelper
import es.eci.utils.ZipHelper

/**
 * Se ejecuta en el esclavo.
 * Este script crea un entregable zip para nexus a partir del entorno de ejecución
 */

def env = System.getenv()

def action = env['action']
		
def artifactId = env['artifactId']
def groupId = env['groupId']
def version = env['version']
		
def workspace = env['WORKSPACE']
def parentWorkspace = env['parentWorkspace']		
		
// El entorno de ejecución se ha construido sobre el parentWorkspace

// INI - GDR - 01/12/2016 - Cambio para contemplar otro tipo de ejecuciones como release
boolean isRelease = false

if ( action in ['release', 'addHotfix', 'addFix'] )
    isRelease = true
// FIN - GDR - 01/12/2016
		
def nexusSnapshotsC = env["C_NEXUS_SNAPSHOTS_URL"]
def nexusReleaseC = env["C_NEXUS_RELEASES_URL"]
def pathNexus = ""
if (isRelease) {
	println "Repositorio destino: " + nexusReleaseC
	pathNexus = nexusReleaseC
} else {
	println "Repositorio destino: " + nexusSnapshotsC
	pathNexus = nexusSnapshotsC
}
		
File tmpZip = ZipHelper.addDirToArchive(new File(parentWorkspace))
NexusHelper.uploadToNexus(env['WINDOWS_VS_MAVEN_ROOT'] + '/bin/mvn.bat', groupId, artifactId, version, tmpZip.getCanonicalPath(), pathNexus, 'zip')