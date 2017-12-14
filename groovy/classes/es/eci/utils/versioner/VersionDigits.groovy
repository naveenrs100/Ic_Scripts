package es.eci.utils.versioner;

class VersionDigits implements Comparable {

	String version;

	public VersionDigits(String version) {
		this.version = version;
	}

	@Override
	/**
	 * Compara con otro objeto VersionDigits.
	 */
	public int compareTo(Object otherVersion) {
		int ret = 0;
		def versionDigits = version.split("\\.");;
		def otherVersionDigits = (((VersionDigits)otherVersion).getVersion()).split("\\.");
		//println("Comparando version ${this.version} con ${otherVersion.getVersion()}")

		for(int i=0; i < versionDigits.length; i++ ) {
			if(otherVersionDigits[i] != null) {
				def versionDigitInt = Integer.valueOf(versionDigits[i].split("-")[0]);
				def otherVersionDigitInt = Integer.valueOf(otherVersionDigits[i].split("-")[0]);
				//println("\t--Comparando digito ${versionDigits[i]} con ${otherVersionDigits[i]}");
				if(versionDigitInt > otherVersionDigitInt) {
					ret = 1;
					break;
				}
				else if(versionDigitInt < otherVersionDigitInt) {
					ret = -1;
					break;
				} else {
					def versionDigitRemanente = versionDigits[i].split("-").size() > 1 ? versionDigits[i].split("-")[1] : null;
					def otherVersionDigitRemanente = otherVersionDigits[i].split("-").size() > 1 ? otherVersionDigits[i].split("-")[1] : null;
					if(versionDigitRemanente != null && otherVersionDigitRemanente != null) {
						if(Integer.valueOf(versionDigitRemanente) > Integer.valueOf(otherVersionDigitRemanente)) {
							ret = 1;
							break;
						} else if (Integer.valueOf(versionDigitRemanente) < Integer.valueOf(otherVersionDigitRemanente)) {
							ret = -1;
							break;
						}
					} else if (versionDigitRemanente == null && otherVersionDigitRemanente != null) {
						ret = -1;
						break;
					} else if(versionDigitRemanente != null && otherVersionDigitRemanente == null) {
						ret = 1;
						break;
					}
				}
			}
			else if(otherVersionDigits[i] == null) {
				ret = 1;
				break;
			}
		}
		return ret;
	}

	
	// Getters and setters
	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

}
