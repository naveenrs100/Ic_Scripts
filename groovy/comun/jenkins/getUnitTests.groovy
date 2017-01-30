/**
 * Este script determina si existen tests unitarios en el proyecto.
 * 
 * SALIDA DEL SCRIPT: Variable de entorno HAY_TESTS, true/false
 */

import hudson.model.*
import cobertura.*


def build = Thread.currentThread().executable
def resolver = build.buildVariableResolver

def proyecto = new File("${build.workspace}")
def tecnologia = resolver.resolve("tecnologia")

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

def params = []
params.add (new StringParameterValue("HAY_TESTS", salida))
build.addAction(new ParametersAction(params))