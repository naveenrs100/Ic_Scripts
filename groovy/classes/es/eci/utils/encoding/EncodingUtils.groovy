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
	
}