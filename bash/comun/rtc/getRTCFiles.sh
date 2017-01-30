# NO FUNCIONA PARA SCMTOOLS Version 3.x solo 4.x
#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

VARS_NEEDED="LOG_ERROR_SCMTOOL SCMTOOLS_HOME userRTC pwdRTC urlRTC stream component fileMatch targetDir parentWorkspace"
# List of vars needed
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

function downloadFile(){
	path=$1
	echo "downloading ${path} to ${targetDir}..."
	${SCM_CMD} get file -u ${userRTC} -P ${pwdRTC} -r ${urlRTC} -o ${path} "" ${targetDir}
	exitOnError $? "downloading file ${path}"
}

function getPaths(){
	${SCM_CMD} list remotefiles ${LOGIN} "${stream}" "${component}" / | grep ${fileMatch}
	exitOnError $? "Listing remotefiles of $component"
}

for path in `getPaths`
do
	downloadFile "${path}"
done

if [[ $configDir ]] ; then
# Limpieza del directorio temporal de metadatos de RTC
echo "Limpiando el directorio temporal de metadatos ${configDir}..." 
rm -r $configDir
fi