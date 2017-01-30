package es.eci.utils.npm

import groovy.json.JsonSlurper
import groovy.json.JsonOutput


class NpmVersionHelper {
	
	//-------------------------------------------------------------------
	// Constantes de la clase
	
	final static def REGEXP_CLOSED = /[0-9]+\.[0-9]+\.[0-9]+(\.[0-9]+)?/
	final static def REGEXP_CLOSED_OR_HOTFIXED = /[0-9]+\.[0-9]+\.[0-9]+(\.[0-9]+)?(\-[0-9]+)?/
	final static def REGEXP_OPEN =  /[0-9]+\.[0-9]+\.[0-9]+(\.[0-9]+)?\-SNAPSHOT/
	
	final static def REGEXP_FOR_NPM = /^v?[0-9]+\.[0-9]+\.[0-9]+(\-([\d\w\s]+))?/
	
	final static String version_txt_file ="/version.txt"
	
	/**
	 * Lee el numerdo de versión desde el parent Workspace
	 * @param directorio
	 * @param nombre fichero
	 * @return el objecto Json
	 */
	
	def getVersionFile(dir,filename){
		return new JsonSlurper().parse(new File (dir,filename))
	}

	/**
	 * Controla si una version es Snapshot
	 * @param actual version number
	 * @return boolen isSnapshot
	 */
	
	def isSnapshot(String projectVersion) {
		projectVersion.toUpperCase().endsWith('-SNAPSHOT')
	}

	/**
	 * Cierra el numero de versión a la Release
	 * @param actual version number
	 * @return incremented version
	 */
		
	def closeVersion(projectVersion) {
		projectVersion.split('-')[0]
		
	}

	/**
	 * incrementa el numero de versión de una Release a Snapshot
	 * @param actual version number
	 * @return incremented version
	 */
	
	def incrementVersion(projectVersion){
		if(!isSnapshot(projectVersion)){
			def sPV=projectVersion.tokenize('.')
			sPV[1] = (((sPV[1] as Integer ) + 1 ) as String)
			// Deja el tercer dígito a cero
			if (sPV.size() > 2) {
				sPV[2] = "0"
			}
			projectVersion=new StringBuilder().append(sPV.join('.')).append('-SNAPSHOT').toString()
		}  
		return projectVersion			
	}
	
	/**
	 * incrementa el numero de versión por un Fix
	 * @param actual version number
	 * @return incremented version
	 */
	
	def incrementFix(projectVersion){
		if(!isSnapshot(projectVersion)){
			def sPV=projectVersion.tokenize('.')
			sPV[2] = (((sPV[2] as Integer ) + 1 ) as String)
			projectVersion=sPV.join('.')
		}
		return projectVersion
	}
	
	/**
	 * incrementa el numero de versión por un HotFix
	 * @param actual version number
	 * @return incremented version	
	 */
	
	def incrementHotFix(projectVersion){

		if(!isSnapshot(projectVersion)){
			if(projectVersion.tokenize('-').size() == 2){
				def vArray = projectVersion.tokenize('-')
				vArray[1] = (((vArray[1] as Integer ) + 1 ) as String)
				projectVersion=vArray.join('-')
			}else{
				projectVersion=projectVersion+"-1"
			}
		}
		return projectVersion
	}

	/**
	 * Guarda el fichero 
	 * @param parent workspace
	 * @param filename 
	 * @param Json Object to write  
	 */
	
	def saveFile (dir,filename,jsonObj) {
		
		File json = new File(dir, filename);
		json.createNewFile();
		json.text = JsonOutput.prettyPrint(JsonOutput.toJson(jsonObj))
		
	}
	
	/**
	 * Indica si la cadena pasada se corresponde con una línea base abierta.
	 * @param s Cadena a examinar
	 * @return Cierto si se corresponde con una versión abierta
	 */
	
	public boolean isOpen(String s) {
		boolean ret = false;
		if (s != null && s.trim().length() > 0) {
			ret = (s ==~ REGEXP_OPEN)
		}
		return ret;
	}
	
	/**
	 * Indica si la cadena pasada se corresponde con una línea base cerrada.
	 * @param s Cadena a examinar
	 * @return Cierto si se corresponde con una versión cerrada
	 */
	
	public boolean isClosed(String s) {
		boolean ret = false;
		if (s != null && s.trim().length() > 0) {
			ret = (s ==~ REGEXP_CLOSED)
		}
		return ret;
	}
	
	/**
	 * Indica si la cadena pasada se corresponde con una línea base cerrada.
	 * @param s Cadena a examinar
	 * @return Cierto si se corresponde con una versión cerrada
	 */
	
	public boolean isClosedOtHotfixed(String s) {
		boolean ret = false;
		if (s != null && s.trim().length() > 0) {
			ret = (s ==~ REGEXP_CLOSED_OR_HOTFIXED)
		}
		return ret;
	}
	
	/**
	 * Este metodo añade el padding derecho de la version para tener 4 cifras ej, 1.0 -> 1.0.0.0
	 * @param version (1.0) (1.0.0) (1.0.0.0)
	 * @return version con padding (1.0.0.0) 
	 */
	
	public String addPaddingToVersion(String version) {
		def occurrence = ((version =~ /\./).count)
		
		if(occurrence==0)
			throw new Exception("Version : ${version} format incorrect")
		
		if(occurrence==2)
			return version
			
		def sversion = version.tokenize( '-' )
		for( i in occurrence..1)
			sversion[0]="${sversion[0]}.0"
		
		return sversion.join("-")
			
	}
	
	/**
	 * Crea un fichero version.txt en el workspace indicado.
	 * @param version Versión del entregable.
	 * @param groupId Group id del entregable.
	 * @param workspace Directorio de construcción.
	 */
	
	// create version.txt file (workarround for the Tagger)
	// version="109.0.2.0"
	// groupId="es.elcorteingles.ciisw.ccomerciales.aplicaciones"
	
	public void createVersionFile(String workspace, String version ,String groupId){
		
		File vfiletxt = new File(workspace + version_txt_file);
		if (!vfiletxt.exists()) {
			vfiletxt.createNewFile();
		}
		vfiletxt.text = ("version=\"" + version + "\"\ngroupId=\"" + groupId + "\"") 
		
	}
	
	
	/**
	 * comprueba que la version es de la forma X.Y.Z o X.Y.Z-cualquiercosa .
	 * @param s Cadena a examinar
	 * @return Cierto si se corresponde con una versión cerrada
	 */
	
	public boolean isNpmValidVersion(String s) {
		boolean ret = false;
		if (s != null && s.trim().length() > 0) {
			ret = (s ==~ REGEXP_FOR_NPM)
		}
		return ret;
	}
	
	
}
