package jenkins

import hudson.model.*;
import es.eci.utils.ParamsHelper;

/**
 * Definimos un handle y que pasaremos luego al "ControllerDockerDirect"
 * y definimos el conjunto de variables de entorno que necesita el docker run
 * para levantar un contendedor correctamente.
 */

def number = build.getEnvironment(null).get("BUILD_NUMBER");
def jobName = build.getEnvironment(null).get("JOB_NAME").replaceAll(/\s+/, "");
String handle = "swarm_slave_${jobName}_${number}";

def dockerizeParams = [:];
dockerizeParams['identificador'] = handle;
// Herramientas Jenkins (revisar cuando sea compatible)
// def slaveGroovyHome = build.getEnvironment(null).get("SLAVE_GROOVY_HOME");
// dockerizeParams['extra_params']="-t Groovy-1.8.7=${slaveGroovyHome}";


// SE AÑADE EL PARÁMETRO 'identificador'
ParamsHelper.addParams(build, dockerizeParams);
