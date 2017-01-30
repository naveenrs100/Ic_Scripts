#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

# List of vars needed
VARS_NEEDED="copyDir parentWorkspace WORKSPACE fileName"
loadConfig $DIR

echo "parentWorkspace: ${parentWorkspace}"
echo "WORKSPACE: ${WORKSPACE}"
if [ "${copyDir}" = "" ]; then
	copyDir="."
fi

if [ "${clean}" = "" ]; then
	cd "${parentWorkspace}"
	if [ "${fromDir}" != "" ]; then
		for file in ${copyDir}
		do
			echo "buscando en $file"
			find "${fromDir}" -name "${file}" >> tmpList.txt
		done
		iList=$(cat tmpList.txt|wc -l)
		if [ ${iList} -gt 0 ]; then
			cat tmpList.txt|xargs zip -rq ${fileName}
		fi
		rm -rf tmpList.txt
	else
		zip -rq ${fileName} "${copyDir}"
	fi
	if [ -f "${fileName}" ];then
		mv ${fileName} "$WORKSPACE"
	fi
else
	rm -f ${fileName}
fi