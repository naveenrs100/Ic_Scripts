package rtc.workitems

import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * Esta clase modela un comentario adjunto a la discusión de un 
 * work item de RTC.
 */
class CommentBean {

	//--------------------------------------------------------------
	// Constantes del comentario
	
	// Formato de fechas
	public static final DateFormat DATE_FORMAT = 
		new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSSX");
	
	//--------------------------------------------------------------
	// Propiedades del comentario
	
	// Fecha de creación
	private Date timestamp;
	// Autor del comentario
	private String creator;
	// Contenido del comentario
	private String content;
	
	//--------------------------------------------------------------
	// Métodos del comentario
	
	// Constructor privado
	private CommentBean() {
		
	}
	
	/**
	 * Parsea un xml de discusión devuelto por el servicio para convertirlo 
	 * en una lista de comentarios.
	 * @param rawXML XML crudo devuelto por el servicio.
	 * @return Lista de objetos con la información de cada comentario.
	 */
	public static List<CommentBean> from (String rawXML) {
		List<CommentBean> ret = [];
		
		def discussion = new XmlParser().parseText(rawXML);
		discussion['rdf:Description'].each { comment ->
			// Descartar aquellos que 
			if (comment['dcterms:description'] != null && comment['dcterms:description'].size() > 0) {
				CommentBean bean = new CommentBean();
				String date = comment['dcterms:created'][0].text()
				bean.timestamp = DATE_FORMAT.parse(date);
				String creator =  AttributeHelper.getAttribute(
					comment['dcterms:creator'][0], "resource");
				// El creator puede venir como una URL, intentamos limpiarla y,
				//	si no, la ponemos tal cual
				bean.creator = cleanCreatorURL(creator);
				bean.content = comment['dcterms:description'][0].text();
				ret << bean;
			}
		}
		
		if (ret != null && ret.size() > 0) {
			Collections.sort(ret, new Comparator<CommentBean>() {
				int compare(CommentBean a, CommentBean b) {
					return a.timestamp.compareTo(b.timestamp)
				};
			});	
		}		
		return ret;
	}
	
	// Busca un patrón de URL de creador, tal que:
	// https://rtc.elcorteingles.pre:59443/jts/users/X12345AB
	// Y si es posible lo reduce al id de usuario: X12345AB
	// De lo contrario, respeta el formato de URL
	private static String cleanCreatorURL (String url) {
		String ret = url;
		def matcher = url =~ /.+users\/(\w+)/
		if (matcher.matches()) {
			ret = matcher[0][1]
		}
		return ret;
	}

	/**
	 * @return the timestamp
	 */
	public Date getTimestamp() {
		return timestamp;
	}

	/**
	 * @return the creator
	 */
	public String getCreator() {
		return creator;
	}

	/**
	 * @return the content
	 */
	public String getContent() {
		return content;
	}
	
	@Override
	public String toString() {
		return "[$creator] [${DATE_FORMAT.format(timestamp)}] - $content"
	}
}
