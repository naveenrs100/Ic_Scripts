package components

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator
import com.deluan.jenkins.plugins.rtc.changelog.JazzChangeSet
import java.io.BufferedReader;


/** 
 * Esta clase debe implementar un parser sencillo de la información facilitada
 * por RTC sobre los componentes de una corriente.
 * 
 * 
 * Un listado de componentes típico tendrá el siguiente formato si se ha invocado
 *	con onlyChanges=false:
 	
Workspace: (8749) "CCCC-SSP_PDA_PruebaConcepto-DESARROLLO"
  Component: (8750) "6MC-LibMovilidad"
  Component: (8751) "6MC-GapiCM"
  Component: (8752) "6BF-HeadersComunes"
  Component: (8753) "6BF-GestionMercancias"
  Component: (8754) "6MC-environment"
 
 * En cambio, en caso de invocarse con onlyChanges=true, sería así:
 
Incoming Changes
  Component (8753) "6BF-GestionMercancias" (added)
  Component (8752) "6BF-HeadersComunes" (added)
  Component (8751) "6MC-GapiCM" (added)
  Component (8750) "6MC-LibMovilidad" (added)
  Component (8754) "6MC-environment" 
 
 * Una llamada en la que figuran cambios en alguno de los componentes:
 
 Incoming Changes
  Component (7705) "6A2_BloqueoBinarios" (added)
  Component (7359) "6A2_MSYC"
  Component (7360) "6A2_TPV"
    (7752) |Carlos Olmos Teruel|carlos_olmos@ieci.es| 57175: Crear la función 65 - Configuración de impresion de tique en la funcion 65 para portugal |2014-08-07-12:37:19|
    (7753) |Fernando Miguel Romero Guerra|fernandomiguel_romero@ieci.es| 56943: Mensaje de no emisión FPP a Bote MQ - Actualización y mejoras. |2014-08-07-14:43:30|
    (7754) |Pablo Iglesias Cardenas|pablo_iglesias@gexterno.es| 56624: Activar operativa mensaje confirmación de cobro - Correccion para fallos detectados al anular y al abonar |2014-08-08-17:16:35|
    (7755) |Nuria Gonzalez Sanchez|nuria_gonzalez@gexterno.es| 58218: Crear armazón de fases y GF para la venta - Creación de la estructura de fases y GF para poder ir enganchando todos los cambios |2014-08-11-09:53:27|
    (7756) |Pablo Iglesias Cardenas|pablo_iglesias@gexterno.es| 58134: Adaptar el abono para que permita que la totalización proceda de extensibilidad - Se añade y modifica del abono las fases y BC's para que trate los datos procedentes de la extensibilidad. |2014-08-11-10:29:21|
    (7757) |Luis Espinosa Cacicedo|luis_espinosa@elcorteingles.es| 57639: Mensajería necesaria para la gestión de la tarjeta anticipo - tajeta anticipo |2014-08-11-11:50:32|
  Component (7361) "6A2_Toolkit" (added)
  Component (7362) "6A2_UpdateToolkit" (added)
 
 * 
 */
class ComponentsParser {

	
	private Closure logger = null
	
	/**
	 * Dota a la clase de un log
	 * @param logger Closure que imprime el log a donde quiera que se haya
	 * inicializado
	 */
	def initLogger( Closure logger ) {
		this.logger = logger
	}
	
	// log de la clase haciendo uso de closure
	def log(def msg) {
	  if (logger != null) {
		  println logger(msg)
	  }
	}
	

	// Formato esperado:
	//	2014-08-11-09:53:27
	DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
	

	def REGEX_COMP = /Component.*\((.*)\).*"(.*)"[\s]*(\(added\))*/
	def REGEX_CHANGE = /\((.*)\)\s*\|([^|]*)\|([^|]*)\|(.*)\|([^|]*)\|$/
	
	/**
	 * Devuelve un mapa de conjuntos de cambios al estilo del plugin de RTC
	 * de jenkins.
	 */
	public Map<String, JazzChangeSet> parseJazz(BufferedReader compareFile) {
		String line = null
		Map<String, JazzChangeSet> result = new LinkedHashMap<String, JazzChangeSet>()
		while ((line = compareFile.readLine()) != null) {
			println "Inicio matcher."
			def matcherChange = (line.trim() =~ REGEX_CHANGE)
			if (matcherChange.matches()) {
				println "Match realizado."
				RTCChangeSet cambio = parsearCambio(matcherChange)
				println "Cambio en RTC: " + cambio.id
				JazzChangeSet changeSet = new JazzChangeSet();
				changeSet.setRev(cambio.id);
				changeSet.setUser(cambio.autor);
				changeSet.setEmail(cambio.email);
				changeSet.setMsg(cambio.comentario);
				changeSet.setDate(cambio.fecha);
				
				result.put(changeSet.getRev(), changeSet)
			}
		}
		return result
	}
	
	/**
	 * Este método parsea el contenido de un fichero componentsCompare.txt 
	 * típico de los generados en el workflow unificado.  Devuelve una lista 
	 * de componentes indicando si han recibido cambios o no
	 * @param f Fichero componentsCompare
	 * @return Objetos RTCComponent con la información correspondiente a cada componente 
	 */
	public List<RTCComponent> parse(File f) {
		
		List<RTCComponent> ret = []		
		
		
		if (f != null && f.exists()) {
			List<String> lineas = []
						
			
			f.eachLine { linea -> lineas << linea }
			RTCComponent compActual = null
			Iterator<String> iter = lineas.iterator();
			boolean seguir = true
			while(seguir) {
				if (!iter.hasNext()) {
					seguir = false
				}
				else {
					String linea = iter.next().trim()
					def matcherComp = (linea =~ REGEX_COMP)					
					if (matcherComp.matches()) {
						String id = matcherComp[0][1];
						String nombre = matcherComp[0][2];
						boolean cambios = false;
						boolean added = false;
						if (matcherComp[0].size() >= 4) {
							added = (matcherComp[0][3] == 'added')
						}
						compActual = new RTCComponent(id, nombre, added)
						ret << compActual
					}
					else { 
						def matcherChange = (linea =~ REGEX_CHANGE)
						if (matcherChange.matches()) {
							if (compActual != null) {
								RTCChangeSet cambio = parsearCambio(matcherChange)
								compActual.anyadirCambio(cambio)
							}
						}
					}
				}
			} 
			
			
		}
		
		return ret
	}
	
	// Parsea la información de un cambio en RTC
	def RTCChangeSet parsearCambio(matcherChange) {
		String id = matcherChange[0][1];
		String autor = matcherChange[0][2];
		String email = matcherChange[0][3];
		String comentarioTarea = matcherChange[0][4];
		String cadenaFecha = matcherChange[0][5];
		List splitComentario = comentarioTarea.trim().split(":")
		String idTarea = null;
		String comentario = null;
		if (splitComentario.size() > 1) {
			idTarea = splitComentario.head();
			comentario = splitComentario.tail().join("");
		} 
		else {
			comentario = comentarioTarea.trim();
		}
		Date fecha = null;
		if (cadenaFecha != null) {
			try {
				fecha = df.parse(cadenaFecha)
			}
			catch (Exception e) {
				log (e.getMessage())
			}
		}
		return new RTCChangeSet(id, autor, email, fecha, idTarea, comentario)
	}
}