package vs

import java.io.File;


/**
 * Esta clase guarda las rutas asociadas a un entorno de compilación.
 */
class C_VS_CompilationEnvironment {
	
	private String lib;
	private String include;
	private String bitmap;
	
	// Este método intenta parsear un entorno de copmilación a partir de un fichero xml.
	//		Espera encontrar un elemento apps/entornoCompilacion
	public static C_VS_CompilationEnvironment parsearEntornoCompilacion(File fichero) {
		C_VS_CompilationEnvironment ret = null;
		if (fichero != null && fichero.isFile() && fichero.exists()) {
			try {
				def xml = new XmlParser().parse(fichero);	
				if (xml.entornoCompilacion != null && xml.entornoCompilacion.size() > 0)  {
					String lib = xml.entornoCompilacion[0].lib?.text()
					String include = xml.entornoCompilacion[0].include?.text()
					String bitmap = xml.entornoCompilacion[0].bitmap?.text()
					ret = new C_VS_CompilationEnvironment(lib, include, bitmap)
				}
			}
			catch (Exception e) {
				println "El fichero ${fichero.getCanonicalPath()} no es un fichero xml"
			}
		}
		return ret;
	}
	
	private C_VS_CompilationEnvironment(String lib, String include, String bitmap) {
		this.lib = lib;
		this.include = include;
		this.bitmap = bitmap;
	}
	
	public String getLib() {
		return lib;
	}
	
	public String getInclude() {
		return include;
	}
	
	public String getBitmap() {
		return bitmap;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Lib -> ")
		sb.append(lib == null?"":lib)
		sb.append(System.getProperty("line.separator"))
		sb.append("Include -> ")
		sb.append(include == null?"":include)
		sb.append(System.getProperty("line.separator"))
		return sb.toString();
	}
}