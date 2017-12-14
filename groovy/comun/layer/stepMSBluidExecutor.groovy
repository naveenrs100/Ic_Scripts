/**
 * Esta clase permite ejecutar MSBuild en cada una de las soluciones que encuentre
 * en el componente.
 */
package layer;

import es.eci.utils.ParameterValidator
import es.eci.utils.SystemPropertyBuilder

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
Map params = propertyBuilder.getSystemParameters();

MSBuildExecutorCommand msbuildExecutorCommand = new MSBuildExecutorCommand();

String msbuildPath = params['msbuildPath']
String componentHome = params['componentHome']
 
ParameterValidator.builder().
	add("componentHome", componentHome).
	add("msbuildPath", msbuildPath).build().validate();

msbuildExecutorCommand.initLogger { println it }

msbuildExecutorCommand.setMsbuildPath(msbuildPath)
msbuildExecutorCommand.setComponentHome(componentHome)

msbuildExecutorCommand.execute()