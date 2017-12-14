package jenkins

import es.eci.utils.ZipHelper;

/**
 *
 * Argumentos:
 * 
 * - Ruta completa de un fichero zip
 * - Ruta completa de directorio al que descomprimirlo
 */

ZipHelper.unzipFile(new File(args[0]), new File(args[1]));