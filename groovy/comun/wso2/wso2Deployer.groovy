package wso2

import es.eci.utils.NexusHelper
import es.eci.utils.StringUtil
import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.ZipHelper
import es.eci.utils.pom.MavenCoordinates

def validate(String s) {
	String ret = s;
	if (StringUtil.isNull(s) || s.startsWith('$')) {
		ret = ''
	}
	return ret;
}

def final packaging = 'zip'

def params = new SystemPropertyBuilder().getSystemParameters()

def workspace    = validate(params.get('parentWorkspace'))
def nexusUser    = validate(params.get('nexusUser'))
def nexusPass    = validate(params.get('nexusPass'))
def nexusUrl     = validate(params.get('nexusUrl'))
def groupId      = validate(params.get('groupId'))
def artifactId   = validate(params.get('artifactId'))
def version      = validate(params.get('builtVersion'))
def clasificador = validate(params.get('clasificador'))
def directorio   = validate(params.get('directorio'))

File carpeta = new File("${workspace}${File.separator}${directorio}")

if (carpeta.exists() && !version.contains('-SNAPSHOT')) {
  File zip = ZipHelper.addDirToArchive(carpeta)
  
  NexusHelper nexusHelper = new NexusHelper(nexusUrl)
  nexusHelper.initLogger { println it }
  nexusHelper.setNexus_user(nexusUser)
  nexusHelper.setNexus_pass(nexusPass)
  
  MavenCoordinates coord = new MavenCoordinates(groupId, artifactId, version)
  coord.setClassifier(clasificador)
  coord.setPackaging(packaging)
  coord.setRepository('eci')
  
  nexusHelper.upload(coord, zip)
  
  zip.delete()
} else {
	println 'No se ejecuta subida de artefacto api porque no existe el directorio o la versión acaba en -SNAPSHOT'
}