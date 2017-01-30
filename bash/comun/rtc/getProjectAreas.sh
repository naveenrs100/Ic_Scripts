#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

VARS_NEEDED="SCMTOOLS_HOME userRTC pwdRTC urlRTC"
loadConfig $DIR

# Variables del script

# Directorio temporal de metadatos de RTC

configDir=$(mktempDir)
echo "Utilizando el directorio temporal de metadatos ${configDir}..." 
SCM_CMD="$SCMTOOLS_HOME/lscm --config $configDir"
LOGIN="-u ${userRTC} -P ${pwdRTC} -r ${urlRTC}"

${SCM_CMD} list projectareas -j -v ${LOGIN} > all.json
PAS=$(${SCM_CMD} list projectareas -j ${LOGIN}|grep uuid|awk -F"\"" '{print $4}')

for uuid in ${PAS}
do
	${SCM_CMD} list streams -j -m 10000 --projectarea ${uuid} -v ${LOGIN} > ${uuid}
done 

# Asegurarse de detener el daemon RTC
$SCMTOOLS_HOME/scm.sh --config $configDir daemon stop -a

if [ -d "$configDir" ] ; then
	# Limpieza del directorio temporal de metadatos de RTC
	echo "Limpiando el directorio temporal de metadatos ${configDir}..."
	rm -rf $configDir
fi