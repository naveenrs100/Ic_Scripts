package jenkins.cli;

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

import es.eci.utils.ParameterValidator
import es.eci.utils.StringUtil
import es.eci.utils.SystemPropertyBuilder
import es.eci.utils.TmpDir
import es.eci.utils.commandline.CommandLineHelper
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

/**
 * Este script se sirve de jenkins-cli para:
 * + Promocionar determinados jobs de pre a pro
 * + Poblar pre con los jobs de producción indicados, pisando los que 
 * estuviesen ya creados.
 * 
 * El uso de este script presupone que se ha provisionado en la máquina:
 * + node.js
 * + jenkins-cli
 * 
 * Parámetros:
 * 
 * jobs -> Obligatorio.  Lista de nombres de jobs a importar/exportar,
 * 	separados por salto de línea.  Viene de un parámetro de caja de
 *  texto en jenkins.
 * source -> Obligatorio - Ruta completa del fichero .json con la 
 * 	configuración del jenkins de origen.
 * target -> Obligatorio - Ruta completa del fichero .json con la
 * 	configuración del jenkins de destino.
 * encoding -> Opcional; por defecto, el que venga en el fichero 
 *  de configuración de destino.  Si viene indicado, se 
 * 	utiliza ese encoding durante esta operación, sin modificar el 
 *  fichero de destino.
 */

String jobs = null;
String source = null;
String target = null;
String encoding = null;

// Lee los parámetros indicados con -Dparam.*
new SystemPropertyBuilder().getSystemParameters().with { Map params ->
	source = params['source']
	target = params['target']
	jobs = params['jobs']
	if (StringUtil.notNull(params['encoding'])) {
		encoding = params['encoding']
	}
}

// Validar parámetros obligatorios
ParameterValidator.builder().
	add("source", source, { StringUtil.notNull(it) && new File(it).exists()}).
	add("target", target, { StringUtil.notNull(it) && new File(it).exists()}).
	build().validate();
	
// Validar el encoding
if (StringUtil.notNull(encoding)) {
	Charset.forName(encoding);
}

// Se copian los ficheros de configuración a un temporal en previsión
//	de que haya que manipularlos para el encoding
TmpDir.tmp { File dir ->
	// Repositorio de jobs
	File pipeline = new File(dir, "pipeline");
	pipeline.mkdirs();
	// Copia de los ficheros de configuración 
	File sourceConfigFile = new File(dir, "source.json");
	File targetConfigFile = new File(dir, "target.json");
	Files.copy(Paths.get(source), sourceConfigFile.toPath());
	Files.copy(Paths.get(target), targetConfigFile.toPath());
	// Fichero de jobs
	File jobsFile = new File(dir, 'jobs');
	jobsFile.createNewFile();
	Files.write(jobsFile.toPath(), 
		Arrays.asList(jobs.split(/[\r\n]+/)), 
		Charset.forName("UTF-8"));
	// Actualizar el directorio del fichero de configuración
	//	de origen
	def sourceObject = new JsonSlurper().parseText(sourceConfigFile.text);
	sourceObject.pipelineDirectory = pipeline.getCanonicalPath();
	sourceConfigFile.text =  JsonOutput.toJson(sourceObject);
	// Actualizarlo sobre el fichero 
	//	de configuración de destino	
	def targetObject = new JsonSlurper().parseText(targetConfigFile.text);
	if (StringUtil.notNull(encoding)) {
		targetObject.encoding = encoding;
	}
	targetObject.pipelineDirectory = pipeline.getCanonicalPath();
	targetConfigFile.text = JsonOutput.toJson(targetObject);
	println "Jobs:"
	println jobsFile.text
	println "Source:"
	println sourceConfigFile.text
	println "Target:"
	println targetConfigFile.text
	
	// Llevar a cabo el cambio
	String backupCommand = 
		"jenkins-cli -c source.json -f ${jobsFile.canonicalPath} backup -j"; 
	String installCommand = 
		"jenkins-cli -c target.json -f ${jobsFile.canonicalPath} install -j";
	CommandLineHelper backupHelper = new CommandLineHelper(backupCommand);
	backupHelper.initLogger { println it }	
	backupHelper.execute(dir);	
	CommandLineHelper installHelper = new CommandLineHelper(installCommand);
	installHelper.initLogger { println it }	
	installHelper.execute(dir);
	
	
}


