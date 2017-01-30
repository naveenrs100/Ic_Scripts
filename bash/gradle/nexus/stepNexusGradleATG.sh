#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

# List of vars needed
VARS_NEEDED="gradleDir parentWorkspace GRADLE_HOME"
loadConfig $DIR

# Configura el entorno ATG gradle
if [ -f .config_gradle_atg ];then
	. ${parentWorkspace}/.config_gradle_atg
fi

cd "${parentWorkspace}"
# Solo la primera ocurrencia de la palabra version
VERSION_LOCAL=$(grep version ${gradleDir}/build.gradle|awk -F\" '{print $(NF-1); exit}')
echo "VERSION_LOCAL: ${VERSION_LOCAL} snapshot: ${snapshot}"

cd "${gradleDir}"
if [[ "${VERSION_LOCAL}" == *SNAPSHOT* ]] && [ "${snapshot}" = "true" ];then
	echo "Uploading snapshot version to nexus..."
elif [[ "${VERSION_LOCAL}" != *SNAPSHOT* ]] && [ "${snapshot}" != "true" ];then
	echo "Uploading release version to nexus..."
else
	echo "Impossible to upload version: ${VERSION_LOCAL} with snapshot parameter: ${snapshot}"
	exit -1
fi
$GRADLE_HOME/bin/gradle -i -x test uploadArchives -Penv=CI
exitOnError $? "uploading archives to nexus..."