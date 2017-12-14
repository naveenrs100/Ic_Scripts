package es.eci.utils.encoding;

import java.io.File;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import java.nio.charset.UnsupportedCharsetException


public class EncodingUtils {
	
	/**
	 * Obtiene el encoding de un archivo utilizando herramientas icu4j-57_1
	 * @param pomFile
	 * @return String charset
	 */
	public static String getEncodingName(File pomFile) {
		InputStream targetStream = new FileInputStream(pomFile);
		BufferedInputStream bis = new BufferedInputStream(targetStream);
		CharsetDetector cd = new CharsetDetector();
		cd.setText(bis);
		CharsetMatch cm = cd.detect();

		def charset = "";

		if (cm != null) {
			def reader = cm.getReader();
			charset = cm.getName();
			println("CHARSET de ${pomFile.getAbsolutePath()} ->" + charset)
		}else {
			throw new UnsupportedCharsetException()
		}
		return charset;
	}
	
	/**
	 * Indica la correspondencia del fichero con un determinado encoding.
	 * @param f Fichero a verificar
	 * @param charset Charset deseado
	 * @return Valor numérico estimado por icu4j de la probabilidad de 
	 * compatibilidad del fichero con el charset deseado
	 */
	public static int matchFileEncoding(File f, String charset) {
		int ret = 0;
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
		CharsetDetector cd = new CharsetDetector();
		cd.setText(bis);
		cd.detectAll().each { CharsetMatch cm ->			
			if (cm.getName() == charset) {
				ret = cm.confidence
			}
		}
		return ret;
	}
	
}