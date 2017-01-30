#!/bin/bash


DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

# List of vars needed
VARS_NEEDED="LOG_ERROR_SCMTOOL SCMTOOLS_HOME userRTC pwdRTC urlRTC component typeOrigin nameOrigin typeTarget nameTarget"
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
STOP=$1

function hayCambio(){
	echo "Comparando componente \"${component}\" desde ${typeOrigin} \"${nameOrigin}\" hasta ${typeTarget} \"${nameTarget}\""	
	retry 3 "${SCM_CMD} compare ${typeOrigin} \"${nameOrigin}\" ${typeTarget} \"${nameTarget}\" -c \"${component}\" ${LOGIN} -f i -I swf -C \"|{name}|{email}|\" -D \"|yyyy-MM-dd-HH:mm:ss|\"" AliasLoader.parseLine $LOG_ERROR_SCMTOOL/scmtoolerror.log changesetCompare.txt	
}

hayCambio
RES=$(cat changesetCompare.txt|wc -l)
if [ "${STOP}" != "false" ] && [ ${RES} -eq 0 ]; then
	echo "No hay cambios"
	exit 1
else
	exit 0
fi

if [[ $configDir ]] ; then
# Limpieza del directorio temporal de metadatos de RTC
echo "Limpiando el directorio temporal de metadatos ${configDir}..." 
rm -r $configDir
fi