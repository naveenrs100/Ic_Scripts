// detecta cambios del componente y genera ficheros de changeSet para Jenkins y el RTC en la carpeta
// $JENKINS_HOME/jobs/ScriptsCore/workspace/groovy/comun/rtc/setChangeSet.groovy
import hudson.scm.*
import hudson.scm.ChangeLogSet.*
import hudson.model.*
import com.deluan.jenkins.plugins.rtc.*
import com.deluan.jenkins.plugins.rtc.commands.*
import com.deluan.jenkins.plugins.rtc.commands.accept.*
import com.deluan.jenkins.plugins.rtc.changelog.*
import java.lang.reflect.Field
import java.lang.ref.WeakReference
import java.io.InputStreamReader
import java.io.FileInputStream
import java.io.BufferedReader

import components.*

def build = Thread.currentThread().executable
def resolver = build.buildVariableResolver
def invokerName = resolver.resolve("jobInvoker")
def invoker = Hudson.instance.getJob(invokerName)
def buildInvoker = null
if (invoker!=null)
	buildInvoker = invoker.getLastBuild()
else
	buildInvoker = build
	
File compareFile = new File("${build.workspace}/changesetCompare.txt")
File acceptFile = new File("${build.workspace}/changesetAccept.txt")

println "buildInvoker.getRootDir(): ${buildInvoker.getRootDir()}"

if (compareFile!=null && compareFile.exists()){
	def compareCmd = new CompareCommand(null)
	//Map changeSetCompare  = compareCmd.parse(new BufferedReader(new InputStreamReader(new FileInputStream(compareFile),System.getProperty("file.encoding"))))
	ComponentsParser parser = new ComponentsParser()
	parser.initLogger { println it }
	Map changeSetCompare  = parser.parseJazz(new BufferedReader(new InputStreamReader(new FileInputStream(compareFile),System.getProperty("file.encoding"))))

	Map changeSetAccept  = null
	if (acceptFile!=null && acceptFile.exists()){
		def acceptCmd = new AcceptNewOutputParser()
		changeSetAccept  = acceptCmd.parse(new BufferedReader(new InputStreamReader(new FileInputStream(acceptFile),System.getProperty("file.encoding"))))
	}else{
		println "WARNING: NO ENCUENTRA FICHERO ${acceptFile}"
	}

	List<JazzChangeSet> ret = new ArrayList<JazzChangeSet>()

	// detecta cambios en este componente y los mete en ret
	for (Map.Entry  entry : changeSetCompare.entrySet()) {
		JazzChangeSet chgset1 = ( JazzChangeSet ) entry.getValue()
		if (changeSetAccept != null && changeSetAccept.get(entry.getKey()) != null)  {
			JazzChangeSet chgset2 = ( JazzChangeSet ) changeSetAccept.get(entry.getKey())
			chgset1.copyItemsFrom(chgset2);
		}
		ret.add(chgset1)
	}
	
	if (!ret.isEmpty()) {
		JazzChangeLogWriter writer = new JazzChangeLogWriter()
		println "Escribe cambios: ${ret}"
		writer.write (ret , new OutputStreamWriter(new FileOutputStream("${buildInvoker.getRootDir()}/changelog.xml"), "UTF-8"))
	}

	// asigna el nuevo conjunto de cambios al build
	Field campo = AbstractBuild.getDeclaredField("changeSet")
	campo.setAccessible(true)
	campo.set(buildInvoker, new WeakReference<JazzChangeSetList>(new JazzChangeSetList(buildInvoker,ret)));
	println "buildInvoker: ${buildInvoker.getChangeSet()}"
}else{
	println "WARNING: NO ENCUENTRA FICHERO ${compareFile}"
}