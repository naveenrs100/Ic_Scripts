package rtc

import java.nio.channels.FileChannel
import java.nio.channels.FileLock

import es.eci.utils.ScmCommand
import es.eci.utils.TmpDir
import es.eci.utils.Utiles
import es.eci.utils.base.Loggable
import groovy.json.JsonSlurper
import es.eci.utils.Retries;

/**
 * Esta clase agrupa funciones de utilidad para los scripts de pasos RTC.
 */
class RTCUtils extends Loggable {
	
	//-----------------------------------------------------------------------
	// Constantes de la clase
	
	// Códigos de retorno
	private static final List<Integer> CODIGOS_RETORNO = [0, 25, 33, 52, 53];
	
	//-----------------------------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Este método lanza una excepción si se ha producido un código de salida
	 * que no sea de los reconocidos por la aplicación.
	 * @param ret Código de retorno de línea de comandos
	 * @param comment Comentario a añadir a la salida
	 */
	public static void exitOnError (int ret, String comment) {
		exitOnError(ret, comment, []);
	}
	
	/**
	 * Este método lanza una excepción si se ha producido un código de salida
	 * que no sea de los reconocidos por la aplicación.
	 * @param ret Código de retorno de línea de comandos
	 * @param comment Comentario a añadir a la salida
	 * @param exceptions Lista de excepciones adicionales al comando que no sean
	 * aplicables generalmente
	 */
	public static void exitOnError (int ret, String comment, List<String> exceptions) {
		def listaCodigos = CODIGOS_RETORNO;
		listaCodigos.addAll(exceptions);
		if (!listaCodigos.contains(ret)) {
			throw new Exception("$comment - $ret")
		}	
	}
	
	// Utilidad para decidir si existe o bien si está vacía una posición de un array
	public static boolean empty(String[] array, int index) {
		return index >= array.length || array[index] == null || array[index].trim().length() == 0;
	}
	
	/**
	 * Este método valida que todas las cadenas de una lista de cadenas
	 * vengan informadas
	 * @param args Lista de argumentos
	 * @param limit Opcional.  Si viene informado, valida solo los limite primeros.  
	 * 	Ha de ser mayor que cero.
	 */
	public static void validate(String[] args, Integer limit = null) {
		Utiles.validate(args, limit);
	}
	
	/**
	 * Intenta leer un parámetro opcional de los argumentos
	 * @param args Lista de argumentos
	 * @param index Índice del parámetro opcional
	 * @return Nulo si el parámetro no existe, el valor del parámetro en otro
	 * caso
	 */
	public static String readOptionalParameter(String[] args, int index) {
		return Utiles.readOptionalParameter(args, index);
	}
	
	/**
	 * Lee como boolean una cadena pasada como parámetro
	 * @param str Cadena a leer
	 * @return Traducción de la cadena a true/false
	 */
	public static boolean toBoolean(String str) {
		return Utiles.toBoolean(str);
	}
	
	/**
	 * Intenta implementar un mutex sobre bloqueos de fichero.  Operación bloqueante
	 * que retiene al cliente hasta que consigue el lock.
	 * @param workspaceRTC Nombre del workspace RTC (identifica el fichero a bloquear)
	 * @return Bloqueo sobre el fichero
	 */
	public static FileLock getLock(String workspaceRTC) {
		File f = new File(new File(System.getProperty("java.io.tmpdir")),
			// En algunos casos, un nombre de workspaceRTC con un '/' puede romper la 
			//	creación del file lock 
			workspaceRTC.replaceAll("\\"+System.getProperty("file.separator"), "_") 
				+ ".lock")
		FileChannel fc = new FileOutputStream(f).getChannel();
		return fc.lock();
	}
	
	/**
	 * Indica si existe determinado fichero en el directorio.  Sin recursividad.
	 * @param parentWorkspace Directorio de búsqueda
	 * @param name Nombre del fichero.
	 * @return El fichero si existe, null en caso contrario
	 */
	public static File findFile(File parentWorkspace, String name) {
		File ret = null;
		File f = new File(parentWorkspace.getCanonicalPath() + System.getProperty("file.separator") + name);
		if (f.exists() && f.isFile()) {
			ret = f;
		}
		return ret;
	}
	
	/**
	 * Indica si un parámetro está informado
	 * @param param Parámetro a un script
	 * @return Cierto si está informado, falso si no
	 */
	public static boolean isSet(String param) {
		return (param != null && param.trim().length() > 0)
	}
	
	/**
	 * Vuelca un texto a un fichero en el workspace
	 * @param parentWorkspace Directorio de destino
	 * @param fileName Nombre del fichero a crear
	 * @param dump Salida a volcar
	 * @return Referencia al fichero en el que se ha volcado
	 */
	public static File dumpExit(File parentWorkspace, String fileName, String dump) {
		File f = new File(parentWorkspace.getCanonicalPath() + System.getProperty("file.separator") + fileName);
		if (!f.exists()) {
			f.createNewFile();
		}
		f.text = dump;
		return f;
	}
	
	/**
	 * Este método valida que se hayan informado todos los parámetros indicados
	 * @param params Lista de valores a comprobar
	 */
	public static void validateMandatory(Object... params) {
		boolean right = true;
		params.each { Object param ->
			if (param instanceof String) {
				right = right && isSet((String) param);
				if(!isSet((String) param)) {
					println("[ERROR]-> Falta el parámetro obligatorio \"${param}\"");
				}
			}
			else {
				right = right && (param != null);
				if(param == null) {
					println("[ERROR]-> El parámetro \"${param}\" llega a null.");
				}
			}
		}
		if (!right) {
			throw new Exception("Some mandatory params are empty!!");
		}
	}
	
	/**
	 * Obtiene el área de proyecto en RTC a la que pertenece una stream
	 * @param stream Corriente cuyo área de proyecto queremos conocer
	 * @param rtcUser Usuario RTC
	 * @param rtcPass Password RTC
	 * @param rtcUrl URL del servidor RTC
	 * @return Nombre del área de proyecto
	 */
	public String getProjectArea(
			String stream, 
			String rtcUser, 
			String rtcPass, 
			String rtcUrl) {
		def attributesObject = getProjectAreaImpl(stream, rtcUser, rtcPass, rtcUrl);			
		return attributesObject.workspaces[0].properties.visibility.info.name;
	}
	
	/**
	 * Obtiene el uuid de área de proyecto en RTC a la que pertenece una stream
	 * @param stream Corriente cuyo área de proyecto queremos conocer
	 * @param rtcUser Usuario RTC
	 * @param rtcPass Password RTC
	 * @param rtcUrl URL del servidor RTC
	 * @return UUID del área de proyecto correspondiente
	 */
	public String getProjectAreaUUID(
			String stream, 
			String rtcUser, 
			String rtcPass, 
			String rtcUrl) {
		def attributesObject;		
		def ret;	
		Retries.retry(5,5000,{
			attributesObject = getProjectAreaImpl(stream, rtcUser, rtcPass, rtcUrl);
			ret = attributesObject.workspaces[0].properties.visibility.info.uuid;
		});
		return ret;
	}

	// Llamada a get attributes de RTC
	private getProjectAreaImpl(
			String stream, 
			String rtcUser, 
			String rtcPass, 
			String rtcUrl) {
		def attributesObject = null;
		TmpDir.tmp { dir ->
			def getAttributesCommand = "get attributes --visibility --workspace \"${stream}\" -j";
			def scm = new ScmCommand(ScmCommand.Commands.SCM);
			scm.initLogger(this);
			def attributesJson = scm.ejecutarComando(
					getAttributesCommand, rtcUser, rtcPass, rtcUrl, dir);
	
			attributesObject = new JsonSlurper().parseText(attributesJson)
		}
		return attributesObject
	}
	
	/**
	 * Este método devuelve una tabla de las áreas de proyecto de una instancia
	 * 	de RTC.		
	 * @param rtcUser Usuario RTC
	 * @param rtcPass Password RTC
	 * @param rtcUrl URL del servidor RTC
	 * @return Tabla de áreas de proyecto indexada por nombre de área de proyecto.
	 */
	public Map<String, RTCProjectArea> getProjectAreas( 
			String rtcUser, 
			String rtcPass, 
			String rtcUrl) {
		Map<String, RTCProjectArea> ret = [:];
		TmpDir.tmp { File dir ->
			def listProjectAreas = 'list projectareas -j -v'
			def scm = new ScmCommand(ScmCommand.Commands.SCM);
			scm.initLogger(this);
			def paJson = scm.ejecutarComando(
					listProjectAreas, rtcUser, rtcPass, rtcUrl, dir);
			def paObject = new JsonSlurper().parseText(paJson);
			paObject.each { def objArea ->
				RTCProjectArea area = new RTCProjectArea(objArea.name, objArea.uuid);
				ret[objArea.name] = area;
			}
		}
		return ret;
	}
	
	/**
	 * Obtiene el uuid del componente en RTC
	 * @param component Nombre del componente
	 * @param rtcUser Usuario RTC
	 * @param rtcPass Password RTC
	 * @param rtcUrl URL del servidor RTC
	 * @return UUID del componente
	 */
	public String getComponentUUID(component, rtcUser, rtcPass, rtcUrl) {
		def uuid = null;
		TmpDir.tmp { File dir ->
			def getAttributesCommand = "get attributes --visibility --component \"${component}\" -j";
			def scm = new ScmCommand(ScmCommand.Commands.SCM);
			scm.initLogger(this);
			def attributesJson = scm.ejecutarComando(
				getAttributesCommand, rtcUser, rtcPass, rtcUrl, dir);
			
			
			def attributesObject = new JsonSlurper().parseText(attributesJson);
			
			uuid = attributesObject.components[0].properties.visibility.info.uuid;
		}
		return uuid;
	}
}