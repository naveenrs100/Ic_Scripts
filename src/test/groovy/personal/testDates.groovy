import es.eci.utils.StringUtil;

def inst = "Sprint 1.1.2";
def stream = "GIS - MICORRIENTE - Development";

println(composeUrbanSnapshot(inst,stream));



def composeUrbanSnapshot(String instantanea, String stream) {
	ArrayList<String> sufijos = new ArrayList<String>(){{
			add(" - DESARROLLO");
			add("-DESARROLLO");
			add(" - RELEASE");
			add("-RELEASE");
			add(" - MANTENIMIENTO");
			add("-MANTENIMIENTO");
			add(" - DEVELOPMENT");
			add("-DEVELOPMENT");
			add(" - Development");
			add("-Development");
			add("-FrozenDevelopment");
			add(" - FrozenDevelopment");
	}}
	
	String urbanSnapshot = null;
	String noSuffixStream = stream.split(sufijos.first())[0];
	for(String suffix : sufijos) {
		noSuffixStream = noSuffixStream.split(suffix)[0];		
	}
	String normStream = StringUtil.normalize(noSuffixStream);
	String normInstantanea = instantanea != null ? StringUtil.normalize(instantanea) : null;
	String date = new Date().format("yyyyMMddHHmmss");
	
	if(instantanea != null && !instantanea.trim().equals("")) {
		urbanSnapshot = "${date}_${normStream}_${normInstantanea}";
	}
	else {
		urbanSnapshot = "${date}_${normStream}";
	}
	return urbanSnapshot;
}