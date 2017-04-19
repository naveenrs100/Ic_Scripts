import java.nio.charset.Charset

import org.w3c.dom.Document

import es.eci.utils.encoding.EncodingUtils
import es.eci.utils.versioner.XmlUtils

File f = new File("C:/Users/dcastro.jimenez/Desktop/config.xml");
File fDest = new File("C:/Users/dcastro.jimenez/Desktop/config2.xml");

Document doc = XmlUtils.parseXml(f);

String[] charsetsToBeTested = ["windows-1253", "ISO-8859-7", "ISO-8859-1","UTF-8"];

EncodingUtils cd = new EncodingUtils();
Charset charset = cd.detectCharset(f, charsetsToBeTested);


println(charset.name());	

//println(EncodingUtils.getEncodingName(f));

XmlUtils.transformXml(doc, f);