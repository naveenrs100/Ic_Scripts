#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../functionsJenkins.sh

artifactId=$1
classifier=$2
version=$3
extension=$4
storeDir=$5
deployDir=$6
launchScript=$7

# List of vars needed
VARS_NEEDED="artifactId version extension storeDir deployDir launchScript"
loadConfig $DIR

BACK_DIR="${storeDir}/back/${artifactId}"
FILE="${BACK_DIR}/${version}/${artifactId}.${extension}"

# DigitalProductImporter
if [ -f $deployDir/$launchScript ];then
bash $deployDir/$launchScript stop
exitOnError $? "Stopping ${script} ..."
fi

# Unzip the deliverable
if [ ! -d "${deployDir}" ];then
	mkdir -p "${deployDir}"
fi
cd "${deployDir}"
cp ${FILE} "${deployDir}"
unzip -oq ${FILE} launcher/$launchScript 
mv ${deployDir}/launcher/$launchScript ${deployDir}
rm -rf ${deployDir}/launcher

chmod 755 $deployDir/$launchScript
bash $deployDir/$launchScript start
