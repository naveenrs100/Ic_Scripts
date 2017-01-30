#!/bin/bash


DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

# List of vars needed
VARS_NEEDED="SCMTOOLS_HOME userRTC pwdRTC urlRTC description workItem parentWorkspace ignoreErrorsWithoutChanges workspaceRTC"
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

cd "${parentWorkspace}"
if [ -f "changed.txt" ];then
	while read -r line
	do
	    ${SCM_CMD} checkin "${line}" -u $userRTC -P $pwdRTC
		exitOnError $? "Checkin code"
	done < changed.txt
else
	${SCM_CMD} checkin . -u $userRTC -P $pwdRTC
	exitOnError $? "Checkin code"
fi
changeSet=$(${SCM_CMD} status -u $userRTC -P $pwdRTC -B -C| grep @|awk '{n=split($0,array,"(")} END{print array[n]}'| awk '{n=split($0,array,")")} END{print array[n-1]}')
if ! [ "${changeSet}" = "" ];then
	echo "El changeset es: ${changeSet}"
	${SCM_CMD} changeset comment ${changeSet} "${description}" ${LOGIN}
	exitOnError $? "Adding comment"
	${SCM_CMD} changeset associate ${LOGIN} ${changeSet} ${workItem}
	exitOnError $? "Associating workItem"
	#${SCM_CMD} changeset close ${changeSet} ${LOGIN}
	${SCM_CMD} set changeset --complete ${LOGIN} -w "${workspaceRTC}" ${changeSet}  
	exitOnError $? "Closing changeSet"
elif [ ${ignoreErrorsWithoutChanges} ]; then
	echo "NO SE HA MODIFICADO NADA, NO SE HA ENCONTRADO CHANGE SET. Aunque no haya cambios no se considera error."
else
	echo "NO SE HA MODIFICADO NADA, NO SE HA ENCONTRADO CHANGE SET"
	exit 1
fi

if [[ $configDir ]] ; then
# Limpieza del directorio temporal de metadatos de RTC
echo "Limpiando el directorio temporal de metadatos ${configDir}..." 
rm -r $configDir
fi