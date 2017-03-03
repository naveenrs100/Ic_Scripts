import es.eci.utils.SystemPropertyBuilder

/**
 * Este script simplemente espera un número de segundos aleatorio entre 1 y
 * maxWaitTime pasado como parámetro. El objetivo del script es reducir la
 * frecuencia de errores de tipo java.nio.file.FileAlreadyExistsException que
 * están identificados como bug en el core de Jenkins (v1.651.3) como consecuencia
 * de la ejecución de múltiples finalizaciones de jobs en paralelo.
 */

// Lógica
 
Closure log = { println it }

// Carga de variables (params de entrada)
 
SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
Map<String, String> mapaVars = propertyBuilder.getSystemParameters();

int maxWaitTime = Integer.parseInt(mapaVars["maxWaitTime"])		// Máximo tiempo de espera.

log "Esperando un número aleatorio entre 1 y ${maxWaitTime} secs..."

Random random = new Random()
sleep(random.nextInt(1 * maxWaitTime) * 1000)

log "Continua la ejecución."