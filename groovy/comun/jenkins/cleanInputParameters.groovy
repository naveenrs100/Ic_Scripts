package jenkins

// Este script limpia un juego de variables del build actual, indicadas en
//	paramsToClean separadas por comas

import hudson.model.*
import jenkins.model.*
import java.beans.XMLDecoder;
import es.eci.utils.ParamsHelper

def paramsToClean = build.buildVariableResolver.resolve("paramsToClean").split(",")

// Iterate over every param to 
//clean and set it to void.
if(paramsToClean != null) {
	ParamsHelper.deleteParams(build, paramsToClean)
}
