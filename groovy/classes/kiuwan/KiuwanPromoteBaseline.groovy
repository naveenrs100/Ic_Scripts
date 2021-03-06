package kiuwan;

import rtc.RTCUtils;
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date;
import es.eci.utils.TmpDir;
import es.eci.utils.NexusHelper
import es.eci.utils.StringUtil;
import es.eci.utils.ZipHelper
import es.eci.utils.base.Loggable;
import es.eci.utils.commandline.CommandLineHelper
import es.eci.utils.pom.MavenCoordinates
import groovy.json.JsonSlurper
import kiuwan.KiuwanExecutor;

class KiuwanPromoteBaseline extends Loggable {

	String instantanea;
	String nexusUrl;
	String fichasGroupId;

	String aplicacionUrbanCode;
	String rtcUser;
	String rtcPass;
	String rtcUrl;
	String kiuwanPath;
	String slaveWorkspace;

	String kiuwanGroupsCache;

	/**
	 * $KIUWANPATH –n $nombre_aplicación –cr $release –l $etiqueta --promote-to-baseline –pbl $nuevaEtiqueta
	 */
	public void execute() {
		NexusHelper nxHelper = new NexusHelper(nexusUrl);
		MavenCoordinates coordinates = new MavenCoordinates(fichasGroupId, StringUtil.normalize(aplicacionUrbanCode), instantanea);
		coordinates.setPackaging("zip");

		String streamOrGroup;
		String product;
		String componentName;
		String version;
		String projectArea;
		def jsonObject;

		TmpDir.tmp { File tmpDir ->
			File descriptorFile = nxHelper.download(coordinates, tmpDir);
			ZipHelper zipHelper = new ZipHelper();
			zipHelper.unzipFile(descriptorFile, tmpDir);

			File rtcJsonFile = new File(tmpDir,"rtc.json");
			File gitJsonFile = new File(tmpDir,"git.json");

			String scmSource;
			if(rtcJsonFile.exists()) {
				jsonObject = (new JsonSlurper()).parseText(rtcJsonFile.text);
				scmSource = "rtc";
			} else if(gitJsonFile.exists()) {
				jsonObject = (new JsonSlurper()).parseText(gitJsonFile.text);
				scmSource = "git";
			}

			streamOrGroup = jsonObject.source;
			def failedAttempts = [];
			jsonObject.versions.each { versionObject ->
				versionObject.keySet().each { String key ->
					componentName = key;
					version = versionObject.get(key);

					KiuwanExecutor ke = new KiuwanExecutor();

					if(scmSource.equals("rtc")) {
						RTCUtils ru = new RTCUtils();
						ru.initLogger(this);
						projectArea = ru.getProjectArea(streamOrGroup, rtcUser, rtcPass, rtcUrl);
						product = StringUtil.normalizeProjectArea(projectArea);

					} else if(scmSource.equals("git")) {
						File cacheKiuwanFile = new File("${slaveWorkspace}/groups");

						def jsonCache = new JsonSlurper().parseText(cacheKiuwanFile.text);
						product = jsonCache[streamOrGroup];

					} else {
						throw new Exception("-- El parámetro calculado \"scmSource\" vale \"${scmSource}\" y es desconocido.");
					}

					log "-- Pasando a baseline la versión \"${version}\" del componente \"${componentName}\"..."

					def command = "${kiuwanPath} "
					command += "-n \"${componentName}\" "
					command += "-cr \"${product}\" "
					command += "-l \"${version}\" "
					command += "--promote-to-baseline "
					command += "-pbl \"${version}\" "

					log "Lanzando el comando kiuwan. \"${command}\""

					CommandLineHelper helper = new CommandLineHelper(command);
					helper.initLogger(this)
					try {
						int result = helper.execute(new File(slaveWorkspace));
						if (result != 0) {
							ke.reportError(result);
						}

						// Actualizamos el log de componentes promocionados
						updateKiuwanLogFile(slaveWorkspace, product, componentName, version, instantanea);

					} catch (Exception e) {

						if(componentName.toLowerCase().endsWith("-properties") || componentName.toLowerCase().endsWith("-config")
						|| componentName.toLowerCase().endsWith("- properties") || componentName.toLowerCase().endsWith("- config")
						|| componentName.toLowerCase().endsWith("- cfg") || componentName.toLowerCase().endsWith("-cfg")
						|| componentName.contains("- Config") || componentName.contains("-Config")
						|| componentName.toLowerCase().contains("- templates") || componentName.toLowerCase().contains("-templates")) {
							log("### WARNING: El componente \"${componentName}\" parece un properties, de configuración o un templates. "
									+ "No debería haber ningún análisis suyo en Kiuwan que promocionar a Baseline.")

						} else {
							log(e.getMessage());
							log("-- Ha habido fallo al intentar promocionar ${componentName}:${version} a baseline.");
							failedAttempts.add(componentName);
						}
					}
				}
			}
			if(failedAttempts.size() > 0) {
				throw new Exception("--- Los siguientes componentes no han podido promocionarse a Baseline: $failedAttempts")
			}

		}
	}


	private updateKiuwanLogFile(String slaveWorkspace, String product, String componentName, String version, String instantanea) {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Date date = new Date();

			String nowDate = dateFormat.format(date);

			String logLocation = "${slaveWorkspace}/kiuwanLog.txt";
			File logKiuwanFile = new File(logLocation);
			System.out.println("Rellenando el logFile en \"${logLocation}\".");

			/** TODO: Si funciona modificar para sacar las variables **/
			def auditResult="";
			try {
				def urlKiuwanApi = "https://api.kiuwan.com";
				def kiuwanUser = "ujenkins";
				def kiuwanPass = "MjZ5pDrH4VUuYI8Qn8JZ";
				KiuwanApiClient client = new KiuwanApiClient(urlKiuwanApi, kiuwanUser, kiuwanPass);
				
				String url = "apps/${componentName}/deliveries";
				//String encodedUrl = java.net.URLEncoder.encode(url, "UTF-8");
				String encodedUrl = url.replaceAll(" ", "%20");
				
				String response = client.get(encodedUrl);
				
				System.out.println("Response desde Kiuwan: \n" + response);

				def jsonObject = new JsonSlurper().parseText(response);

				if(jsonObject != null) {
					jsonObject.each { analysis ->
						def analysisVersion = analysis.label;
						if(analysisVersion.equals(version)) {
							auditResult = analysis.auditResult;
						}
					}
				}
			} catch (Exception e) {
				System.out.println("###### Ha habido problemas calculando el \"auditResult\":\n " + e.getMessage());
			}
		
			logKiuwanFile.append("${dateFormat.format(date)},${product},${componentName},${version},${auditResult},${instantanea}\n");
			
		} catch (Exception e) {
			System.out.println("### Ha habido algún error modificando el log de kiuwan.");
			System.out.println(e.getMessage());
		}
	}

	public String getInstantanea() {
		return instantanea;
	}

	public void setInstantanea(String instantanea) {
		this.instantanea = instantanea;
	}

	public String getNexusUrl() {
		return nexusUrl;
	}

	public void setNexusUrl(String nexusUrl) {
		this.nexusUrl = nexusUrl;
	}

	public String getFichasGroupId() {
		return fichasGroupId;
	}

	public void setFichasGroupId(String fichasGroupId) {
		this.fichasGroupId = fichasGroupId;
	}

	public String getAplicacionUrbanCode() {
		return aplicacionUrbanCode;
	}

	public void setAplicacionUrbanCode(String aplicacionUrbanCode) {
		this.aplicacionUrbanCode = aplicacionUrbanCode;
	}

	public String getRtcUser() {
		return rtcUser;
	}

	public void setRtcUser(String rtcUser) {
		this.rtcUser = rtcUser;
	}

	public String getRtcPass() {
		return rtcPass;
	}

	public void setRtcPass(String rtcPass) {
		this.rtcPass = rtcPass;
	}

	public String getRtcUrl() {
		return rtcUrl;
	}

	public void setRtcUrl(String rtcUrl) {
		this.rtcUrl = rtcUrl;
	}

	public String getKiuwanPath() {
		return kiuwanPath;
	}

	public void setKiuwanPath(String kiuwanPath) {
		this.kiuwanPath = kiuwanPath;
	}

	public String getParentWorkspace() {
		return slaveWorkspace;
	}

	public void setParentWorkspace(String parentWorkspace) {
		this.slaveWorkspace = parentWorkspace;
	}

	public String getKiuwanGroupsCache() {
		return kiuwanGroupsCache;
	}

	public void setKiuwanGroupsCache(String kiuwanGroupsCache) {
		this.kiuwanGroupsCache = kiuwanGroupsCache;
	}


}