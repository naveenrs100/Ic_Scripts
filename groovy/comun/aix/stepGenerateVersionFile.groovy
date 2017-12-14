package aix

import es.eci.utils.ComponentVersionHelper

def stream = build.buildVariableResolver.resolve("stream")
def component = build.buildVariableResolver.resolve("component")
def workspace = build.getEnvironment().get("WORKSPACE")
def urlRTC = build.getEnvironment().get("urlRTC")
def userRTC= build.getEnvironment().get("userRTC") 
def pwdRTC = build.buildVariableResolver.resolve("pwdRTC") 

println('Using stream: ' + stream)
println('Using component: ' +component)
println('Using workspace: ' + workspace)
println('Using urlRTC: ' + urlRTC)
println('Using userRTC: ' + userRTC)
println('Using pwdRTC: ' + pwdRTC)

ComponentVersionHelper componentVersionHelper = new ComponentVersionHelper()
String version = componentVersionHelper.getVersion(component, stream, userRTC, pwdRTC, urlRTC)
println('Current version:' + version)

String newVersion=version.replace('-SNAPSHOT','')
def versionFile = new File(workspace + '/' + component + '/version.txt')
versionFile.write('groupId=es.eci.elcorteingles.cccc.tpv \n' + 'version=' + newVersion)


