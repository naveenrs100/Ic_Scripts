#!/bin/bash


DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

# List of vars needed
VARS_NEEDED="SCMTOOLS_HOME userRTC pwdRTC urlRTC stream tagType"
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

# Trata de recuperar la versión
if [ ! -z "$instantanea" -a "${tagType}" = "snapshot" ]; then
# Si viene informado el parámetro de instantánea y además se está haciendo la
# instantánea y NO una línea base, se usa este parámetro
	VERSION_LOCAL=$instantanea
elif [ -f "version.txt" ]; then
	VERSION_LOCAL=$(grep version= version.txt|awk -F= '{print $(NF)}'|tr -d '"')
else
	echo "No existe la variable INSTANTANEA ni tampoco el fichero version.txt"
	exit 1
fi

if [ "$description" = "" ];then
	description="JENKINS BASELINE"
fi
if [ "$version" = "" ];then
	fecha=`date '+%Y%m%d-%H:%M'`
	version="${VERSION_LOCAL}-build:${compJobNumber}"
elif [ "$version" = "local" ];then
	version=${VERSION_LOCAL}
fi

versionTxt=${version}
if [ "${streamInVersion}" == "true" ];then
	versionTxt="${stream} - ${version}"
fi
if [ "${tagType}" = "baseline" ]; then
	if [ "${makeSnapshot}" != "false" ]; then
		echo "baseline: ${versionTxt}"
		${SCM_CMD} create baseline ${LOGIN} "${workspaceRTC}" "${versionTxt}" "${component}" --overwrite-uncommitted
		exitOnError $? "Creating baseline"
	else
		echo "NO CIERRA baseline!!"
	fi
else
	${SCM_CMD} create snapshot ${LOGIN} "${stream}" -n "${versionTxt}" -d "${description}"
	exitOnError $? "Creating snapshot"
fi

if [ -f "${RTCVersionFile}" ]; then
    OLDVersion=$(cat ${RTCVersionFile}|grep RTCBaseline)
	echo "La version antigua es: ${OLDVersion} la nueva es: ${version}"
	if [ "${OLDVersion}" != "" ]; then
		eval "sed 's/${OLDVersion}/RTCBaseline=${version}/g' ${RTCVersionFile} > tmpFile.txt"
		mv tmpFile.txt ${RTCVersionFile}
	else
		echo "RTCBaseline=${version}" >> ${RTCVersionFile}
	fi
fi

if [[ $configDir ]] ; then
# Limpieza del directorio temporal de metadatos de RTC
echo "Limpiando el directorio temporal de metadatos ${configDir}..." 
rm -r $configDir
fi
