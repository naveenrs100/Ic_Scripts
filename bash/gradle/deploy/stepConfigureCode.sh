#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

# List of vars needed
VARS_NEEDED="gradleDir MAVEN_REPOSITORY extension artifactId groupId version"
loadConfig $DIR

if [ "${reload}" = "true" ] || ! [ -d ${gradleDir} ]; then
	# Elimina el código fuente que pueda existir
	rm -rf ${gradleDir}
	mkdir -p ${gradleDir}
	cd ${gradleDir}

	# Bajo artefacto de código fuente
	grupo=$(echo $groupId |sed 's:\.:/:g')
	
	wget -O result.xml ${MAVEN_RESOLVE}?r=public\&g=${groupId}\&a=${artifactId}\&v=${version}\&e=${extension}
	exitOnError $? "resolving version artifact ${artifactId}-${version}.${extension}"
	repositoryPath=$(grep repositoryPath result.xml|awk -F">" '{print $2}'|awk -F"<" '{print $1}')
	
	wget -O "${artifactId}-${version}.${extension}" ${MAVEN_REPOSITORY}/${repositoryPath}
	exitOnError $? "Downloading artifact ${artifactId}-${version}.${extension}"
	
	# wget -q ${MAVEN_REPOSITORY}/${grupo}/${artifactId}/${version}/${artifactId}-${version}.${extension}

	# Descomprime y borra el zip
	unzip ${artifactId}-${version}.${extension}
	rm ${artifactId}-${version}.${extension}
fi