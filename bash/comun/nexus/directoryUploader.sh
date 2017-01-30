#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

# List of vars needed
VARS_NEEDED="extension parentWorkspace directory repositoryId repositoryUrl JENKINS_HOME MAVEN_HOME JAVA_HOME DEPLOYMENT_USER DEPLOYMENT_PWD"
loadConfig $DIR

cd "${parentWorkspace}"
if [ -f "version.txt" ]; then
	VERSION_LOCAL=$(grep version= version.txt|awk -F= '{print $(NF)}'|tr -d '"')
	GROUPID_LOCAL=$(grep groupId= version.txt|awk -F= '{print $(NF)}'|tr -d '"')
	ARTIFACTID_LOCAL=$(grep artifactId= version.txt|awk -F= '{print $(NF)}'|tr -d '"')
fi

if [ "${VERSION_LOCAL}" != "" ]; then
	version="${VERSION_LOCAL}"
fi
if [ "${GROUPID_LOCAL}" != "" ]; then
	groupId="${GROUPID_LOCAL}"
fi
if [ "${ARTIFACTID_LOCAL}" != "" ]; then
	artifactId="${ARTIFACTID_LOCAL}"
fi

if [ "${version}" != "" ] && [ "${groupId}" != "" ] && [ "${artifactId}" != "" ]; then


	FILE="${artifactId}-${version}.${extension}"

	if [ "${extension}" = "zip" ]; then
		zip -rq ${FILE} "${directory}"
	elif [ "${extension}" = "jar" ]; then
		$JAVA_HOME/bin/jar cvf ${FILE} "${directory}"
	else
		echo "Extensión ${extension} no implementada - extensiones aceptadas: zip/jar"
		exit -1
	fi

	# Sube a nexus
	${MAVEN_HOME}/bin/mvn -q deploy:deploy-file -DuniqueVersion=false -Durl=${repositoryUrl} -DrepositoryId=${repositoryId} -DgroupId=${groupId} -DartifactId=${artifactId} -Dversion=${version} -Dpackaging=${extension} -Dfile=${FILE}
	exitOnError $? "Uploading ${artifactId} ${version} to ${repositoryId} (${repositoryUrl})"
	rm -f ${FILE}
else
	echo "version: ${version}, groupId: ${groupId}, artifactId: ${artifactId} - No se ha definido coordenadas maven para subir el artefacto!!"
	exit -1
fi
