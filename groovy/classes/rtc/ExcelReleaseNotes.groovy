package rtc;


@Grab(group='org.apache.poi', module='poi-ooxml', version='3.10.1')

import java.text.DateFormat;
import java.text.SimpleDateFormat

import components.ComponentsParserLight
import components.RTCChangeSet

import components.RTCComponent

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;


/**
 * Esta clase implementa la transformación de un changelog de release notes en 
 * una hoja de cálculo
 * 
 * Se puede ejecutar como script con los siguientes parámetros:
 * Ruta absoluta de un changelog de release notes
 * Ruta absoluta donde se desea crear la hoja de cálculo con el resultado
 * Juego de caracteres del fichero (opcional)
 */


excel(args[0], args[1], args.length > 2 ? args[2] : null)

/**
 * Este método convierte un changelog de release notes en una hoja de cálculo
 * @param ficheroOrigen Ruta absoluta de un changelog de release notes
 * @param ficheroDestino Ruta absoluta del fichero de destino
 * @param charset 
 */
def excel(String ficheroOrigen, String ficheroDestino, String charset = null) {
	println "ficheroOrigen <- ${ficheroOrigen}"
	println "ficheroDestino <- ${ficheroDestino}"
	println "charset <- ${charset}"
	def env = System.getenv()
	def usuarioJenkinsRTC = env['userRTC']
	// Validaciones de parámetros
	if (ficheroOrigen == null || ficheroDestino == null) {
		throw new NullPointerException()
	}
	File origen = new File(ficheroOrigen)
	File destino = new File(ficheroDestino)
	if (!origen.exists() || !origen.isFile()) {
		throw new IllegalArgumentException(ficheroOrigen)
	}
	destino.createNewFile()
	// Recorrer el fichero
	ComponentsParserLight parser = new ComponentsParserLight()
//		parser.initLogger { this.logger }
	parser.initLogger { println it }
	List<RTCComponent> componentes = parser.parse(origen, charset)
	Workbook book = new SXSSFWorkbook(); 
	DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm")
	// A veces los componentes se repiten
	Map<String, Integer> apariciones = new HashMap<String, Integer>()
	CellStyle estiloCabecera = book.createCellStyle()
	Font font = book.createFont();
	font.setBoldweight(Font.BOLDWEIGHT_BOLD)
	estiloCabecera.setFont(font);
	componentes.each { RTCComponent componente ->
		Sheet hoja = null
		if (!apariciones.containsKey(componente.nombre)) {
			hoja = book.createSheet(componente.nombre)
			apariciones.put(componente.nombre, 1)
		}
		else if (componente.cambios.size() > 0) {
			Integer numero = apariciones.get(componente.nombre)
			numero = numero + 1
			apariciones.put(componente.nombre, numero)
			hoja = book.createSheet(componente.nombre + " " + numero)
		}
		if (hoja != null) {
			Row columnas = hoja.createRow(0)
			// Id de cambio, autor, email, idTarea, comentario, fecha
			def titulosColumnas = [/*'Id de cambio', */'Autor', 'Email', 'WorkItem', 'Comentario', 'Fecha']
			int numColumna = 0
			titulosColumnas.each { titulo ->
				Cell celda = columnas.createCell(numColumna++) 
				celda.setCellStyle(estiloCabecera)
				celda.setCellValue(titulo)
			} 
			short numFila = 1
			List<RTCChangeSet> cambios = componente.getCambios()
			Collections.sort(cambios, new Comparator<RTCChangeSet>() {
				int compare(RTCChangeSet c1, RTCChangeSet c2) {
					return -1 * c1.fecha.compareTo(c2.fecha)
				};
			}) 
			cambios.each { RTCChangeSet changeSet ->
				if (changeSet.autor != usuarioJenkinsRTC) {
					Row fila = hoja.createRow(numFila++)
					int numCelda = 0
					[ /*changeSet.id, */changeSet.autor, changeSet.email, changeSet.idTarea, changeSet.comentario, df.format(changeSet.fecha)].each { campo ->
						fila.createCell(numCelda++).setCellValue(campo)
					}
				}
			}
		}
	}
	OutputStream os = new FileOutputStream(destino)
	try {
		book.write(os)
	}
	finally {
		if (os != null) {
			os.close();
		}
	}
}
