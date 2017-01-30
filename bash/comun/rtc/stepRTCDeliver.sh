#!/bin/bash


DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

# List of vars needed
VARS_NEEDED="SCMTOOLS_HOME userRTC pwdRTC urlRTC workspaceRTC component streamTarget"
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
NEW=""

existComp=$(${SCM_CMD} list components ${LOGIN} "${streamTarget}"|grep -i "${component}"|wc -l)
if [ $existComp -eq 0 ]; then
	echo "Adding component: \"${component}\""
	${SCM_CMD} workspace add-components ${LOGIN} "${streamTarget}" "${component}" -s "${workspaceRTC}"
	exitOnError $? "Adding componet to stream target"
	NEW="true"
fi

if [ "$force" = "true" ] && [ "${NEW}" = "" ];then
	${SCM_CMD} workspace replace-components -o ${LOGIN} "${streamTarget}" workspace "${workspaceRTC}" "${component}"
	exitOnError $? "Replacing componet to stream target"
elif [ "${NEW}" != "true" ];then
	${SCM_CMD} deliver ${LOGIN} -s "${workspaceRTC}" -t "${streamTarget}" --overwrite-uncommitted
	exitOnError $? "Delivering to stream target"
fi

if [[ $configDir ]] ; then
# Limpieza del directorio temporal de metadatos de RTC
echo "Limpiando el directorio temporal de metadatos ${configDir}..." 
rm -r $configDir
fi