#!/bin/bash
###################################
#### DOWNLOAD FILES FROM NEXUS ####
###################################

groupId=$1
artifactId=$2
classifier=$3
version=$4
extension=$5
ARTIFACTS_DIR=$6
MAVEN_REPOSITORY=$7
MAVEN_RESOLVE=$8

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

# List of vars needed
VARS_NEEDED="groupId artifactId version extension ARTIFACTS_DIR MAVEN_REPOSITORY MAVEN_RESOLVE"
loadConfig $DIR

mkdir -p "$ARTIFACTS_DIR"
cd "$ARTIFACTS_DIR"
if [ "${classifier}" != "" ]; then
	downloadVertx $groupId $artifactId $classifier $version $extension 2 -q
else
	download $groupId $artifactId $version $extension 2 -q
fi