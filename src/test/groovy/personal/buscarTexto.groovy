import es.eci.utils.versioner.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

File dir = new File("C:/Users/dcastro.jimenez/Desktop/Jobs a Modificar/JOBS_INT");

String texto = "0005 - Registro - Unico";

dir.eachFileRecurse { File file ->
	if(file.getName() == "config.xml") {
		if(file.getText().contains("DESARROLLO")) {
			println(file.getCanonicalPath());
		}
	}	
}

