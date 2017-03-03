import hudson.model.*
import jenkins.model.*

import components.ComponentsParser
import components.RTCComponent

import es.eci.utils.ParamsHelper

/**
 * Se ejecuta desde el job de la corriente (el principal).
 * Este script parsea el componentsCompare.txt para determinar qué componente contiene el 
 * fichero XML descriptor.
 * SALIDA DEL SCRIPT: variable de entorno ${environmentComponent}
 */
 
def build = Thread.currentThread().executable


def workspace = build.getEnvironment().get("WORKSPACE")
def comps = new File(workspace + "/componentsCompare.txt");

def nombreComponente = null;

ComponentsParser parser = new ComponentsParser()
parser.initLogger { println it }

def listaComps = parser.parse(comps)

// Este script debe identificar el componente xxx-environmentCatalogoxxx

List<RTCComponent> compEnv = []

listaComps.each { componente ->
	if (componente.nombre.contains("environmentCatalogo")) {
		compEnv << componente
	}
}


// Environment

if (compEnv.size() != 1) {
	println "Debe existir un solo componente con nombre terminado en environment"
	throw new Exception("Debe existir un solo componente con nombre terminado en environment")
}

nombreComponente = compEnv[0].nombre;

println "Nombre del componente environment -> $nombreComponente" 

def params = [:]
params['environmentComponent'] = nombreComponente

println "Param environmentComponent: ${params['environmentComponent']}"

ParamsHelper.addParams(build, params)

