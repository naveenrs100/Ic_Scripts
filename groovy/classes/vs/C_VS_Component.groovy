package vs

import java.io.File;
import java.util.List;

/**
 * Esta clase parsea el xml descriptor de cada componente, construyendo una
 * lista ordenada de bibliotecas y una lista ordenada de aplicaciones.  Así
 * mismo, construye una lista ordenada de ficheros de configuración a tener 
 * en cuenta para el empaquetado. 
 */
class C_VS_Component {
	

	
	/**
	 * Implementación del método to String
	 */
	public String toString() {
		def linea = { sb, str -> 
			sb.append(str)
			sb.append(System.getProperty("line.separator"))
		}
		def presentarPlataformas = { sb, platforms ->
			if (platforms?.size() > 0) {
				linea(sb, "Plataformas:")
				platforms?.each { platform ->
					def fragmentoSubPlatId = ""
					if (platform.getPlatSubId()?.trim().length() > 0) {
						fragmentoSubPlatId = "-${platform.getPlatSubId()}"
					}
					linea(sb, "\t${platform.platId}${fragmentoSubPlatId} [${platform.rutatar}]")
				}
			}
		}
		def presentarDlls = { sb, dlls ->
			if (dlls?.size() > 0) {
				linea(sb, "Dlls:")
				dlls?.each { dll ->
					linea(sb, "\t${dll.dll}")
				}
			}
		}
		StringBuilder sb = new StringBuilder()
		linea(sb, "COMPONENTE $nombre")
		if (bibliotecas?.size() > 0) {
			linea(sb, "--> Bibliotecas auxiliares")
			bibliotecas.each { libAux ->
				linea(sb, libAux)				
			}
		}
		if (cfgs?.size() > 0) {
			linea(sb, "--> Ficheros de configuración")
			cfgs.each { cfg ->
				linea(sb, cfg.ruta)
				presentarPlataformas(sb, cfg.platforms)
			}
		}
		if (entregables?.size() > 0) {
			linea(sb, "--> Entregables")
			entregables.each { entregable ->
				linea(sb, "tipo: $entregable.type")
				linea(sb, "groupId: $entregable.groupId")
				linea(sb, "id: $entregable.id")	
				linea(sb, "ide: $entregable.ide")	
				linea(sb, "ruta: $entregable.ruta")	
				presentarPlataformas(sb, entregable.platforms)
				presentarDlls(sb, entregable.dlls)
			}
		}
		return sb.toString()
	} 
	
	// Nombre del componente
	private String nombre;
	
	// Bibliotecas auxiliares
	private List<String> bibliotecas;
	
	// Entregables
	private List<C_VS_Deliverable> entregables;
	
	// Ficheros de configuración
	private List<C_VS_Configuration> cfgs;
	
	/**
	 * Constructor sobre un fichero para parsearlo
	 * @param f Fichero a parsear
	 */
	private C_VS_Component(File f) {
		nombre = f.getName().substring(0, f.getName().lastIndexOf("."))
		bibliotecas = new LinkedList<String>()
		entregables = new LinkedList<C_VS_Deliverable>()
		cfgs = new LinkedList<C_VS_Configuration>()
		parseImpl(f);
	}
	
	/** 
	 * Método estático de factoría
	 * @param f Fichero a parsear
	 * @return Componente parseado
	 */
	public static C_VS_Component parse(File f) {
		return new C_VS_Component(f)
	}

	public List<String> getBibliotecas() {
		return bibliotecas;
	}

	public List<C_VS_Deliverable> getEntregables() {
		return entregables;
	}

	public List<C_VS_Configuration> getCfgs() {
		return cfgs;
	}
	
	/** 
	 * Este método parsea un xml de componente extrayendo los entregables
	 * definidos en el mismo
	 * @param f Fichero a parsear
	 */
	private void parseImpl(File f) {
		def xml = new XmlParser().parse(f);		
		xml.libAux.each { lib ->
			bibliotecas << lib.ruta.text()
		}
		xml.app.each { app ->
			entregables << parseDeliverable(app)			
		}
		xml.lib.each { lib ->
			entregables << parseDeliverable(lib)			
		}
		xml.cfg.each { cfg ->
			cfgs << parseConfigFile(cfg)
		}		
	}
	
	/**
	 * Este método parsea un nodo xml para extraer del mismo la información
	 * de un entregable
	 * @param xml Nodo xml a parsear
	 */
	private C_VS_Deliverable parseDeliverable(xml) {
		C_VS_Deliverable ret = null;
		if (xml != null) {
			ret = new C_VS_Deliverable();
			ret.setType(xml.name())
			ret.setGroupId(xml.groupId?.text())
			ret.setId(xml.cfgId?.text())
			ret.setRuta(xml.ruta?.text())
			ret.setIde(xml.ide?.text())
			Boolean debug = Boolean.FALSE;
			if (xml.debug != null) {
				debug = Boolean.valueOf(xml.debug.text())
			}
			ret.setDebug(debug)
			xml.platforms?.platform.each { platform ->
				ret.addPlatform(parsePlatform(platform))
			}
			xml.dlls?.dll.each { dll ->
				C_VS_DLL objDll = new C_VS_DLL()
				objDll.setDll(dll.text())
				ret.addDll(objDll)
			}
		}
		return ret;		
	}
	
	/**
	 * Este método parsea un nodo xml para extraer del mismo la información
	 * de un fichero de configuración
	 * @param xml Nodo xml a parsear
	 */
	private C_VS_Configuration parseConfigFile(xml) {
		C_VS_Configuration ret = null;
		if (xml != null) {
			ret = new C_VS_Configuration();
			ret.setRuta(xml.ruta?.text())
			xml.platforms?.platform.each { platform ->
				ret.addPlatform(parsePlatform(platform))
			}
		}
		return ret;
	}
	
	/**
	 * Parsea el nodo xml buscando la información correspondiente a una plataforma
	 * @param xml Nodo xml a parsear
	 * @return Objeto C_VS_Platform con la información extraida del nodo
	 */
	private C_VS_Platform parsePlatform(platform) {
		C_VS_Platform pl = new C_VS_Platform()
		pl.setPlatId(platform.platId.text())
		pl.setPlatSubId(platform.platSubId?.text())
		pl.setRutatar(platform.rutatar.text())
		return (pl)
	}
}
