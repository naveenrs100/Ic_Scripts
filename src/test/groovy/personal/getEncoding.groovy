import es.eci.utils.encoding.EncodingUtils;
import es.eci.utils.versioner.*;

EncodingUtils eu = new EncodingUtils();

File componentDir = new File("C:/OpenDevECI/WSECI_NEON/6A2_SCO_NCR");


PomXmlOperations.checkOpenVersion(componentDir, null);
