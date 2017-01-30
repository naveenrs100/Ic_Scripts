/**
 * Este script determina si existen tests unitarios en el proyecto.
 * 
 * SALIDA DEL SCRIPT: Imprime por consola la variable HAY_TESTS, true/false
 */

import cobertura.*

def proyecto = new File(args[0])
def tecnologia = args[1]

if (tecnologia == null || tecnologia.trim().length() == 0) {
	tecnologia = "maven"
}

def salida = null

if (!CoberturaHelper.findUnitTests(proyecto, tecnologia)) {
	salida = "false"
}
else {
	salida = "true"
}


println salida