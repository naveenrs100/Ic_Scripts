import es.eci.utils.versioner.VersionDigits; 

def versions = [	
	"2.0.0.1.0-1",
	"2.0.0.0.0-10",
	"2.0.0.0.0-9",
	"1.0.0.0.1-9",
	"2.0.0.0.10-6",
	"2.0.0.0.8-2",
	"1.0.0.0.0-3"
	]


def versionObjects = [];
versions.each {
	versionObjects.add(new VersionDigits(it));
}

def listaOrdenada = versionObjects.sort{ it }

def finalVersionList = [];

listaOrdenada.each {
	finalVersionList.add(it.getVersion());
}

println(finalVersionList)


//public class VersionDigits implements Comparable {
//
//	String version;
//
//	public VersionDigits(String version) {
//		this.version = version;
//	}
//
//	@Override
//	public int compareTo(Object otherVersion) {
//		int ret = 0;
//		def versionDigits = version.split("\\.");;
//		def otherVersionDigits = (((VersionDigits)otherVersion).getVersion()).split("\\.");
//		println("Comparando version ${this.version} con ${otherVersion.getVersion()}")
//
//		for(int i=0; i < versionDigits.length; i++ ) {
//			if(otherVersionDigits[i] != null) {
//				def versionDigitInt = Integer.valueOf(versionDigits[i].split("-")[0]);
//				def otherVersionDigitInt = Integer.valueOf(otherVersionDigits[i].split("-")[0]);
//				println("\t--Comparando digito ${versionDigits[i]} con ${otherVersionDigits[i]}");
//				if(versionDigitInt > otherVersionDigitInt) {
//					ret = 1;
//					break;
//				}
//				else if(versionDigitInt < otherVersionDigitInt) {
//					ret = -1;
//					break;
//				} else {
//					def versionDigitRemanente = versionDigits[i].split("-").size() > 1 ? versionDigits[i].split("-")[1] : null;
//					def otherVersionDigitRemanente = otherVersionDigits[i].split("-").size() > 1 ? otherVersionDigits[i].split("-")[1] : null;
//					if(versionDigitRemanente != null && otherVersionDigitRemanente != null) {
//						if(Integer.valueOf(versionDigitRemanente) > Integer.valueOf(otherVersionDigitRemanente)) {
//							ret = 1;
//							break;
//						} else if (Integer.valueOf(versionDigitRemanente) < Integer.valueOf(otherVersionDigitRemanente)) {
//							ret = -1;
//							break;
//						}
//					} else if (versionDigitRemanente == null && otherVersionDigitRemanente != null) {
//						ret = -1;
//						break;
//					} else if(versionDigitRemanente != null && otherVersionDigitRemanente == null) {
//						ret = 1;
//						break;
//					}
//				}
//			}
//			else if(otherVersionDigits[i] == null) {
//				ret = 1;
//				break;
//			}
//		}
//		return ret;
//	}
//
//	public String getVersion() {
//		return this.version;
//	}
//
//}