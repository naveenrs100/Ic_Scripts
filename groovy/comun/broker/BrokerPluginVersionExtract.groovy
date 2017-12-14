package broker

import groovy.util.XmlSlurper

// slaveWorkspace viene en el primer parámetro
def workspace = args[0]


def xmlSlurper = new XmlSlurper().parse(new File("${workspace}/pom.xml"))

def pluginList = xmlSlurper.build.plugins.children()

String pluginName = 'brokermojo-maven-plugin'
String pluginVersion = null

for (def item : pluginList) {
  if (item.artifactId.text() == pluginName) {
	println "Encontrado: ${item.artifactId.name()} : ${item.artifactId.text()} - versi\u00F3n: ${item.version.text()}"
	pluginVersion = item.version.text()
	break
  }
}

if (pluginVersion != null) {
  
	File tmp = new File("${workspace}/versionPlugin.txt")
	tmp.createNewFile()
	tmp.text = pluginVersion
}