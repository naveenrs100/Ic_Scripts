package kiuwan

import es.eci.utils.SystemPropertyBuilder

/**
 * Este script se ejecuta como Groovy Script simple
 * 
 * Lanza el análisis mediante la herramienta de línea de comandos de kiuwan
 * 
 * Parámetros de entrada
 * 
 * parentWorkspace Directorio de ejecución
 * component Componente construido - dato para kiuwan
 * builtVersion Versión construida del software - dato para kiuwan
 * product Producto - dato para kiuwan
 * subsystem Subsistema - dato para kiuwan
 * action Acción del proceso de IC ejecutado
 * provider Proveedor - dato para kiuwan
 * changeRequest Petición de cambio - dato para kiuwan
 * kiuwanPath Ruta del ejecutable de kiuwan
 * kiuwanExclusions Exclusiones definidas para el análisis kiuwan
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
KiuwanExecutor executor = new KiuwanExecutor();
executor.initLogger { println it }
propertyBuilder.populate(executor);

executor.execute();
