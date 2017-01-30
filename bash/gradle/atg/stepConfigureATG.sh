#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

# List of vars needed
VARS_NEEDED="GRADLE_HOME parentWorkspace ATG_CACHE_HOME SCMTOOLS_HOME"
loadConfig $DIR

configFile=${parentWorkspace}/.config_gradle_atg

rm -f ${configFile}

if [ "${gradleFile}" != "pom.xml" ];then
	echo "*** GRADLE"
	ATGVersion=$(grep atg.version "${gradleDir}/${gradleFile}"|sed $'s/\r//'|awk '{n=split($0,array,"=")} END{print array[n]}'|sed -e "s/'//g")
elif [ "${gradleFile}" == "pom.xml" ];then
	echo "*** MAVEN"
	ATGVersion=$(grep -oPm1 "(?<=<atg-version>)[^<]+" "${gradleFile}")
fi
	
echo "*** Version ATG: ${ATGVersion}"
echo "export ATG_HOME=\"${parentWorkspace}/ATG${ATGVersion}\"" >> ${configFile}
echo "export ATG_EAR=\"${parentWorkspace}/${gradleDir}/target\"" >> ${configFile}
echo "export DYNAMO_HOME=\"${parentWorkspace}/ATG${ATGVersion}/home\"" >> ${configFile}

if ! [ -d ATG${ATGVersion} ];then
	# Comprueba si está en la caché
	echo "*** Comprobando si la version ${ATGVersion} existe en la cache..."
	if ! [ -d $ATG_CACHE_HOME/ATG/ATG${ATGVersion} ];then
		echo "*** Creando cache nueva!"
		if ! [ -d $ATG_CACHE_HOME/ATG ];then
			mkdir $ATG_CACHE_HOME/ATG
		fi
		cd $ATG_CACHE_HOME/ATG
		# Download ATG installation
		echo "*** Descargando instalacion de ATG desde ${MAVEN_REPOSITORY}/${grupo}/${ArtifactId}/${ATGVersion}/${ArtifactId}-${ATGVersion}.${extension}..."
		grupo=$(echo $GroupId |sed 's:\.:/:g')
		wget -q ${MAVEN_REPOSITORY}/${grupo}/${ArtifactId}/${ATGVersion}/${ArtifactId}-${ATGVersion}.${extension}
		echo "*** Descomprimiendo..."
		unzip -uq ${ArtifactId}-${ATGVersion}.${extension}
		echo "*** Borrando temporales..."
		rm -f ${ArtifactId}-${ATGVersion}.${extension}
		echo "*** Hecho"
	fi
	echo "*** Copiando desde la cache $ATG_CACHE_HOME/ATG/ATG${ATGVersion} al workspace..."
	cp -r $ATG_CACHE_HOME/ATG/ATG${ATGVersion} "$parentWorkspace"
	echo "*** Fin de la copia"
fi
	
if [ -d "$parentWorkspace/ATG${ATGVersion}" ];then
	cd "$parentWorkspace/ATG${ATGVersion}"
	if [ "${gradleDir}" != "" ];then
		ln -sf "../${gradleDir}"
	else
		echo "No se creará link simbólico"
	fi
fi

