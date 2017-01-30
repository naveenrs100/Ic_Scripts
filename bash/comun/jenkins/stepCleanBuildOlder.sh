#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

# List of vars needed
VARS_NEEDED="dias JENKINS_HOME"
loadConfig $DIR

# NO FUNCIONA FIND -EMPTY
function borraDirVacios(){
	BASE=$1
	echo "Borrando directorios vacíos de ${BASE}"
	if [ "$BASE" != "" ];then
		DIRES=$(find "${BASE}" -type d)
		IFS=$'\n'
		for dir in ${DIRES}
		do
			NUM=$(ls "$dir"|wc -l)
			if [ $NUM -eq 0 ]; then
				rm -rf "${dir}"
			fi
		done
	else
		echo "No se ha pasado base!!"
	fi
}

BACK_DIR=$JENKINS_HOME/back
mkdir -p $BACK_DIR

cd $JENKINS_HOME/jobs
DATE=`date +%Y%m%d`
ZIP_FILE=$BACK_DIR/archived_${DATE}.zip
OLD_FILE=old.txt

if [ -f $OLD_FILE ]; then
	echo "${OLD_FILE} existe!!, borrando..."
	rm -f ${OLD_FILE}
fi
DIRECTORIOS=$(find . -name builds)
IFS=$'\n'
for direc in ${DIRECTORIOS}
do
	find ${direc} -type f -mtime +${dias} >> ${OLD_FILE}
done

if [ -s "${OLD_FILE}" ];then
	echo "Archiva ficheros con más de ${dias} dias de antigüedad"
	cat ${OLD_FILE}|zip -q ${ZIP_FILE} -@
	exitOnError ${PIPESTATUS[1]} "Archivando ficheros de más de ${dias} días de antigüedad"
fi
echo "Borra ficheros con más de ${dias} días de antigüedad"
for file in `cat ${OLD_FILE}`
do
	rm -f "${file}"
done
borraDirVacios "$JENKINS_HOME/jobs"
ls -las $BACK_DIR