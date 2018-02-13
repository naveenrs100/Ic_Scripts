package es.eci.utils.mail

import java.text.DateFormat 
import java.text.DecimalFormat
import java.text.SimpleDateFormat

import es.eci.utils.base.Loggable

/**
 * Esta clase agrupa operaciones de embellecimiento de formato de presentación para
 * el correo de cambios.
 */
class PrettyFormatter extends Loggable {
	
	//-----------------------------------------------------------------------
	// Constantes del formateador
	
	public static DateFormat SCM_CHANGE_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy - HH:mm");
	
	//-----------------------------------------------------------------------
	// Propiedades del formateador	
	
	// Formato de números de salida
	private DecimalFormat numberOutputFormat = new DecimalFormat("#####")
	// Posibles formatos de fechas de entrada
	private List<DateFormat> dateInputFormats = [
		new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss"),
		new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z", Locale.ENGLISH)
	];
	
	//-----------------------------------------------------------------------
	// Métodos del formateador

	public PrettyFormatter() {
	}
	
	/**
	 * Este método toma una fecha, o una cadena asimilable a una fecha,
	 * y la formatea como una fecha dd/MM/yyyy - HH:mm
	 * @param date Valor fecha o una cadena específica devuelta por RTC.  Los formatos
	 * posibles son:
	 * 2017-12-19-16:13:02
	 * Tue Dec 19 15:19:28 2017 +0100
	 * @return Fecha expresada en formato dd/MM/yyyy - HH:mm
	 */
	public String formatDate(def date) {
		String ret = null;
		try {
			Date d = null;
			if (date instanceof Date) {
				d = date;
			}
			if (d == null && date instanceof String) {
				dateInputFormats.each { DateFormat f ->
					try { 
						if (d == null) { 
							d = f.parse(date.trim()) 
						}
					} catch (Exception e) { 
						d = null 
					}
				}
			}
			ret = SCM_CHANGE_DATE_FORMAT.format(d);
		}
		catch (Exception e) {
			log("[WARNING] No se puede interpretar la fecha " + date)
			ret = "";
		}
		return ret;
	}
	
	/**
	 * Este método toma un número, o una cadena asimilable a un número,
	 * y lo formatea sin decimales.
	 * @param number Algo interpretable como un número
	 * @return Número entero expresado sin decimales
	 */
	public String formatInteger(def number) {
		Number n = null;
		if (number instanceof Number) {
			n = number;
		}
		if (n == null) {
			// Intentar parsearlo
			n = Float.parseFloat(number);
		}
		return numberOutputFormat.format(n);
	}
}
