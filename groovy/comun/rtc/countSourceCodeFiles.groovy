package rtc

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Matcher
import java.util.regex.Pattern

import es.eci.utils.ScmCommand
import es.eci.utils.Stopwatch
import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.TmpDir
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

/**
 * Cuenta los ficheros correspondientes a un patrón en una lista de áreas de
 * proyecto de RTC.  Para ello, 
 * + lista los componentes de cada área de proyecto
 * + toma la última línea base de cada componente
 * + lista los ficheros correspondientes a la misma
 * + cuenta el número de ficheros que cumplen el patrón
 * 
 * Genera la salida en un fichero por cada área de proeycto en el directorio
 * indicado por outputDir.
 * 
 * Parámetros de entrada:
 *
 * --- OBLIGATORIOS
 * scmToolsHome Directorio raíz de las herramientas RTC
 * userRTC Usuario RTC
 * pwdRTC Password RTC
 * urlRTC URL de repositorio RTC
 * projectAreasFile Fichero con un área de proyecto por cada línea
 * outputDir Directorio de salida de los resultados
 * pattern Patrón de correspondencia de los ficheros fuente
 * parentWorkspace Directorio de ejecución
 * numHilos Número de hilos worker
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

def parameters = propertyBuilder.getSystemParameters()
String scmToolsHome = parameters.get("scmToolsHome");
String userRTC = parameters.get("userRTC");
String pwdRTC = parameters.get("pwdRTC");
String urlRTC = parameters.get("urlRTC");
String projectAreasFile = parameters.get("projectAreasFile");
File outputDir = new File((String) parameters.get("outputDir"));
String patternStr = parameters.get("pattern");
String parentWorkspace = parameters.get("parentWorkspace");
Integer numHilos = Integer.valueOf(parameters.get("numHilos"))

ArrayBlockingQueue<String> logQueue = new ArrayBlockingQueue<String>(20);
ConcurrentLinkedDeque<Runnable> jobsQueue = new ConcurrentLinkedDeque<Runnable>();

List lines = new File(projectAreasFile).readLines();

Runnable logConsumer = new Runnable() {
	private int areas = 0;
	private boolean continueConsumer = true;
	
	public void setAreas(int areas) {
		this.areas = areas;
	}
	
	public synchronized endConsumer() {
		continueConsumer = false;
	}
	
	void run() {
		long millisTotal = Stopwatch.watch {
			for (int i = 1; i <= areas; i++) {
				String log = logQueue.take();
				if (log != null) {
					println "===================================="
					println "Procesada área de proyecto :: $i / $areas ..."
					println log;
				}
			}
		}
		println "-------------> Tiempo total transcurrido para contar los ficheros: $millisTotal ms."
	}
};

logConsumer.setAreas(lines.size());

def processArea(
		final String scmToolsHome,
		final String projectArea, 
		final File outputDir, 
		final Queue<String> queue,
		final Queue<Runnable> jobsQueue,
		final String userRTC, 
		final String pwdRTC, 
		final String urlRTC,
		final Map<String, String> areasMap,
		final Pattern pattern) {
	Runnable item = new Runnable() {
		public void run() {
			def parser = new JsonSlurper();
			def slurper = new JsonSlurper();
			StringBuilder sb = new StringBuilder();
			TmpDir.tmp { File baseDir ->
				TmpDir.tmp { File daemonsConfigDir ->
					ScmCommand command = new ScmCommand(
							ScmCommand.Commands.LSCM,
							scmToolsHome,
							daemonsConfigDir.getCanonicalPath());
					try {
						// Estructura de datos de vuelta
						Map<String, Integer> result =
								new HashMap<String, Integer>();
						File outputFile = new File(outputDir, "${projectArea}.output");
						outputFile.createNewFile();
						outputFile.text = "";
						long millisAP = Stopwatch.watch {
							String output = command.ejecutarComando(
									"list components -m 9999 --projectarea \"$projectArea\" -j",
									userRTC, pwdRTC, urlRTC, baseDir);
							def jsonComponents = slurper.parseText(output);
							// Función que procesa los ficheros de un componente
							def processComponent = { def componentObject ->
								String component = componentObject.name;
								String componentUuid = componentObject.uuid;
								sb.append "Obteniendo última línea base de $component...\n";
								String jsonBaseline = command.ejecutarComando(
										"list baselines -m 1 -C \"$componentUuid\" -j",
										userRTC, pwdRTC, urlRTC, baseDir);
								def data = slurper.parseText(jsonBaseline);
								String baselineUuid = data[0].baselines[0].uuid;
								String nameBaseline = data[0].baselines[0].name;
								sb.append "Obteniendo los ficheros del componente $component [$nameBaseline]...\n"
								String remoteFilesString =
										command.ejecutarComando(
										"list remotefiles --depth - -b $baselineUuid -j $componentUuid",
										userRTC, pwdRTC, urlRTC, baseDir);
								def files = parser.parseText(remoteFilesString);
								files["remote-files"].each { def file ->
									Matcher m = pattern.matcher(file.path);
									if (m.matches()) {
										Integer fileCount = result.get(projectArea);
										if (fileCount == null) {
											fileCount = Integer.valueOf(1);
										}
										else {
											fileCount = fileCount + 1;
										}
										result.put(projectArea, fileCount);
									}
								}
							}
							// Procesar los componentes que pertenecen directamente al
							//	área de proyecto
							jsonComponents.components.each { def componentObject ->
								processComponent(componentObject);
							}
							// Áreas de equipo
							areasMap[projectArea].each { String teamArea ->
								sb.append "Procesando área de equipo $teamArea...\n"
								String outputTeamArea = command.ejecutarComando(
										"list components -m 9999 --teamarea \"$teamArea\" -j",
										userRTC, pwdRTC, urlRTC, baseDir);
								def jsonComponentsTeamArea = slurper.parseText(outputTeamArea);
								jsonComponentsTeamArea.components.each { def componentObject ->
									processComponent(componentObject);
								}
							}
						}
						sb.append "AP procesada en $millisAP ms.\n"
						sb.append "---------- RESULTADO\n";
						sb.append result;
						outputFile.text = new JsonBuilder(result).toPrettyString();
					}
					catch(Exception e) {
						sb = new StringBuilder();
						sb.append("ERROR $projectArea - ${e.getMessage()}")
					}
					finally {
						queue.add(sb.toString());
						command.detenerDemonio(baseDir);
					}
				}
			}
		}
	}
	jobsQueue.add(item);
}

File baseDir = new File(parentWorkspace);
Pattern pattern = Pattern.compile(patternStr);

Map<String, List<String>> areasMap = new HashMap<String, List<String>>();
def parser = new JsonSlurper();


TmpDir.tmp { File daemonsConfigDir ->
	ScmCommand command = new ScmCommand(
			ScmCommand.Commands.LSCM,
			scmToolsHome,
			daemonsConfigDir.getCanonicalPath());
	//command.initLogger { println it }
	long millisTeamAreas = Stopwatch.watch {
		println "Leyendo las áreas de equipo..."
		// Primero: cachear las áreas de equipo de cada área de proyecto
		// Para ello hay que consultar las áreas de proyecto con -v
		String areasOutput = command.ejecutarComando(
				"list projectareas -v -j",
				userRTC, pwdRTC, urlRTC, baseDir);
		def objAreas = parser.parseText(areasOutput);
		objAreas.each { def projectArea ->
			List<String> teamAreas = [];
			projectArea["team-areas"].each { def teamArea ->
				teamAreas << teamArea.name;
			}
			areasMap.put(projectArea.name, teamAreas);
		}
	}
	println "Lectura de áreas de equipo: $millisTeamAreas ms."

	
	try {
		int i = 1;
		lines.each { String projectArea ->
			processArea(scmToolsHome, projectArea, outputDir, 
				logQueue, jobsQueue, userRTC, 
				pwdRTC, urlRTC, areasMap, pattern)
		}
	}
	finally {
		command.detenerDemonio(baseDir);
	}
	
	
	new Thread(logConsumer).start();
	
	for (int i = 1; i <= numHilos; i++) {
		// Instanciar hilo worker
		Thread worker = new Thread() {
			void run() {
				boolean keepOn = true;
				while (keepOn) {
					Runnable item = jobsQueue.poll();
					if (item == null) {
						keepOn = false;
					}					
					else {
						item.run();
					}
				}
			};
		}
		worker.start();
	}
}