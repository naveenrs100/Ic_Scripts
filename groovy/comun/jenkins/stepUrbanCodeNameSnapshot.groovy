/**
 * Este script se invoca al empezar un deploy, para poner un nombre "ficticio" a
 * la instantánea UrbanCode que se está creando
 */

import es.eci.utils.ParamsHelper;
import hudson.model.*

def build = Thread.currentThread().executable

// Nombre de la instantánea
def nombre = "INSTANTANEA_" + new java.util.Date().getTime()

ParamsHelper.addParams(build, ['instantanea':nombre])