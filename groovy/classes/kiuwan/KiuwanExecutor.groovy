package kiuwan

import es.eci.utils.StringUtil
import es.eci.utils.base.Loggable
import es.eci.utils.commandline.CommandLineHelper
import ppm.PPMProductParser
import ppm.PPMProductsCollection

/**
 * Esta clase sirve la funcionalidad para lanzar un análisis contra Kiuwan.
 * 
 * <br/>
 * Notar que es <b>imprescindible</b> contar con una instalación local del cliente.  Se puede provisionar
 * desde:<br/>
 * <a href="http://nexus.elcorteingles.int/service/local/repositories/eci-docker-software/content/es/kiuwan/kiuwan/1.0.0/kiuwan-1.0.0.zip">Cliente kiuwan en Nexus</a>
 */
class KiuwanExecutor extends Loggable {
	
	//----------------------------------------------------------------------
	// Propiedades de la clase

	// Directorio base del análisis
	private File parentWorkspace;
	
	// Nombre del componente/repositorio a analizar
	private String component;
	// Versión construida del componente/repositorio
	private String builtVersion;
	
	// Portfolios
	
	// Nombre del producto a informar en Kiuwan
	private String product;
	// Nombre del subsistema a informar en Kiuwan
	private String subsystem;
	
	// Proveedor del software 
	private String provider;
	
	// Acción de IC (build/deploy/release/addFix/addHotfix)
	private String action;
	// Identificador de la petición de cambio (issue de Jira, p. ej.)
	private String changeRequest;
	// Ruta completa del cliente de Kiuwan en disco duro
	private String kiuwanPath;
	// Lista, en formato ant, de la lista de directorios a excluir del análisis.  
	// P. ej. :  
	// **/bower_components/**,**/datatables.js
	private String kiuwanExclusions;
	// Si se dispone de un fichero de PPM para filtrar los nombres de 
	//	proyecto, se informa aquí su ruta completa
	private String ppmFile;
	
	//----------------------------------------------------------------------
	// Métodos de la clase

	/**
	 * Setter de componente
	 * @param component Nombre del componente/repositorio a analizar
	 */
	public void setComponent(String component) {
		this.component = component;
	}
	/**
	 * Setter de versión
	 * @param builtVersion Versión construida del componente/repositorio
	 */
	public void setBuiltVersion(String builtVersion) {
		this.builtVersion = builtVersion;
	}
	/**
	 * Setter de producto
	 * @param product Nombre del producto a informar en Kiuwan
	 */
	public void setProduct(String product) {
		this.product = product;
	}
	/**
	 * Setter de subsistema
	 * @param subsystem Nombre del subsistema a informar en Kiuwan
	 */
	public void setSubsystem(String subsystem) {
		this.subsystem = subsystem;
	}
	/**
	 * Setter de proveedor
	 * @param provider Proveedor del software 
	 */
	public void setProvider(String provider) {
		this.provider = provider;
	}
	/**
	 * Setter de acción
	 * @param action Acción de IC (build/deploy/release/addFix/addHotfix)
	 */
	public void setAction(String action) {
		this.action = action;
	}
	/**
	 * Setter de changeRequest
	 * @param changeRequest Identificador de la petición de cambio (issue de Jira, p. ej.)
	 */
	public void setChangeRequest(String changeRequest) {
		this.changeRequest = changeRequest;
	}
	/**
	 * Setter de ruta del cliente Kiuwan
	 * @param kiuwanPath Ruta completa del cliente de Kiuwan en disco duro
	 */
	public void setKiuwanPath(String kiuwanPath) {
		this.kiuwanPath = kiuwanPath;
	}
	/**
	 * Setter de exclusiones
	 * @param kiuwanExclusions Lista, en formato ant, de la lista de directorios a excluir del análisis.
	 */
	public void setKiuwanExclusions(String kiuwanExclusions) {
		this.kiuwanExclusions = kiuwanExclusions;
	}
	/**
	 * Setter de fichero PPM
	 * @param ppmFile Si se dispone de un fichero de PPM para filtrar los nombres de 
		proyecto, se informa aquí su ruta completa
	 */
	public void setPpmFile(String ppmFile) {
		this.ppmFile = ppmFile;
	}
	
	/**
	 * Setter de directorio base
	 * @param parentWorkspace Directorio base del análisis
	 */
	public void setParentWorkspace(String parentWorkspace) {
		this.parentWorkspace = new File(parentWorkspace);
	}
	
	/**
	 * Este método debe identificar los códigos de retorno
	 * correspondientes a proyecto no existente en Kiuwan.
	 * 
	 * Los que corresponden, según la documentación, son:
	 * 
	 * · 20 - declarado obsoleto desde septiembre de 2017
	 * · 24 - Novedad desde septiembre de 2017
	 * · 30 - Novedad desde marzo de 2018
	 * 
	 * @param returnCode Código de retorno de Kiuwan
	 * @return Cierto si el proyecto no existe
	 */
	private boolean projectNonExistant(int returnCode) {
		return [20, 24, 30].contains(returnCode);
	}
	
	/**
	 * Este método lanza una excepción para un determinado código de error, 
	 * remitiendo a la página de códigos de error oficial de Kiuwan.
	 * @param returnCode Código de error
	 */
	public void reportError(int returnCode) {
		def errorString = "Error de ejecución Kiuwan: $returnCode \n"
		errorString += "Consultar los códigos de error en la documentación oficial: "
		errorString += "https://www.kiuwan.com/docs/display/K5/Local+Analyzer+Return+Codes"
		throw new Exception(errorString);
	}	
	
	/**
	 * Lanza el análisis del código contra Kiuwan.
	 */
	public void execute() {
		def baselineCommand = {
			def command = kiuwanPath;
			command += " -s \"${parentWorkspace.canonicalPath}\" "
			command += "-c -n \"$component\" "
			command += "-l \"$builtVersion\" "
			command += ".kiuwan.application.portfolio.Plataforma=Distribuido "
			command += "\".kiuwan.application.portfolio.Producto=${product}\" "
			command += "\".kiuwan.application.portfolio.Subsistema=${product}_${subsystem}\" "
			command += "\".kiuwan.application.portfolio.SquareQA_ECI=ECI\" "
			command += "\".kiuwan.application.provider=${provider}\" "
			command += "\".kiuwan.application.businessValue=MEDIUM\" "
			command += "\"exclude.patterns=${kiuwanExclusions}\" "
			command += "-Dencoding=UTF-8 "
			return command;
		}
		
		boolean makeAnalysis = false;
		// Verificar el producto contra el fichero de ppm
		// El fichero se encuentra en donde indique el parámetro ppmFile
		log "Fichero de validación: $ppmFile"
		if (StringUtil.notNull(ppmFile) && StringUtil.notNull(product)
				&& !"null".equalsIgnoreCase(product)) {			
			File ppm = new File(parentWorkspace, ppmFile)
			log "File <- $ppm"
			if (ppm.exists()) {
				log "Validando $product en el fichero $ppm ..."
				// Parsear el fichero
				PPMProductsCollection products = new PPMProductParser().parse(ppm);
				if (products.findByName(product) == null) {
					// Si el fichero ppm no contempla el producto, se lanza un error y no se analiza
					throw new Exception ("El producto $product no puede validarse contra el fichero $ppm");
				}
				else {
					makeAnalysis = true;
					log "El producto $product se ha encontrado en el fichero $ppm"
				} 
			}
			else {
				log "El fichero $ppm no existe"
			}
		}
		
		if (makeAnalysis) {
			log "Enviando análisis..."
			if ("baseline".equals(action)) {	
				// Creación de línea base
				CommandLineHelper helper = new CommandLineHelper(baselineCommand());
				helper.initLogger { log it }
				int result = helper.execute(parentWorkspace);
				if (result != 0) {
					reportError(result);
				}
			}
			else if ("deploy".equals(action)) {
				// Nocturno
				def command = kiuwanPath;
				command += " -s \"${parentWorkspace.canonicalPath}\" "
				command += "-n \"$component\" "
				command += "-l \"$builtVersion\" "
				command += "-cr \"${changeRequest}\" -crs inprogress " 
				command += ".kiuwan.application.portfolio.Plataforma=Distribuido "
				command += "\".kiuwan.application.portfolio.Producto=${product}\" "
				command += "\".kiuwan.application.portfolio.Subsistema=${product}_${subsystem}\" "
				command += "\".kiuwan.application.portfolio.SquareQA_ECI=ECI\" "
				command += "\".kiuwan.application.provider=${provider}\" "
				command += "\"exclude.patterns=${kiuwanExclusions}\" "
				command += "-Dencoding=UTF-8 "
				CommandLineHelper helper = new CommandLineHelper(command);
				helper.initLogger { log it }
				int result = helper.execute(parentWorkspace);
				if (result != 0 && projectNonExistant(result)) {
					CommandLineHelper basicHelper = new CommandLineHelper(baselineCommand());
					basicHelper.initLogger { log it }
					result = basicHelper.execute(parentWorkspace)
					if (result != 0) {
						reportError(result);
					}
				}
				else if (result != 0) {
					reportError(result);
				}
			}
			else if (["release", "addFix", "addHotfix"].contains(action)) {
				// Entrega 'oficial'
				def command = kiuwanPath;
				command += " -s \"${parentWorkspace.canonicalPath}\" "
				command += "-n \"$component\" "
				command += "-l \"$builtVersion\" "
				command += "-cr \"${changeRequest}\" -crs resolved " 
				command += ".kiuwan.application.portfolio.Plataforma=Distribuido "
				command += "\".kiuwan.application.portfolio.Producto=${product}\" "
				command += "\".kiuwan.application.portfolio.Subsistema=${product}_${subsystem}\" "
				command += "\".kiuwan.application.provider=${provider}\" "
				command += "\"exclude.patterns=${kiuwanExclusions}\" "
				command += "-Dencoding=UTF-8 "
				CommandLineHelper helper = new CommandLineHelper(command);
				helper.initLogger { log it }
				int result = helper.execute(parentWorkspace);
				if (result != 0 && projectNonExistant(result)) {
					CommandLineHelper basicHelper = new CommandLineHelper(baselineCommand());
					basicHelper.initLogger { log it }
					result = basicHelper.execute(parentWorkspace)
					if (result != 0) {
						reportError(result);
					}
				}
				else if (result != 0) {
					reportError(result);
				}
			}
		}
		else {
			log "NO se realiza el análisis"
		}
	}
	
}
