package vs
import es.eci.utils.Utiles
import groovy.io.FileType

/**
 * Esta clase genera un fichero .inf de generación de MS Cabinet a partir de
 * una lista de ficheros en un determinado directorio
 */

class CabinetConfigGenerator {
	
	//-----------------------------------------------------
	// Propiedades de la clase
	
	// Directorio base
	private File dir;
	
	// Filtro de ficheros
	private List<String> fileFilter = null;
	// Lista de ficheros en el directorio base
	private List<File> files = null;
	
	//-----------------------------------------------------
	// Métodos de la clase
	
	/**
	 * Construye el generador a partir de un directorio base y una lista de
	 * ficheros a extraer
	 * @param dir Directorio base del cual extraer ficheros
	 * @param ficheros Lista de ficheros a extraer (por nombre y sin ruta relativa).  Parámetro opcional,
	 * de venir null se extrae todo el contenido del directorio.  Si viene informado, 
	 * es un filtro sobre el contenido
	 * del directorio
	 */
	public CabinetConfigGenerator(File dir, List<String> fileFilter = null) {
		 this.dir = dir;
		 this.fileFilter = fileFilter;
		 readFiles();
	}
	
	/**
	 * Lee el directorio base para extraer una lista de ficheros, dado un filtro previo
	 */
	private void readFiles() {
		if (dir != null && dir.exists() && dir.isDirectory()) {
			files = new ArrayList<File>();
			dir.traverse(type : FileType.FILES,
					maxDepth: -1) { file ->
				if (fileFilter == null || fileFilter.size() == 0 || fileFilter.contains(file.getName())) {
					files << file;
				}
			}
		}
	}
	
	/*
	 * Añade al StringBuilder una sección con unos determinados valores
	 * @param sb Buffer sobre el que se construye el fichero .inf
	 * @param title Título de una sección
	 * @param values Valores de la sección
	 */
	private void addSimpleSection(StringBuilder sb, String title, Map<String, String> values) {
		sb.append("[");
		sb.append(title);
		sb.append("]\n");
		if (values != null) {
			for (String key: values.keySet()) {
				if (key.compareTo("") != 0) { 
					sb.append(key);
					sb.append("=");
				}
				sb.append(values.get(key));
				sb.append("\n");
			}
		}
	}
	
	/*
	 * Construye un listado de ficheros Files.Common1,Files.Common2,...
	 * @return Files.Common1,Files.Common2,...
	 */
	private String listCommonFiles() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < files.size(); i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append("Files.Common");
			sb.append(i + 1);
		}
		return sb.toString();
	}
	
	/*
	 * Construye un map indexado por orden y con la ruta del directorio que contiene cada fichero
	 1=,"Common1",,"E:\PDA\102_2V\6BS-MT2090Inventario_PDA\Instala\ECIMT2090\"
	 2=,"Common2",,"E:\PDA\102_2V\6BS-MT2090Inventario_PDA\Instala\"
	 ...
	 */
	private Map<String, String> mapFilePaths() {
		Map<String, String> ret = new LinkedHashMap<String, String>();
		for(int i = 0; i < files.size(); i++) {
			ret.put(new Integer(i + 1).toString(), ",\"Common"+( i + 1) + "\",,\"" + files[i].getParentFile() + "\"");
		}
		return ret;
	}
	
	/*
	 * Construye un map indexado por nombre de fichero y con el índice del mismo.
	 	"GapiCM.dll"=1
		"ECIMT2090._nk"=2
		"Español.IDM"=3
		"MT2090.CPY"=4
		"Inglés.IDM"=5
		"Portugués.IDM"=6
		"STARTMENU.RUN"=7
		"Navigator._nk"=8
		"ECIMT2090.exe"=9
	 */
	private Map<String, String> mapFileNames() {
		Map<String,String> ret = new LinkedHashMap<String, String>();
		for(int i = 0; i < files.size(); i++) {
			ret.put("\"" + files.get(i).getName() + "\"", new Integer(i + 1).toString());
		}
		return ret;
	}
	
	/*
	 * Construye un map con una entrada fija de shortcuts y a continuación la ruta
	 * relativa dentro del cabinet para cada fichero.  Para dicha ruta se va a utilizar
	 * la diferencia entre la ruta total del fichero y el directorio base original
	 	Shortcuts=0,%CE2%\Start Menu
		Files.Common1=0,"Application\ECIMT2090"
		Files.Common2=0,"Application"
		Files.Common3=0,"Application\ECIMT2090"
		Files.Common4=0,"Application"
		Files.Common5=0,"Application\ECIMT2090"
		Files.Common6=0,"Application\ECIMT2090"
		Files.Common7=0,"Application\Startup"
		Files.Common8=0,"Application"
		Files.Common9=0,"Application\ECIMT2090"
	 */
	private Map<String, String> mapDestinationDirs() {
		Map<String,String> ret = new LinkedHashMap<String, String>();
		ret.put("Shortcuts", '0,%CE2%\\Start Menu');
		for(int i = 0; i < files.size(); i++) {
			ret.put("Files.Common" + ( i + 1), "0,\"" + Utiles.rutaRelativa(dir, files[i]) + "\"")
		}
		return ret;
	}
	
	/**
	 * Crea un fichero .inf con la información necesaria para crear un cabinet
	 * que contenga todos los ficheros indicados del directorio base
	 * @return Fichero inf para construir un cabinet simple
	 */
	public String createInfFile() {
		StringBuilder sb = new StringBuilder();
		addSimpleSection(sb, "Version", ['Signature':'"$Windows NT$"','Provider':'"El Corte Inglés S.A"','CESignature':'"$Windows CE$"']);
		sb.append("\n");
		addSimpleSection(sb, "CEStrings", ['AppName':'"MT2090"','InstallDir':'%CE1%\\%AppName%']);
		sb.append("\n");
		addSimpleSection(sb, "Strings", ['Manufacturer':'"El Corte Inglés S.A"']);
		sb.append("\n");
		addSimpleSection(sb, "CEDevice", ['VersionMin':'4.0','VersionMax':'6.99','BuildMax':'0xE0000000']);
		sb.append("\n");
		addSimpleSection(sb, "DefaultInstall", ['CEShortcuts':'Shortcuts','AddReg':'RegKeys','CopyFiles':listCommonFiles()]);
		sb.append("\n");
		addSimpleSection(sb, "SourceDisksNames", mapFilePaths());
		sb.append("\n");
		addSimpleSection(sb, "SourceDisksFiles", mapFileNames());
		sb.append("\n");
		addSimpleSection(sb, "DestinationDirs", mapDestinationDirs());
		for(int i = 0; i < files.size(); i++) {
			sb.append("\n");
			addSimpleSection(sb, "Files.Common" + (i + 1), ['':'\"' +files[i].name + '\",\"' + files[i].name + '\",,0']);
		}
		sb.append("\n");
		addSimpleSection(sb, "Shortcuts", null);
		sb.append("\n");
		addSimpleSection(sb, "RegKeys", null);
		
		
		return sb.toString();
	}
}