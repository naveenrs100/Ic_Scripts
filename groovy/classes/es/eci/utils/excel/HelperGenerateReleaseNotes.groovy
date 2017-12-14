package es.eci.utils.excel;

import java.io.File;

import org.apache.poi.hssf.usermodel.HSSFCell
import org.apache.poi.hssf.usermodel.HSSFCellStyle
import org.apache.poi.hssf.usermodel.HSSFFont
import org.apache.poi.hssf.usermodel.HSSFSheet
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row

import es.eci.utils.LogUtils
import es.eci.utils.NexusHelper;
import es.eci.utils.ScmCommand
import es.eci.utils.TmpDir;
import es.eci.utils.ZipHelper
import es.eci.utils.base.Loggable;
import es.eci.utils.pom.MavenCoordinates;
import git.commands.GitCloneCommand
import git.commands.GitLogCommand
import groovy.json.JsonOutput
import groovy.json.JsonParser;
import groovy.json.JsonSlurper
import es.eci.utils.StringUtil;
import rtc.RTCUtils;


/**
 * Busca las dos ultimas lineas base del usuario JENKINS_RTC para mostrar 
 * los cambios de la release note.
 */
class HelperGenerateReleaseNotes extends Loggable {

	String n;
	String nexusUrl;
	File parentWorkspace;
	String scmToolsHome;
	String daemonConfigDir;
	String fichasGroupId;
	String pwdRTC;
	String userRTC;
	String urlRTC;
	String gitUser;
	String gitHost;
	String targetStream;

	public HelperGenerateReleaseNotes(String nexusUrl, String pwdRTC,
	String userRTC, String urlRTC, String n, File parentWorkspace,
	String scmToolsHome, String daemonConfigDir, String fichasGroupId, String gitHost, String gitUser) {
		super();
		this.nexusUrl = nexusUrl;
		this.pwdRTC = pwdRTC;
		this.userRTC = userRTC;
		this.urlRTC = urlRTC;
		this.n = n;
		this.parentWorkspace = parentWorkspace;
		this.scmToolsHome = scmToolsHome;
		this.daemonConfigDir = daemonConfigDir;
		this.fichasGroupId = fichasGroupId;
		this.gitHost = gitHost;
		this.gitUser = gitUser;
	}

	public HelperGenerateReleaseNotes() {
		super();
	}

	/**
	 * Genera un informe de cambios entre dos instantaneas de una aplicación UrbanCode para proyectos que están subidos a git.
	 * @return
	 */
	def public generateReleaseNotesGIT(aplicacionUrbanCode,instantanea1,instantanea2,outputFile) {
		TmpDir.tmp { File tmpDir ->
			Map comparaciones = obtenerMapaComparaciones(aplicacionUrbanCode, instantanea1, instantanea2, tmpDir, "git");
			Map<String,Object> finalChangeSet = [:];
			Map superComponentMap = [:];
			List componentsList = [];
			comparaciones.keySet().each { String componentName ->
				List<String> versiones = comparaciones.get(componentName);
				if(versiones.size() == 2) {
					String startTag = versiones[0];
					String endTag = versiones[1];
					String group = getScmSource(tmpDir,"git");
					String gitPath = "${group}/${componentName}";
					String gitBranch = getGitBranch(tmpDir) != null ? getGitBranch(tmpDir) : "RELEASE"; // Temporal hasta que venga en el git.json el branch.

					GitCloneCommand cloneCommand = new GitCloneCommand(gitUser, gitHost, gitPath, gitBranch, componentName, tmpDir, null, null, "false");
					cloneCommand.initLogger(this);
					cloneCommand.execute();

					GitLogCommand logCommand = new GitLogCommand(n, new File(tmpDir,"${componentName}").getCanonicalPath(), null, startTag, endTag);
					logCommand.initLogger(this);
					String logTxt = logCommand.execute(); // Log de cambios entre las dos tags.



					Map thisComponentMap = parseGitLogCommandOutput(logTxt, componentName);

					componentsList.add(thisComponentMap);
				}
			}

			superComponentMap.put("components", componentsList)

			println(JsonOutput.prettyPrint(JsonOutput.toJson(superComponentMap)));

			writeExcelReport(JsonOutput.toJson(superComponentMap), outputFile);
		}
	}

	/**
	 * Genera un informe de cambios entre dos instantaneas de una aplicación UrbanCode para proyectos que están subidos a RTC.
	 * @return
	 */
	def public generateReleaseNotesRTC(aplicacionUrbanCode,instantanea1,instantanea2,lineaBaseInicial,outputFile) {
		TmpDir.tmp { File tmpDir ->

			Map<String,List<String>> comparaciones = obtenerMapaComparaciones(aplicacionUrbanCode, instantanea1, instantanea2, tmpDir, "rtc");
			List componentChanges = [];

			// Recorremos los componentes y comparamos las dos baselines en caso de que las versiones difieran.
			comparaciones.keySet().each { String componentName ->				
				List<String> versiones = comparaciones.get(componentName);
				ScmCommand command = new ScmCommand(ScmCommand.Commands.SCM,scmToolsHome,daemonConfigDir);
				String fragmentoConfig = command.iniciarSesion(userRTC,pwdRTC,urlRTC,tmpDir,"aliasLogin");

				if(!versiones[0].equals(versiones[1])) {
					String stream = getScmSource(tmpDir,"rtc");
					Map<String,Object> changeSetMap = compareBaselines(versiones,stream,componentName,fragmentoConfig,tmpDir,"aliasLogin",command,this.targetStream);

					componentChanges.add(changeSetMap.get("components")[0]);

					String changeSetJson = JsonOutput.toJson(changeSetMap);
					println("changeSetJson calculado: ${changeSetMap}")
				}

			}

			Map<String,Object> finalChangeSet = [:];
			finalChangeSet.put("components", componentChanges);

			writeExcelReport(JsonOutput.toJson(finalChangeSet), outputFile);
		}
	}

	/**
	 * Lee la salida del log de comparativa de git y devuelve un 
	 * mapa de con los cambios legible por el método writeExcelReport
	 * encargado de crear el excel final.
	 * 
	 * @param logTxt
	 * @return
	 */
	private Map parseGitLogCommandOutput(String logTxt, String componentName) {
		List<String> lineas = [];
		logTxt.eachLine { String line ->
			lineas.add(line);
		}

		List<List<String>> listaBloques = [];

		for(int i=0; i < lineas.size(); i++) {
			if(lineas[i].startsWith("commit")) {
				List<String> thisBloque = [];
				for(int k=i+1; k < lineas.size(); k++) {
					if(!lineas[k].startsWith("commit")) {
						if(!lineas[k].trim().equals("")) {
							thisBloque.add(lineas[k].trim());
						}
					} else {
						break;
					}
				}
				listaBloques.add(thisBloque);
			}
		}

		Map componentMap = [:];
		componentMap.put("name", componentName);
		componentMap.put("scm", "git");

		List changesList = [];

		listaBloques.each { List<String> changeSet ->
			Map changeSetMap = [:];
			def author = "";
			def email = "";
			def date = "";
			// Cada bloque del listaBloques es realmente un changeset
			def comment = changeSet[changeSet.size() -1];
			changeSetMap.put("Comentario", comment);
			changeSet.each { String line ->
				if(line.startsWith("Author")) {
					author = line.split("Author: ")[1].split(" <")[0];
					changeSetMap.put("Autor", author);
					email = line.split("<")[1].split(">")[0];
					changeSetMap.put("Email", email);
				}
				if(line.startsWith("Date:")) {
					date = line.split("Date: ")[1];
					changeSetMap.put("Fecha", date);
				}
			}
			changesList.add(changeSetMap);
		}

		componentMap.put("changeset", changesList);

		return componentMap;
	}

	/**
	 * Compara dos lineas base o dos snapshots	 
	 * @throws Exception
	 */
	def private Map<String,Object> compareBaselines(baselinesList,stream,component,fragmentoConfig,
		baseDir,alias,ScmCommand command,targetStream) throws Exception {
		
		String lineaBaseInicial = getLineaBaseInicial(component, targetStream, command, fragmentoConfig, alias);
		
		def lastBaseline = baselinesList[0];
		def penultimateBaseline = baselinesList[1] != null ? baselinesList[1] : lineaBaseInicial;
		
		// Convertir baseline a su uuid para evitar duplicados.
		RTCUtils ru = new RTCUtils();	
		String lastUuid = ru.getBaselineUuid(component, lastBaseline, userRTC, pwdRTC, urlRTC, scmToolsHome)
		String penultimateUuid = ru.getBaselineUuid(component, penultimateBaseline, userRTC, pwdRTC, urlRTC, scmToolsHome)
		
		println "Last baseline: ${lastBaseline} (uuid: ${lastUuid})"
		println "Penultimate baseline: ${penultimateBaseline} (uuid: ${penultimateUuid})"
		Map finalChangeSet = [:];

		// Si se están comparando baselines de un componente y tiene más de una baseline válida:
		println "Ejecutando el comando: compare baseline \"${lastBaseline}\" baseline \"${penultimateBaseline}\" -c \"${component}\" -C \"|{name}|{email}|\" -D \"|yyyy-MM-dd-HH:mm:ss|\" -j"
		command.initLogger(this);
		def resultCompare = command.ejecutarComando("compare baseline \"${lastUuid}\" baseline \"${penultimateUuid}\" -c \"${component}\" -C \"|{name}|{email}|\" -D \"|yyyy-MM-dd-HH:mm:ss|\" -j",fragmentoConfig,alias,baseDir)
		println "resultCompare ---> \n${resultCompare}"

		def resultCompareObject = new JsonSlurper().parseText(resultCompare);
		def componentsList = [];

		def directions = resultCompareObject.direction;
		def componentObject;
		int directionChanges = 0;
		directions.each { thisDirection ->
			if(thisDirection.components[0].changesets != null) {
				componentObject = thisDirection.components[0];
				directionChanges++;
			}
		}

		def changesetList = []
		if(componentObject != null && componentObject.changesets != null) {
			componentObject.changesets.each { changeset ->
				if(changeset != null) {
					Map changesetMap = [:];
					def author = changeset.author;
					def name = author != null ? changeset.author.userName : "";
					def email = author != null ? changeset.author.mail : "";
					def workitem = changeset.workitems != null ? changeset.workitems[0]."workitem-number" : "";
					def workitemLabel = changeset.workitems != null ? changeset.workitems[0]."workitem-label" : "";
					def comment = changeset.comment != null ? changeset.comment : "";
					def date = changeset.creationDate != null ? changeset.creationDate : "";
					changesetMap.put("Autor", name);
					changesetMap.put("Email", email);
					changesetMap.put("WorkItem", workitem);
					changesetMap.put("WorkItem Label", workitemLabel);
					changesetMap.put("Comentario", comment);
					changesetMap.put("Fecha", date);
					changesetList.add(changesetMap);
				}
			}
			
			def componentMap = [:];
			componentMap.put("name", componentObject.name);
			componentMap.put("scm", "RTC");
			componentMap.put("changeset", changesetList);
			componentsList.add(componentMap);
	
			finalChangeSet.put("components", componentsList);
		}
		else {
			def componentMap = [:];
			componentMap.put("name", component);
			componentMap.put("scm", "RTC");
			componentMap.put("changeset", changesetList);
			componentsList.add(componentMap);
	
			finalChangeSet.put("components", componentsList);
		}

		log "***** Termina de generar release notes";

		return finalChangeSet;
	}


	/**
	 * Escribe un excel con el informe de cambios en base al json de cambios que se ha generado previamente.
	 * Formato del JSON de cambios:
	 * {
	 * 	"components" : [
	 * 		{
	 * 			"name": "Test Component 1",
	 * 			"scm": "RTC",			
	 * 			"changeset" : [
	 * 				{
	 * 					"Autor" : "dcjimenez",
	 * 					"Email" : "david.castrojimenez@gexterno.es",
	 *					"WorkItem" : "1111",
	 *					"Comentario" : "Cambio de prueba 1",
	 *					"Fecha" : "19-07-2017"
	 *				}
	 *			]
	 *		}
	 * 	  ]
	 *  }
	 * @return
	 */
	def public static writeExcelReport(String changesetJson, String changeLogXLS) {
		HSSFWorkbook workbook = new HSSFWorkbook();

		HSSFFont defaultFont= workbook.createFont();
		defaultFont.setBoldweight(HSSFFont.BOLDWEIGHT_NORMAL);
		defaultFont.setFontHeightInPoints((short)10);
		defaultFont.setFontName("Arial");
		defaultFont.setColor(IndexedColors.BLACK.getIndex());
		defaultFont.setItalic(false);

		HSSFFont font= workbook.createFont();
		font.setFontHeightInPoints((short)10);
		font.setFontName("Arial");
		font.setColor(IndexedColors.BLACK.getIndex());
		font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		font.setItalic(false);

		HSSFCellStyle boldStyle = workbook.createCellStyle();
		boldStyle.setFont(font);

		HSSFCellStyle defaultStyle = workbook.createCellStyle();
		defaultStyle.setFont(defaultFont);

		// ROWS (Se sacan del json de cambios)
		int compoCount = 0;
		def jsonObject = new JsonSlurper().parseText(changesetJson);

		jsonObject.components.each { componentObject ->
			String componentName = componentObject.name;
			String scm = componentObject.scm;
			// Se crea una página en el excel por cada componente:
			HSSFSheet sheet = workbook.createSheet(componentName);
			// HEADER
			def headers = ["Autor","Email","WorkItem", "WorkItem Label", "Comentario","Fecha"];
			Row headerRow = sheet.createRow(0);
			int c = 0;
			headers.each { String headerValue ->
				if(!scm.equals("RTC") && (headerValue.equals("WorkItem") || headerValue.equals("WorkItem Label"))) {
					// No se hace nada si la cabecera es WorkItem y el scm es distinto de RTC
				}
				else {
					HSSFCell thisHeaderCell = headerRow.createCell(c);
					thisHeaderCell.setCellValue(headerValue);
					thisHeaderCell.setCellStyle(boldStyle);
					c++;
				}
			}

			int changeCount = 1;
			componentObject.changeset.each { changesetObject ->
				//Create a new row in current sheet
				Row row = sheet.createRow(changeCount);
				//Create cells in current changeset row
				int cellCount = 0;
				headers.each { String headerValue ->
					if(!scm.equals("RTC") && (headerValue.equals("WorkItem") || headerValue.equals("WorkItem Label"))) {
						// No se hace nada si la cabecera es WorkItem y el scm es distinto de RTC
					} else {
						Cell cell = row.createCell(cellCount);
						cell.setCellValue(changesetObject."${headerValue}");
						cellCount++;
					}
				}
				changeCount++;
			}
			for(int i=0; i< headers.size(); i++) {
				sheet.autoSizeColumn(i);
			}

			compoCount++;
		}

		try {
			FileOutputStream fileOut = new FileOutputStream(changeLogXLS);
			workbook.write(fileOut);
			fileOut.flush();
			fileOut.close();
			System.out.println("Excel written successfully..");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Devuelve un Map de componentes que hay en las instantaneas indicadas con las versiones que tienen que comparar.
	 * ["componente1:{"version11","version22"}, "componente2":{"version21,"version22"},...]
	 * @param aplicacionUrbanCode
	 * @param instantanea1
	 * @param instantanea2
	 * @param parentWorkspace
	 * @param scm
	 * @return
	 */
	private Map<String, List<String>> obtenerMapaComparaciones(aplicacionUrbanCode, instantanea1, instantanea2, parentWorkspace, scm) {
		NexusHelper nxHelper = new NexusHelper(nexusUrl);
		MavenCoordinates coordinates1 = new MavenCoordinates(fichasGroupId, StringUtil.normalize(aplicacionUrbanCode), instantanea1);
		coordinates1.setPackaging("zip");
		MavenCoordinates coordinates2 = new MavenCoordinates(fichasGroupId, StringUtil.normalize(aplicacionUrbanCode), instantanea2);
		coordinates2.setPackaging("zip");

		File descriptorFile1 = nxHelper.download(coordinates1, parentWorkspace);

		// Mapa de las comparaciones a llevar a cabo. Será del tipo:
		// ["componente1":{"version11","version12"}, "componente2":{"version21","version22"},...]
		Map<String, List<String>> comparaciones = [:];
		String stream;
		
		File tmpDir1 = new File(parentWorkspace,"tmpDir1");
		ZipHelper zipHelper = new ZipHelper();
		zipHelper.unzipFile(descriptorFile1, tmpDir1);

		File rtcJsonFile = new File(tmpDir1,"${scm}.json");
		def rtcJsonObject = new JsonSlurper().parseText(rtcJsonFile.getText());

		stream = rtcJsonObject.source;
		String[] auxSplit = stream.split(" - ");
		String streamType = auxSplit[auxSplit.length - 1];
		
		this.targetStream = stream.replace(streamType,"RELEASE"); 
		
		rtcJsonObject.versions.each { Map componentMap ->
			componentMap.keySet().each { String componentName ->
				String version = componentMap.get(componentName);
				List listaVersiones = [];
				listaVersiones.add(version);
				comparaciones.put(componentName, listaVersiones);
			}
		}

		File descriptorFile2 = nxHelper.download(coordinates2, parentWorkspace);

		File tmpDir2 = new File(parentWorkspace,"tmpDir2");
		ZipHelper zipHelper2 = new ZipHelper();
		zipHelper2.unzipFile(descriptorFile2, tmpDir2);

		File rtcJsonFile2 = new File(tmpDir2,"${scm}.json");
		def rtcJsonObject2 = new JsonSlurper().parseText(rtcJsonFile2.getText());

		String normalizedSource = StringUtil.trimStreamName(rtcJsonObject2.source);
		String normalizedStream = StringUtil.trimStreamName(stream);
		
		if(!normalizedSource.equals(normalizedStream)) {
			log("\n\n\n##### WARNING: Las corrientes de las dos instantaneas no coinciden\n\n\n"); 
			//throw new Exception("Las corrientes de las dos instantaneas no coinciden") 
		}

		rtcJsonObject2.versions.each { Map componentMap ->
			componentMap.keySet().each { String componentName ->
				List<String> listaVersiones = comparaciones.get(componentName);
				String version = componentMap.get(componentName);
				listaVersiones.add(version);

				comparaciones.put(componentName,listaVersiones);
			}
		}
		println("Mapa de comparaciones: " + comparaciones);
		return comparaciones;
	}

	/**
	 * Devuelve el SCM origen de los componentes.
	 * @param parentWorkspace
	 * @param scm
	 * @return
	 */
	private String getScmSource(parentWorkspace, scm) {
		String source;
		File tmpDir1 = new File(parentWorkspace,"tmpDir1");

		File rtcJsonFile = new File(tmpDir1,"${scm}.json");
		def rtcJsonObject = new JsonSlurper().parseText(rtcJsonFile.getText());

		source = rtcJsonObject.source;
	}

	/**
	 * Devuelve el branch origen del componente.
	 * @param parentWorkspace
	 * @return
	 */
	private getGitBranch(File parentWorkspace) {
		String branch =  null;
		File tmpDir1 = new File(parentWorkspace,"tmpDir1");

		File rtcJsonFile = new File(tmpDir1,"git.json");
		def rtcJsonObject = new JsonSlurper().parseText(rtcJsonFile.getText());
		// TODO: Esperar a que en el git.json subido a Nexus venga la información del branch.

		return branch;
	}
	
	/**
	 * Devuelve la linea base inicial de un componente en la corriente de release.
	 * @param componentName
	 * @return
	 */
	private String getLineaBaseInicial(String componentName, String corriente, ScmCommand command, String fragmentoConfig, String alias) {
		String lineaBaseInicial = null;
		
		TmpDir.tmp { File baseDir ->
			String baselines = command.ejecutarComando("list baselines -w \"${corriente}\" -C \"${componentName}\" -j",fragmentoConfig,alias,baseDir);
			JsonSlurper slurper = new JsonSlurper();
			def listObject = slurper.parseText(baselines);
			
			def baselinesObject = listObject[0];
			if(baselinesObject != null) {
				def baselinesList = baselinesObject.baselines;
				if(baselinesList != null) {
					baselinesList.each {
						lineaBaseInicial = it.name;
					}
				}
			}
		}
		
		return lineaBaseInicial;
	}
	

}
