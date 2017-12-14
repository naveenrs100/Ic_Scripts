package encoding;

import es.eci.utils.encoding.EncodingUtils;
import es.eci.utils.ParameterValidator
import es.eci.utils.StringUtil
import es.eci.utils.SystemPropertyBuilder
import static groovy.io.FileType.*
import static groovy.io.FileVisitResult.*

import java.nio.charset.Charset
import java.nio.file.Files

@Grab(group='com.ibm.icu', module='icu4j', version='57.1')

/**
 * Este job comprueba que los ficheros config.xml de definición de jobs
 * de Jenkins se encuentran en el encoding apropiado, informa de los que
 * no lo estén y los corrige.
 * 
 * Admite un parámetro dry_run true/false para no alterar los jobs, 
 * informando simplemente de los que no se encuentren en el encoding indicado.
 * 
 * El script se invoca como groovy script.
 * 
 * Parámetros:
 * 
 * jobsFolder: ruta absoluta desde la que buscar definiciones de jobs
 * 	de Jenkins.
 * encoding: [Opcional] Nombre del encoding contra el que chequear los jobs.  Por
 * 	defecto: UTF-8
 * dry_run: [Opcional] Indica si debe corregir los encodings de los ficheros.  Por 
 *	defecto: true
 */

SystemPropertyBuilder parameterBuilder = new SystemPropertyBuilder();
def params = parameterBuilder.getSystemParameters();

String jobsFolder = params['jobsFolder']

String encoding = "UTF-8"
if (StringUtil.notNull(params['encoding'])) {
	encoding = params['encoding']
}

Boolean dryRun = true;
if (StringUtil.notNull(params['dry_run'])) {
	dryRun = Boolean.valueOf(params['dry_run'])
}

ParameterValidator.builder().
	add("jobsFolder", jobsFolder, 
			{ File f = new File(it.toString()); 
				return f.exists() && f.isDirectory()} ).
	add("encoding", encoding, 
			{ Charset.forName(it.toString()) } ).
	add("dryRun", dryRun).build().validate();
	
if (dryRun) {
	println "=========================================="
	println "DRY RUN -> NO modifica ficheros"
	println "=========================================="
}

File dir = new File(jobsFolder);

def fixEncoding = { File f ->
	println "Revisando ${f}..."
	int confidence = EncodingUtils.matchFileEncoding(f, encoding);
	if (confidence == 0) {
		String fileEncoding = EncodingUtils.getEncodingName(f);
		if (!dryRun) {
			println "CAMBIANDO el encoding de $f"
			String fileContent = 
				new String(Files.readAllBytes(f.toPath()), 
						Charset.forName(fileEncoding));
			Files.write(f.toPath(), 
				fileContent.getBytes(Charset.forName(encoding)));
		}
		else {
			println "DRY RUN: se cambiaría el encoding de $f"
		}
	}
	else {
		println "Detectado ${encoding} al ${confidence}%"
	}
}

def configFileFilter = ~/config\.xml$/
dir.traverse type: FILES, visit: fixEncoding, nameFilter: configFileFilter
