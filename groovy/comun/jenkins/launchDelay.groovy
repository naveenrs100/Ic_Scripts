/**
 * Script que lanza una ejecución diferida de un proceso en Jenkins
 */

package jenkins

import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.jenkins.JobDelayExecutor

Map params = new SystemPropertyBuilder().getSystemParameters()

def url       = params['url']
def delay     = params['timeDelay']
def user      = params['user']
def password  = params['password']

params.remove('url')
params.remove('timeDelay')
params.remove('user')
params.remove('password')

println ''
println '------------ Parámetros de entrada al script ------------'
println "url: ${url}"
println "delay: ${delay}"
println "user: ${user}"
println "Parámetros del job:"

params.keySet().each { key ->
	println "$key -> ${params[key]}"
}

JobDelayExecutor jde = new JobDelayExecutor(url, delay, user, password, params)
jde.sentRequest()
