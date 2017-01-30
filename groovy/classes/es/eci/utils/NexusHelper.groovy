package es.eci.utils


import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.charset.Charset

import es.eci.utils.base.Loggable
import es.eci.utils.commandline.CommandLineHelper
import es.eci.utils.pom.MavenCoordinates

/**
 * Métodos de utilidad para interactuar con nexus
 */
class NexusHelper extends Loggable {
	
	//---------------------------------------------------------------------------------
	// Propiedades de la clase
	
	// URL del servidor Nexus
	private String nexusURL;
	
	//---------------------------------------------------------------------------------
	// Métodos de la clase

	/**
	 * Descarga de un fichero de nexus a partir de una URL
	 * @param groupId Coordenadas GAV de nexus: id. de grupo
	 * @param artifactId Coordenadas GAV de nexus: id. de artefacto
	 * @param version Coordenadas GAV de nexus: versión
	 * @param pathDescargaLibrerias Directorio de descarga
	 * @param extension Extensión del fichero a descargar
	 * @param pathNexus URL del repositorio nexus
	 * @deprecated Usar el método no estático NexusHelper.download
	 */
	public static File downloadLibraries(String groupId, String artifactId, String version, String pathDescargaLibrerias, String extension, String pathNexus) {
		def fixExtension =  (extension.startsWith(".")?extension:("."+extension));
		def pathNexusFinal = "";
		pathNexusFinal = pathNexus + (pathNexus.endsWith("/")?"":"/") +
			groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + 
			version + "/" + artifactId + "-" + version + fixExtension;

		println pathNexusFinal;
		URL urlNExus = new URL(pathNexusFinal);
		//java.net.Proxy proxyApl = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress("proxycorp.geci", 8080))
		ReadableByteChannel lectorEci = 
			Channels.newChannel(urlNExus.openConnection().getInputStream());
		//ReadableByteChannel lectorEciRelease = Channels.newChannel(urlNExus.openConnection().getInputStream());
		println "pathDescargaLibrerias:"+pathDescargaLibrerias
		File destino = new File(pathDescargaLibrerias+"/"+artifactId+fixExtension)
		FileOutputStream fos = new FileOutputStream(destino);
		fos.getChannel().transferFrom(lectorEci, 0, 1 << 24);
		println "To local file system path:"+pathDescargaLibrerias+"/"+artifactId+fixExtension
		return destino
	}

	/**
	 * Sube a Nexus un entregable por medio de maven
	 * @param maven Ruta del entregable de maven
	 * @param groupId Coordenadas GAV de nexus: id. de grupo
	 * @param artifactId Coordenadas GAV de nexus: id. de artefacto
	 * @param version Coordenadas GAV de nexus: versión
	 * @param rutaFichero Ruta completa del fichero a subir
	 * @param pathNexus URL de nexus
	 * @param pathNexus URL del repositorio nexus
	 * @param tipo Tipo de fichero (jar/zip/tar/etc.)
	 */
	
	public static int uploadToNexus(String maven, String groupId, String artifactId, String version, String rutaFichero, String pathNexus, String tipo, Closure log = null) {
		String repo = pathNexus.split('/')[pathNexus.split('/').length - 1]
		String local_folder = rutaFichero.substring(0,rutaFichero.lastIndexOf(System.getProperty("file.separator")) )
		
		def comando = [
			maven,
			"deploy:deploy-file",
			"-DgroupId=" + groupId,
			"-DartifactId=" + artifactId,
			"-Dversion=" + version,
			"-U",
			"-Dpackaging=${tipo}",
			"-Dfile=" + rutaFichero,
			"-Durl=${pathNexus}",
			"-DrepositoryId=${repo}"
		]
//		TmpDir.tmp { tmpDir ->
//			def deploy = comando.execute(null, tmpDir);
//			StreamGobbler cout = new StreamGobbler(deploy.getInputStream(), true)
//			StreamGobbler cerr = new StreamGobbler(deploy.getErrorStream(), true)
//			cout.start()
//			cerr.start()
//
//			deploy.waitFor()
//
//			if (log != null) {
//				log comando.join(" ")
//				log cout.getOut()
//				log cerr.getOut()
//			}
//			else {
//				println cout.getOut()
//				println cerr.getOut()
//			}
//		}
		def exec_command = comando.join(" ")
		
		println "Comando :${exec_command}"
		
		CommandLineHelper buildCommandLineHelper = new CommandLineHelper(exec_command);
		buildCommandLineHelper.initLogger(log == null ? { println it} : log);
		
		int returnCode = buildCommandLineHelper.execute(new File(local_folder));
				
		if( returnCode != 0) {
			println ("Error al ejecutar comando ${exec_command} . Código -> ${returnCode}");
		}
		return returnCode
	}

	/**
	 * Sube a Nexus un comprimido por medio de maven
	 * @param uDeployUser Coordenadas GAV de nexus: id. de grupo
	 * @param uDeployPass Coordenadas GAV de nexus: id. de grupo
	 * @param m2home ruta inslación Maven
	 * @param groupId Coordenadas GAV de nexus: id. de grupo
	 * @param artifactId Coordenadas GAV de nexus: id. de artefacto
	 * @param repositoryId
	 * @param version Coordenadas GAV de nexus: versión
	 * @param rutaFichero Ruta completa del fichero a subir
	 * @param pathNexus URL de nexus
	 * @param tipo Tipo de fichero (jar/zip/tar/etc.)
	 * @param isRelease indica se es release o no
	 */
	public static void uploadTarNexusMaven(String uDeployUser, String uDeployPass, String m2home, String groupId, String artifactId, String repositoryId, String version, String rutaFichero, String pathNexus, String tipo, String isRelease, Closure log = null) {

		if (isRelease == "false" && !version.contains("-SNAPSHOT")){
			println "******* Se anade el snapshot a la version ******"
			version += "-SNAPSHOT"
		}

		m2home=m2home+"/bin"
		def cadena = "mvn deploy:deploy-file -Durl=" + pathNexus + " -DrepositoryId=eci-c-snapshots -Dfile=" + rutaFichero + " -DgroupId=" + groupId + " -DartifactId=" + artifactId + " -Dversion=" + version + " -Dtype=" + tipo + " -DuniqueVersion=false -DDEPLOYMENT_USER=" + uDeployUser + " -DDEPLOYMENT_PWD=" + uDeployPass

		def p = null
		if (System.properties['os.name'].toLowerCase().contains('windows')) {
			p = ['cmd.exe' , '/C' , cadena].execute(null, new File(m2home))
		}else {
			p = ['sh' , '-c' , cadena].execute(null, new File(m2home))
		}

		log cadena

		StreamGobbler cout = new StreamGobbler(p.getInputStream(), true)
		StreamGobbler cerr = new StreamGobbler(p.getErrorStream(),true)
		cout.start()
		cerr.start()
		p.waitFor()

		log "Salida: " + cout.getOut()
		log "Salida error: " + cerr.getOut()
	}

	/**
	 * Sube un comprimido a Nexus con binarios.
	 * @param uDeployUser Coordenadas GAV de nexus: id. de grupo
	 * @param uDeployPass Coordenadas GAV de nexus: id. de grupo
	 * @param gradleBin Binario de gradle
	 * @param cScriptsHome Home del script store de C
	 * @param nexusPublicC Coordenadas GAV de nexus: id. de grupo
	 * @param groupId Coordenadas GAV de nexus: id. de grupo
	 * @param artifactId Coordenadas GAV de nexus: id. de artefacto
	 * @param version Coordenadas GAV de nexus: versión
	 * @param pathNexus URL del repositorio nexus
	 * @param isRelease Cadena 'true' o 'false'
	 * @param artifactPath Path del artefacto a subir
	 * @param artifactType tar/zip/jar...
	 */
	public static int uploadTarNexus(uDeployUser, uDeployPass, gradleBin, cScriptsHome, nexusPublicC, String groupId, String artifactId, String version, String pathNexus, String isRelease, String artifactPath, String artifactType, Closure log = null){
		if (isRelease == "false" && !version.contains("-SNAPSHOT")){
			println "******* Se anade el snapshot a la version ******"
			version += "-SNAPSHOT"
		}
		def nexusGradleUploadC="${cScriptsHome}C_workFlowNexus/uploadProjectsC.gradle"

		groupId = groupId.replaceAll(/\(/,"")
		groupId = groupId.replaceAll(/\)/,"")



		if (log != null) {
			log "gradleBin:${gradleBin}\n"+
					"nexusGradleUploadC:${nexusGradleUploadC}\n"+
					"artifactPath:${artifactPath}\n"+
					"artifactId:${artifactId}\n"+
					"artifactType:${artifactType}\n"+
					"groupId:${groupId}\n"+
					"version:${version}\n"+
					"nexusPublicC:${nexusPublicC}\n"+
					"nexusUploadRepoC:${pathNexus}\n"
		}

		def uploadCNexus = []
		uploadCNexus.add("${gradleBin}")
		uploadCNexus.add("-i")
		uploadCNexus.add("-b${nexusGradleUploadC}")
		uploadCNexus.add("uploadCartifact")
		uploadCNexus.add("-PartifactPath=${artifactPath}")
		uploadCNexus.add("-PartifactId=${artifactId}")
		uploadCNexus.add("-PartifactType=${artifactType}")
		uploadCNexus.add("-PgroupId=${groupId}")
		uploadCNexus.add("-Pversion=${version}")
		uploadCNexus.add("-PnexusPlubicC=${nexusPublicC}")
		uploadCNexus.add("-PnexusUploadC=${pathNexus}")
		uploadCNexus.add("-Puser=${uDeployUser}")
		uploadCNexus.add("-Ppassword=${uDeployPass}")
		//uploadCNexus.add(uDeployUser)
		//uploadCNexus.add(uDeployPass)
		if (log != null) {
			log uploadCNexus.join(" ")
		}
		def procNexus = uploadCNexus.execute()
		procNexus.waitFor()
		if (procNexus.exitValue() == 0){
			if (log != null) {
				log "Upload Nexus C: CORRECTO"
				log procNexus.in.text
			}
		}else{
			if (log != null) {
				log "Upload Nexus C: ERROR"
				log procNexus.in.text
				log procNexus.err.text
			}
		}
		return procNexus.exitValue()
	}
	
	/** 
	 * Construye una instancia del helper con la URL de nexus apropiada
	 * @param nexusURL Dirección de Nexus
	 */
	public NexusHelper(String nexusURL) {
		this.nexusURL = nexusURL;
	}
	
	/**
	 * Resuelve el timestamp exacto de un artefacto Snapshot contra Nexus
	 * @param coordinates GAV + packaging del artefacto
	 * @param repo Nombre del repositorio de Nexus
	 * @return Si la versión acaba en -SNAPSHOT, devuelve el timestamp del último
	 * snapshot de ese grupo y artefacto.  En caso contrario, devuelve la versión.
	 */
	public String resolveSnapshot(MavenCoordinates coordinates, String repository = "public") {
		def isNull = { String it ->
			return it == null || it.trim().length() == 0;
		}
		def groupId = coordinates.getGroupId();
		def artifactId = coordinates.getArtifactId();
		def version = coordinates.getVersion();
		def packaging = coordinates.getPackaging();
		if (isNull(groupId) 
				|| isNull(artifactId) 
				|| isNull(version) 
				|| isNull(packaging) 
				|| isNull(repository)) {
			throw new NullPointerException(
				"Repo -> $repository ;; GAV -> $groupId :: $artifactId :: $version ;; Packaging -> $packaging");
		}
		String ret = version;
		if (version.endsWith("-SNAPSHOT")) {
			nexusURL += 
				"/service/local/artifact/maven/resolve?r=${repository}&g=${groupId}&a=${artifactId}&v=${version}&p=${packaging}";
			String classifier = coordinates.getClassifier(); 
			if (classifier != null && classifier.trim().length() > 0) {
				nexusURL += "&c=${classifier}"
			}
			URL resolverService = new URL(nexusURL);
			log "Resolviendo el timestamp contra $resolverService";
			ReadableByteChannel lectorEci =
				Channels.newChannel(resolverService.openConnection().getInputStream());
			ByteBuffer bb = ByteBuffer.allocate(1024);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			boolean keepOn = true;
			while (keepOn) {
				int readBytes = lectorEci.read(bb);
				if (readBytes == -1) {
					keepOn = false;
				}
				else {
					bb.rewind();
					byte[] tmp = new byte[readBytes];
					bb.get(tmp);
					baos.write(tmp);
				}
			}
			// Traducir baos a una cadena
			String content = new String(baos.toByteArray(), Charset.forName("UTF-8"));
			log content;
			// El contenido se parsea a XML
			def result = new XmlSlurper().parseText(content);
			ret = result.data[0].version[0];
			log "Resultado: $ret"
		}
		return ret;
	}
	
	/**
	 * Descarga de un fichero de nexus a partir de una URL
	 * @param coordinates Coordenadas Maven del fichero
	 * @param downloadPath Directorio de descarga
	 * @param pathNexus URL del repositorio nexus
	 */
	public File download(MavenCoordinates coordinates, File downloadPath) {
		String extension = "jar"; 
		// ¿Se indica un empaquetado?
		if (coordinates.getPackaging() != null && coordinates.getPackaging().trim().length() > 0) {
			extension = coordinates.getPackaging();
		}
		String classifier = "";
		// ¿Se indica un clasificador?
		if (coordinates.getClassifier() != null && coordinates.getClassifier().trim().length() > 0) {
			classifier = coordinates.getClassifier();
		}
		String fixExtension = (extension.startsWith(".")?extension:("." + extension));
		String pathNexusFinal = this.nexusURL + (this.nexusURL.endsWith("/")?"":"/") +
			coordinates.getGroupId().replaceAll("\\.", "/") + "/" + coordinates.getArtifactId() + "/" +
			coordinates.getVersion() + "/" + coordinates.getArtifactId() + "-" + coordinates.getVersion() + 
			classifier + fixExtension;

		log "Descargando de Nexus: $pathNexusFinal"
		URL urlNexus = new URL(pathNexusFinal);
		ReadableByteChannel reader =
			Channels.newChannel(urlNexus.openConnection().getInputStream());
		File target = new File(downloadPath, coordinates.getArtifactId() + fixExtension)
		FileOutputStream fos = new FileOutputStream(target);
		fos.getChannel().transferFrom(reader, 0, 1 << 24);
		log "To local file system path: " +
			downloadPath.getCanonicalPath() + "/" + 
			coordinates.getArtifactId() + fixExtension
		return target
	}
}