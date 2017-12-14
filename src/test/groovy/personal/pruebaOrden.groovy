import java.io.File;
import java.util.List;
import java.util.Map;

import es.eci.utils.RTCBuildFileHelper;


RTCBuildFileHelper helper = new RTCBuildFileHelper("release", new File("C:/OpenDevECI/WSECI_NEON"));

def base = "C:/OpenDevECI/WSECI_NEON"

Map<String, List<File>> ficheros = ["6IX - C6E06IX1":
										[new File("${base}/6IX - C6E06IX1/6IX-C6E06IX1-JAR/pom.xml"),
										new File("${base}/6IX - C6E06IX1/pom.xml")],
									"6IX - Intermarketing":
										[new File("${base}/6IX - Intermarketing/pom.xml"),
										new File("${base}/6IX - Intermarketing/6IX-Intermarketing-EAR/pom.xml"),
										new File("${base}/6IX - Intermarketing/6IX-Intermarketing-WEB/pom.xml"),
										new File("${base}/6IX - Intermarketing/ValidadorCampos-JAR/pom.xml")]];

helper.processSnapshotMaven("release", 
							ficheros,
							new File("C:/OpenDevECI/WSECI_NEON"), 
							new File("C:/OpenDevECI/WSECI_NEON"), 
							null)