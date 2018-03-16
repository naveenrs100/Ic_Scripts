package portal

@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')
@Grab(group='org.apache.httpcomponents', module='httpmime', version='4.4')

//imports

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.*
import static groovyx.net.http.ContentType.*
import groovy.json.*

import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody

import es.eci.utils.QuveHelper

// Parametros de entrada

workspace = args[0]
jobType = args[1]
quveurl = args[2]
jenkinsHome = args[4]
jsonTxt = "{"+
  "\"executionUuid\": \"${args[3]}\","+
    "\"result\": {"+
        "\"duration\": 1000,"+
        "\"version\": null,"+
        "\"baseline\": null,"+
        "\"log\": \"Trabajo finalizado\","+
        "\"finalStatus\": \"NOT_BUILT\""+
    "},"+
    "\"description\": \"Parada a mano\""+
"}";

//Variables calculadas

jobFilesName = "jobFiles.zip"
nameJson = "portal.json"
ant = new AntBuilder()

def zipJobFiles(jobFiles){
  def file = new File("dummy")
  file << ""
  ant.zip(
    destfile: jobFiles,
    basedir: workspace,
    level: 9,
    encoding: "UTF-8",
    excludes: "*.groovy"
  )
}

//-------------------------


def jobFiles = new File(jobFilesName)

jsonInfo = zipJobFiles(jobFiles)

MultipartEntity entity = new MultipartEntity()
entity.addPart("file", new FileBody(jobFiles))
entity.addPart("updateCommand",new StringBody(jsonTxt))

QuveHelper helper = new QuveHelper(jenkinsHome, quveurl)
helper.initLogger { println it }
helper.sendQuve(quveurl, "executions/${jobType}", entity, "multipart/form-data")
