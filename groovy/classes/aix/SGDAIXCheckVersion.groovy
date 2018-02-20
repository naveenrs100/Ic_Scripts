package aix

import es.eci.utils.base.Loggable;
import es.eci.utils.ComponentVersionHelper
import es.eci.utils.ParameterValidator;
import es.eci.utils.Stopwatch;
import es.eci.utils.Utiles;

class SGDAIXCheckVersion extends Loggable {
	
	private String builtVersion;
	
	public void execute() {
		
		// Validación de obligatorios
		ParameterValidator.builder()
			.add("builtVersion", builtVersion)
		
		long millis = Stopwatch.watch {
			
			if ( !builtVersion.endsWith("SNAPSHOT") ) {
				throw new Exception("Se esta intentando realizar un build/deploy de un componente en version cerrada. Se cancela la construccion.");
			}
			
		}
		
	}

	/**
	 * @return the builtVersion
	 */
	public String getBuiltVersion() {
		return builtVersion;
	}

	/**
	 * @param builtVersion the builtVersion to set
	 */
	public void setBuiltVersion(String builtVersion) {
		this.builtVersion = builtVersion;
	}

}
