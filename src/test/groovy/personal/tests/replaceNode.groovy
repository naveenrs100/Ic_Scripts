import hudson.model.*
import java.util.regex.*
import groovy.xml.*
import groovy.util.Node
import java.io.*
import java.nio.charset.Charset

def configFile = new File("C:/Users/dcastro.jimenez/Desktop/xml/config.xml");

def writeConfig(text, jobName, workspace){
	println "cambiando ${jobName}..."
	new File("${workspace}/${jobName}").mkdirs()
	def destFile = new File("${workspace}/${jobName}/config.xml")
	destFile.delete()
	//destFile << text
	Writer writer = new OutputStreamWriter(new FileOutputStream(destFile), Charset.forName("UTF-8"))
	writer.write(text, 0, text.length())
	writer.flush()
  }


def pattern = '''<hudson.plugins.ws__cleanup.Pattern>
					<pattern>*lastCommit.txt</pattern>
					<type>EXCLUDE</type>
				</hudson.plugins.ws__cleanup.Pattern>'''	

 def xml = new XmlSlurper().parseText(configFile.getText("UTF-8"));
 def patternXml = 	 new XmlSlurper().parseText(pattern);
 
 xml.publishers['hudson.plugins.ws__cleanup.WsCleanup'][0].patterns[0].appendNode(patternXml)
 
 writeConfig(XmlUtil.serialize(xml), "JOB_PRUEBA", "C:/Users/dcastro.jimenez/Desktop/xml");
 
 