package vstudio
import java.nio.charset.Charset

import es.eci.utils.NexusHelper
import es.eci.utils.StreamGobbler
import es.eci.utils.TmpDir
import es.eci.utils.ZipHelper
import vs.CabinetConfigGenerator

def env = System.getenv()
String cabWizard = env['CABINET_WIZARD']
// C:\\Program Files\\Microsoft Visual Studio 8\\SmartDevices\\SDK\\SDKTools\\cabwiz.exe
String repoNexus = env["C_NEXUS_RELEASES_URL"]
String groupId = env['groupIdZip']
String artifactId = env['artifactIdZip']
String version = env['versionZip']
String type = 'zip'

String plataforma = env['platform']

String groupIdCab = groupId
String artifactIdCab = 'cabinet'
String typeCabinet = 'cab'

def toArray = { List<String> lista ->
	String[] ret = null
	if (lista != null && lista.size() > 0) {
		ret = new String[lista.size()]
		int index = 0
		lista.each { obj ->
			ret[index++] = (String) obj
		}
	}
	return ret
}

// Descarga del zip en el directorio temporal dir
TmpDir.tmp { File dir ->
	File zip = NexusHelper.downloadLibraries(groupId, artifactId, version, 
		dir.getCanonicalPath(), type, repoNexus)
	// Descomprime el zip en el directorio temporal platformDir
	TmpDir.tmp { File zipDir ->
		ZipHelper.unzipFile(zip, zipDir)
		def listFilesZip = zipDir.listFiles()
		listFilesZip.each { File fileZip ->
			println fileZip.getCanonicalPath()
		}
		File platformDir = new File([zipDir.getCanonicalPath(), plataforma].
			join(System.getProperty("file.separator")))
		println "Directorio de plataforma -> ${platformDir.canonicalPath}"
		// Se genera el cabinet en el directorio temporal cabinetDir
		TmpDir.tmp { File cabinetDir ->
			String inf = new CabinetConfigGenerator(platformDir).createInfFile();
			println inf
			File infFile = new File([cabinetDir.getCanonicalPath(), "cabinet.inf"].
				join(System.getProperty("file.separator")))
			infFile.createNewFile();
			// wizcab solo lee UTF-16 little endian
			Writer writer = new OutputStreamWriter(new FileOutputStream(infFile), Charset.forName("UTF-16LE"))
			writer.write(inf)
			writer.close()
			// Lanzar el comando
			List<String> command = [ 
				'cmd.exe' , 
				'/C' , 
				"\"\"${cabWizard}\" \"${cabinetDir.canonicalPath}\\cabinet.inf\" /dest \"${cabinetDir.canonicalPath}\" /err \"${cabinetDir.canonicalPath}\\CabWiz.log\"\"" 
			]
			File copy = new File("c:/temp/cabinet.inf")
			copy.createNewFile()
			copy.bytes = infFile.bytes
			println command
			Process p = Runtime.getRuntime().exec(toArray(command), toArray(null), cabinetDir);
			StreamGobbler cout = new StreamGobbler(p.getInputStream(), true)
			StreamGobbler cerr = new StreamGobbler(p.getErrorStream(), true)
			cout.start()
			cerr.start()
			int result = p.waitFor()
			println "Resultado de la llamada a cabwiz: ${result}" 
			
			File log = new File("${cabinetDir.canonicalPath}/CabWiz.log")
			if (log.exists()) {
				println log.text
			}
			else {
				println "No se encuentra el log ${log.canonicalPath}"
			}
			
			def listFiles = cabinetDir.listFiles()
			listFiles.each { File file ->
				println file.getCanonicalPath()
			}
						
			File cabinetFile = new File([cabinetDir.getCanonicalPath(), "cabinet.CAB"].
				join(System.getProperty("file.separator")))
		
			// Subir el cabinet.cab resultante a nexus
			NexusHelper.uploadToNexus(env['WINDOWS_VS_MAVEN_ROOT'] + '/bin/mvn.bat', 
				groupIdCab, artifactIdCab, version, cabinetFile.getCanonicalPath(), 
				repoNexus, typeCabinet)
		}
	}
}