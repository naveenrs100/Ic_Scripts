import rtc.ProjectAreaCacheReader;

def stream = "GIS - PlataformaIC - DESARROLLO";

def area = findArea(stream);
area = area.replaceAll('\\(RTC\\)', '').trim();

println(area);

public String findArea(stream) {
	def ret = null;
	boolean busqueda = true;
	// Se busca la projectArea en el fichero full_areas.xml, que tarda menos.
	try {
		ProjectAreaCacheReader reader = new ProjectAreaCacheReader(new FileInputStream("C:/OpenDevECI/WSECI/DIC - Scripts/src/test/groovy/personal/full_areas.xml"));
		ret = reader.getProjectArea(stream);
	} catch (Exception e) {
		busqueda = false;
	}

//	if(ret == null || busqueda == false) {
//		// Se calcula el projectArea consultando directamente a RTC
//		if(gitGroup == null || gitGroup.trim().equals("")) {
//			if(pArea == null || pArea.trim().equals("") || pArea.trim().equals('${projectArea}')) {
//				RTCUtils ru = new RTCUtils();
//				def pa = ru.getProjectArea(
//						stream,
//						rtcUser,
//						rtcPass,
//						rtcUrl);
//				ret = pa;
//			} else {
//				ret = pArea;
//			}
//		} else {
//			ret = gitGroup.trim();
//		}
//	}
}