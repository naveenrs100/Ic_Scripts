#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

# List of vars needed
VARS_NEEDED="gradleDir parentWorkspace"
loadConfig $DIR

cd "${parentWorkspace}/${gradleDir}"
echo "
eciMavenRepositoryPublicUrl=${mavenPublic}" >> gradle.properties
echo "eciMavenRepositoryReleasesUrl=${mavenRelease}" >> gradle.properties
echo "eciMavenRepositorySnapshotsUrl=${mavenSnapshot}" >> gradle.properties