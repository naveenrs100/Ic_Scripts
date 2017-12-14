/**
 * Esta clase permite descargar de Nexus las librerias necesarias para el entorno de
 * compilación de Windows para Layer, en base a las librerías que se indiquen en el
 * fichero entorno_windows.comp
 */
package layer;

import es.eci.utils.ParameterValidator
import es.eci.utils.SystemPropertyBuilder

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
Map params = propertyBuilder.getSystemParameters();

EnvironmentProviderCommand environmentProviderWindows = new EnvironmentProviderCommand();

String urlNexus = params['urlNexus']
String finalLocation = params['finalLocation']
 
ParameterValidator.builder().
	 add("urlNexus", urlNexus).
	 add("finalLocation", finalLocation).build().validate();

environmentProviderWindows.initLogger { println it }

environmentProviderWindows.setUrlNexus(urlNexus)
environmentProviderWindows.setFinalLocation(finalLocation)

environmentProviderWindows.execute()