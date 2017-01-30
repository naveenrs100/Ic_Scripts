#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../functionsJenkins.sh

artifactId=$1
classifier=$2
version=$3
extension=$4
storeDir=$5
deployDir=$6
vertxScriptsDir=$7

# List of vars needed
VARS_NEEDED="artifactId classifier version extension storeDir deployDir vertxScriptsDir"
loadConfig $DIR

BACK_DIR="${storeDir}/back/${artifactId}"
FILE="${BACK_DIR}/${version}/${artifactId}.${classifier}.${extension}"

bash $vertxScriptsDir/rc.downMUY
exitOnError $? "Stopping vertx..."

if [ -d "${deployDir}" ];then
	rm -rf "${deployDir}"
	mkdir -p "${deployDir}"
else
	mkdir -p "${deployDir}"
fi
cd "${deployDir}"
unzip -oq ${FILE}
exitOnError $? "Unzipping artifact ${FILE}"

bash $vertxScriptsDir/rc.iplMUY
exitOnError $? "Starting vertx"