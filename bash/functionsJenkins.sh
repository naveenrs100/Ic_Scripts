#!/bin/bash

function exitOnError(){
	echo $1 $2
	retval=$1
	if [ ${retval} -gt 0 ] && [ ${retval} -ne 52 ]  &&  [ ${retval} -ne 52 ] &&  [ ${retval} -ne 53 ]; then
		echo "ERROR: $2"
		exit ${retval};
	fi
}

function checkVariables(){
    for var in $1
    do
        eval test="$"${var}
        if [ "${test}" = "" ];then
			echo "${var} NOT DEFINED, please define ${var} in job's parameters or in Jenkins General Configuration"
			exit 1
		else
			echo "${var} Checked! value: ${test}"
        fi
    done
}

function loadConfig(){
	if [ "$VARS_NEEDED" != "" ]; then
		checkVariables "$VARS_NEEDED"
	else
		echo "WARNING: No variable is checked!!"
	fi
}

function retry() {
	MAX=$1
	CMD=$2
	PATTERN=$3
	LOG_FILE=$4
	FILEOUT=$5
	error=0

	if [ "${FILEOUT}" = "" ]; then
		FILEOUT=tmp.txt
	fi

	NUM=1
	while [ $NUM -le $MAX ]; do
		eval ${CMD} 2>&1| tee "${FILEOUT}"
		error=$(grep ${PATTERN} "${FILEOUT}"|wc -l)
		if [ $error -gt 0 ]; then
			echo "(try: ${NUM}) - `date` - ${CMD}"
			echo "(try: ${NUM}) - `date` -------------------------------" >> ${LOG_FILE}
			cat "${FILEOUT}" >> ${LOG_FILE}
			rm -f "${FILEOUT}"
			echo "--------------------------------------------" >> ${LOG_FILE}
			let NUM=$NUM+1
		else
			let NUM=$MAX+1
		fi
	done

	if [ $error -gt 0 ]; then
		echo "ERROR: Número máximo de intentos ${MAX} sobrepasado, detalles en ${LOG_FILE}"
		exit 1
	fi

	if [ "${FILEOUT}" = "tmp.txt" ]; then
		rm -f ${FILEOUT}
	fi
}

function download (){
	groupId=$1
	artifactId=$2
	version=$3
	extension=$4
	limit=$5
	repositoryId=public
	shift;shift;shift;shift;shift

	grupo=$(echo $groupId |sed 's:\.:/:g')

	wget -O result.xml ${MAVEN_RESOLVE}?r=${repositoryId}\&g=${groupId}\&a=${artifactId}\&v=${version}\&e=${extension}
	exitOnError $? "resolving version artifact ${artifactId}-${version}.${extension}"

	repositoryPath=$(grep repositoryPath result.xml|awk -F">" '{print $2}'|awk -F"<" '{print $1}')
	echo "repositoryPath: ${repositoryPath}"
	wget -O "${artifactId}-${version}.${extension}" -P ${ARTIFACTS_DIR} ${MAVEN_REPOSITORY}/${repositoryPath}
	exitOnError $? "Downloading artifact ${artifactId}-${version}.${extension}"

	BACK_DIR="${ARTIFACTS_DIR}/back/${artifactId}"
	mkdir -p "${BACK_DIR}/${version}"
	mv "${ARTIFACTS_DIR}/${artifactId}-${version}.${extension}" "${BACK_DIR}/${version}/${artifactId}.${extension}"

	if [ "${limit}" != "" ]; then
		cleanDir ${BACK_DIR} "" ${limit}
	fi
}

function downloadVertx (){
	groupId=$1
	artifactId=$2
	classifier=$3
	version=$4
	extension=$5
	limit=$6
	repositoryId=public
	shift;shift;shift;shift;shift

	grupo=$(echo $groupId |sed 's:\.:/:g')

	wget -O result.xml ${MAVEN_RESOLVE}?r=${repositoryId}\&g=${groupId}\&a=${artifactId}\&c=${classifier}\&v=${version}\&e=${extension}
	exitOnError $? "resolving version artifact ${artifactId}-${version}.${extension}"

	repositoryPath=$(grep repositoryPath result.xml|awk -F">" '{print $2}'|awk -F"<" '{print $1}')
	echo "repositoryPath: ${repositoryPath}"
	wget -O "${artifactId}-${version}.${extension}" -P ${ARTIFACTS_DIR} ${MAVEN_REPOSITORY}/${repositoryPath}
	exitOnError $? "Downloading artifact ${artifactId}-${version}.${extension}"

	BACK_DIR="${ARTIFACTS_DIR}/back/${artifactId}"
	mkdir -p "${BACK_DIR}/${version}"
	mv "${ARTIFACTS_DIR}/${artifactId}-${version}.${extension}" "${BACK_DIR}/${version}/${artifactId}.${classifier}.${extension}"

	if [ "${limit}" != "" ]; then
		cleanDir ${BACK_DIR} "" ${limit}
	fi
}

function cleanDir (){
	dirbase=$1
	pattern=$2
	number=$3

	if [ "${dirbase}" = "" ]; then
		echo "ERROR: dirbase and pattern no specifyed"
		exit -1
	fi
	if [ "${number}" = "" ]; then
		number=1
	fi
	total=$(ls ${dirbase}|grep "${pattern}"|wc -l)
	echo "There is ${total} files ($pattern) in $dirbase. Limit is: ${number}"
	if [ $total -ge $number ]; then
		delete=$((${total}-${number}))
		cd ${dirbase}
		ls -t|grep "${pattern}"|tail -${delete}|xargs rm -rf
	fi
}

# Devuelve un identificador aleatorio de sesión RTC
function generateRTCSessionId() {
	echo $(od -N4 -tu /dev/random | awk 'NR==1 {print $2} {}')
}

# Crea un directorio temporal
function mktempDir() {
	directorio=/tmp/$(generateRTCSessionId)
	mkdir $directorio
	echo $directorio
}

# Devuelve un directorio de configuración para una corriente, creándolo si es necesario
# Crea una réplica del sandbox dentro de DAEMONS_HOME y lo utiliza como directorio
#	de metadatos
function getRTCDaemonDir() {
	directorio=${DAEMONS_HOME}/$(pwd)
	mkdir -p $directorio
	echo $directorio
}

# Asignación de timeouts de RTC
function setRTCTimeout() {
	export SCM_DAEMON_CONNECTION_TIME_OUT=${TIMEOUT_RTC_MILISEGUNDOS}
	export SCM_DAEMON_INACTIVE_TIME_OUT=${TIMEOUT_RTC_MILISEGUNDOS}
}

