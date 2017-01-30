#!/bin/bash


DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

VARS_NEEDED="LOG_ERROR_SCMTOOL SCMTOOLS_HOME userRTC pwdRTC urlRTC onlyChanges fileOut nameTarget"
# List of vars needed
if [ "${onlyChanges}" = "true" ]; then
	VARS_NEEDED="${VARS_NEEDED} typeOrigin nameOrigin typeTarget"
fi
loadConfig $DIR

# Variables del script
if [ "$light" == "true" ] ; then
setRTCTimeout
daemonConfigDir=$(getRTCDaemonDir)
echo "Utilizando el directorio de metadatos de daemon ${daemonConfigDir}..." 
SCM_CMD="$SCMTOOLS_HOME/lscm --config ${daemonConfigDir} "
else
# Directorio temporal de metadatos de RTC
configDir=$(mktempDir)
echo "Utilizando el directorio temporal de metadatos ${configDir}..." 
SCM_CMD="$SCMTOOLS_HOME/scm.sh --config $configDir"
fi
LOGIN="-u ${userRTC} -P ${pwdRTC} -r ${urlRTC}"

function getTecnologyJson(){
	stream=$1
	component=$2
	${SCM_CMD} list remotefiles -u ${userRTC} -P ${pwdRTC} -r ${urlRTC} "${stream}" "${component}" / > components.txt
	exitOnError $? "Listing remotefiles of $component"
	MAVEN=$(cat components.txt|grep pom.xml|wc -l)
	GRADLE=$(cat components.txt|grep build.gradle|wc -l)
	rm -f components.txt
	echo "{\"component\": \"$component\", \"maven\": \"$MAVEN\", \"gradle\": \"$GRADLE\"},"
}

function getComponents(){
	echo "Obteniendo componentes de \"${1}\" "
	echo "${SCM_CMD} list components \"${1}\" ${LOGIN}"
	${SCM_CMD} list components "$1" -u ${userRTC} -P ${pwdRTC} -r ${urlRTC} > ${2}
	exitOnError $? "Listing components"
}

if [ "${onlyChanges}" = "true" ]; then
	echo "Comparando cambios de ${typeOrigin} \"${nameOrigin}\" frente a ${typeTarget} \"${nameTarget}\""
	retry 3 "${SCM_CMD} compare ${typeOrigin} \"${nameOrigin}\" ${typeTarget} \"${nameTarget}\" ${LOGIN} -f i -I dcbsw -C \"|{name}|{email}|\" -D \"|yyyy-MM-dd-HH:mm:ss|\"" AliasLoader.parseLine $LOG_ERROR_SCMTOOL/scmtoolerror.log ${fileOut}
	cat ${fileOut}|grep -v ${userRTC} > ${fileOut}_tmp
	mv ${fileOut}_tmp ${fileOut}
elif [ "${streamFile}" != "" ]; then
	if [ -f "${fileOut}" ]; then
		rm -f "${fileOut}"
	fi
	SAVEIFS=$IFS
	IFS=$'\n'
	echo "[" >> "${fileOut}"
	for stream in `cat $streamFile`
	do
		getComponents "${stream}" tmp.txt
		echo "{\"name\": \"$stream\",\"components\": [" >> "${fileOut}"
		for component in `cat tmp.txt|grep Component:|awk -F'"' '{print $2}'`
		do
			echo "getTecnologyJson of \"${component}\" in \"${stream}\""
			json=$(getTecnologyJson "${stream}" "${component}")
			echo $json >> "${fileOut}"
		done
		echo "]}," >> "${fileOut}"
		rm -f tmp.txt
	done
	IFS=${SAVEIFS}
	echo "]" >> "${fileOut}"
else
	getComponents "${nameTarget}" "${fileOut}"
fi

if [[ $configDir ]] ; then
# Limpieza del directorio temporal de metadatos de RTC
echo "Limpiando el directorio temporal de metadatos ${configDir}..." 
rm -r $configDir
fi