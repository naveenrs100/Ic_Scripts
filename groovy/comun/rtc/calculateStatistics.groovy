package rtc

import java.util.regex.Matcher;
import java.util.regex.Pattern

import es.eci.utils.SystemPropertyBuilder
import static groovy.io.FileType.FILES


/**
 * Saca estadísticas de tamaño de ficheros dentro del directorio indicado.
 * 
 * 
 * Parámetros de entrada:
 *
 * --- OBLIGATORIOS
 * baseDir Directorio del proyecto
 * pattern Patrón de ficheros considerados como código fuente
 */

SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();

def parameters = propertyBuilder.getSystemParameters()

File baseDir = new File(parameters.get("baseDir"));
Pattern pattern = Pattern.compile(parameters.get("pattern"));

List<File> sourceFiles = []

baseDir.eachFileRecurse(FILES) {
	Matcher matcher = pattern.matcher(it.name);
    if(matcher.matches()) {
        sourceFiles << it
    }
}

Integer min = null;
Integer max = null;
List<Integer> sizes = [];

sourceFiles.each { File file ->
	int lines = file.readLines().size();
	if (min == null || min > lines) {
		min = lines;
	}
	if (max == null || max < lines) {
		max = lines;
	}
	sizes << lines;
}

// Media
Integer total = 0;
sizes.each { Integer size ->
	total += size;
}

def average = total / sizes.size();
println "Mínimo: $min ; Máximo: $max"
println "Media: $average"

