package es.eci.utils.versioner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import es.eci.utils.base.Loggable;
import es.eci.utils.encoding.EncodingUtils;

/**
 * Esta clase agrupa los métodos de escritura de XML con un encoding
 * concreto.
 */
public class XmlWriter extends Loggable {

	/**
	 * Transforma un org.w3c.dom.Document en un archivo destino;
	 * @param doc
	 * @param destFile
	 */
	public static void transformXml(Document doc, File destFile) {
		String encoding = EncodingUtils.getEncodingName(destFile);
		DOMSource domSource = new DOMSource(doc);
		StringWriter sw = new StringWriter();
		OutputStreamWriter char_output = new OutputStreamWriter(
				new FileOutputStream(destFile.getAbsolutePath()),
				Charset.forName(encoding).newEncoder()
				);
		StreamResult sr = new StreamResult(char_output);

		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
		transformer.transform(domSource, sr);
	}
	
}
