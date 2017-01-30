@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.4')

import ldap.LDAPClient
import ldap.LDAPRecord
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

// Conexión a gitlab
GitlabHelper gitlabHelper = new GitlabHelper(
	new GitlabClient(
		params["urlGitlab"],
		params["privateGitlabToken"],
		params["keystoreVersion"],
		params["nexusURL"]))
gitlabHelper.initLogger{ println it };

// Conexión a LDAP
LDAPClient ldapClient = 
	new LDAPClient(
		params["ldapURL"], 
		params["ldapUser"], 
		params["ldapPassword"]);
ldapClient.initLogger { println it }

// Obtener ambas listas
List<LDAPRecord> noMail = []
List<GitlabUser> differentMail = []
List<GitlabUser> noLDAP = []
List<GitlabUser> allUsers = []

// Comparar las listas
gitlabHelper.getAllUsers().each { GitlabUser user ->
	allUsers << user
	LDAPRecord record = ldapClient.getByUserName(user.getUserName());
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
		// Algunos usuarios no tienen correspondencia en LDAP (p. ej. ecici)
		noLDAP << user;
	}
}

// Resumen final

println(''); 
println('');

println "======================================================================"
println "${allUsers.size()} usuario(s) activos en gitlab"
println "======================================================================"
allUsers.each {
	println it.getUserName()
}

println(''); 
println('');

// Informar del resultado
if (noLDAP.size() > 0) {
	println "======================================================================"
	println "${noLDAP.size()} usuario(s) de gitlab no existen en LDAP"
	println "======================================================================"
	noLDAP.each {
		println it.getUserName();
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
		println it.getUserId();
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
