package git

@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.4')

import ldap.LDAPClient
import ldap.LDAPRecord
import es.eci.utils.Stopwatch;
import es.eci.utils.SystemPropertyBuilder
import git.GitlabClient
import git.GitlabHelper
import git.beans.GitlabUser

/**
 * Compara la lista de usuarios de gitlab con los disponibles en LDAP,
 * indicando:
 * 
 * + Usuarios de gitlab que no tienen correo en LDAP
 * + Usuarios de gitlab con correo en LDAP pero son distintos
 * 
 * Parámetros de entrada
 * 
 * urlGitlab URL para conexión a gitlab
 * privateGitlabToken Token de autenticación para gitlab
 * 
 * ldapURL URL para conexión a ldap
 * ldapUser Usuario para LDAP
 * ldapPassword Password para LDAP
 */
 
SystemPropertyBuilder propertyBuilder = new SystemPropertyBuilder();
Map<String, String> params = propertyBuilder.getSystemParameters();

// Cache para presentación posterior del informe
Map<String, LDAPRecord> ldapCache = [:]
Map<String, GitlabUser> gitlabCache = [:]

// Conexión a gitlab
GitlabClient gitClient = new GitlabClient(
		params["urlGitlab"],
		params["privateGitlabToken"],
		params["keystoreVersion"],
		params["nexusURL"]);
gitClient.initLogger { println it }
GitlabHelper gitlabHelper = new GitlabHelper(gitClient);

// Usuarios administradores de gitlab, mantenidos como usuarios locales de gitlab
List<String> userExceptions = []
if (params["gitUserExceptions"] != null) {
	userExceptions = Arrays.asList(params["gitUserExceptions"].split(","));
} 

// Conexión a LDAP
LDAPClient ldapClient = 
	new LDAPClient(
		params["ldapURL"], 
		params["ldapUser"], 
		params["ldapPassword"]);

// Obtener ambas listas
List<LDAPRecord> noMail = []
List<GitlabUser> differentMail = []
List<GitlabUser> noLDAP = []
List<GitlabUser> allUsers = []

long millis = Stopwatch.watch {
	println "Generando informe..."

	// Comparar las listas
	gitlabHelper.getAllUsers().each { GitlabUser user ->
		gitlabCache[user.getUserName()] = user;
		allUsers << user
		LDAPRecord record = ldapClient.getByUserName(user.getUserName().toUpperCase());
		if (record != null) {
			ldapCache.put(user.getUserName(), record);
			String email = record.getMail();
			if (email == null || email.trim().length() == 0) {
				noMail << record;
			}
			else {
				if (!email.toLowerCase().equals(user.getEmail().toLowerCase())) {
					differentMail << user;
				}
			}
		}
		else {
			if (!userExceptions.contains(user.getUserName())) {
				// Algunos usuarios no tienen correspondencia en LDAP (p. ej. ecici)
				noLDAP << user;
			}
		}
	}
}

println "Informe generado en ${millis} mseg."

// Resumen final

println(''); 
println('');

println "======================================================================"
println "${allUsers.size()} usuario(s) activos en gitlab"
println "======================================================================"
allUsers.each {
	println it.getUserName() + " - " + it.getUserDisplayName()
}

println(''); 
println('');

// Informar del resultado
if (noLDAP.size() > 0) {
	println "======================================================================"
	println "${noLDAP.size()} usuario(s) de gitlab no existen en LDAP"
	println "======================================================================"
	noLDAP.each {
		println it.getUserName() + " - correo gitlab: " + it.getEmail();
	}
}
else {
	println "======================================================================"
	println "Todos los usuarios de gitlab tienen correspondencia en LDAP"
	println "======================================================================"
}

println(''); 
println('');

if (noMail.size() > 0) {
	println "======================================================================"
	println "${noMail.size()} usuario(s) de LDAP sin correo"
	println "======================================================================"
	noMail.each {
		String line = it.getUserId() + " - " + it.getDisplayName();
		// Correo del usuario en gitlab
		if (gitlabCache.containsKey(it.getUserId())) {
			line += (" - " + gitlabCache[it.getUserId()].getEmail())
		}
		println line;
	}
}
else {
	println "======================================================================"
	println "Todos los usuarios consultados en LDAP tienen correo"
	println "======================================================================"
}

println(''); 
println('');

if (differentMail.size() > 0) { 
	println "======================================================================"
	println "${differentMail.size()} usuario(s) de gitlab con el correo mal"
	println "======================================================================"
	differentMail.each {
		println it.getUserName() + " gitlab: " + it.getEmail() + 
			" LDAP: " + ldapCache[it.getUserName()].getMail();
	}
}
else {
	println "======================================================================"
	println "Todos los usuarios consultados en gitlab tienen el correo correcto"
	println "======================================================================"
}
println(''); 
println('');

// Forzar el lanzamiento del informe solo cuando:
//	+ Hay usuarios de LDAP sin correo
//	+ Algún usuario de gitlab no existe en LDAP
//	+ Algún usuario de gitlab tiene el correo mal
if (noLDAP.size() > 0 || noMail.size() > 0 || differentMail.size() > 0) {
	println("======================================================================")
	println("RESULTADO: ERRORES ENCONTRADOS");
	println("RESUMEN DE INCONSISTENCIAS ENTRE LDAP Y GITLAB");
	println("======================================================================")
	if (noLDAP.size() > 0) {
		println("Encontrados ${noLDAP.size()} usuarios que no existen en LDAP")
	}	
	if (noMail.size() > 0) {
		println("Encontrados ${noMail.size()} usuarios que no tienen mail en LDAP")
	}
	if (differentMail.size() > 0) {
		println("Encontrados ${differentMail.size()} usuarios con mail distinto en gitlab y en LDAP")
	}
	System.out.flush();
	throw new Exception()
}
else {
	println("======================================================================")
	println("RESULTADO: CORRECTO");
	println("======================================================================")
}