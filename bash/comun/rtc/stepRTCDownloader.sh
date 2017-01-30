#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh
. $DIR/../mutex/mutex-wait.sh

# List of vars needed
VARS_NEEDED="SCMTOOLS_HOME userRTC pwdRTC urlRTC workspaceRTC component stream"
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

function setWorkspace(){


	# Critical section: isolate operations on this repository workspace
	getLock ${workspaceRTC}

	exist=$(${SCM_CMD} list workspaces ${LOGIN} -n "${workspaceRTC}" |grep -iw "${workspaceRTC}"|wc -l)
	existComp=1
	if [ "$recreateWS" = "true" ]; then
		${SCM_CMD} workspace delete ${LOGIN} "${workspaceRTC}"
		echo "workspace \"${workspaceRTC}\" deleted!"
		exist=0
	fi
	echo "exists workspace \"${workspaceRTC}\"?: $exist"
	if [ $exist -eq 0 ]; then
		echo "creating workspace \"${workspaceRTC}\""
		${SCM_CMD} create workspace -e ${LOGIN} "${workspaceRTC}"
		exitOnError $? "Creating workspace"
		existComp=0
	else
		existComp=$(${SCM_CMD} list components ${LOGIN} "${workspaceRTC}"|grep -iw "${component}"|wc -l)
	fi

	release ${workspaceRTC}
	# End of critical section: release lock on repository workspace

	if [ $existComp -eq 0 ]; then
		echo "Adding component: \"$component\""
		if [ "${1}" != "" ]; then
			${SCM_CMD} workspace add-components ${LOGIN} "${workspaceRTC}" "$component" -b "${1}"
			exitOnError $? "Adding component baseline ${1}"
		else
			${SCM_CMD} workspace add-components ${LOGIN} "${workspaceRTC}" "$component" -s "$stream"
			exitOnError $? "Adding component"
		fi
	elif [ "${1}" != "" ]; then
		echo "Replacing component baseline ($1): \"$component\""
		${SCM_CMD} workspace replace-components -b "${1}" -o ${LOGIN} "${workspaceRTC}" workspace "${workspaceRTC}" "${component}" 2>&1| tee replace.txt
		exitOnError $? "Replacing component"
	fi
}

function setBaseline(){
	aliasLinea=$(${SCM_CMD} list snapshots ${LOGIN} "${stream}" -m 20|grep "${snapshot}"|head -1|awk '{print $1}'|tr -d '('|tr -d ')')
	if [ "$aliasLinea" != "" ]; then
		componentes=$(${SCM_CMD} list components -s ${aliasLinea} -v ${LOGIN} "${stream}")
		SAVEIFS=$IFS
		IFS=$'\n'
		FOUND="false"
		for componente in ${componentes}
		do
			existe=$(echo ${componente}|grep ${component})
			if [ "${existe}" != "" ]; then
				FOUND="true"
			fi
			if [ "${existe}" = "" ] && [ "${FOUND}" = "true" ]; then
				echo "linea: ${componente}"
				FOUND="false"
				baseline=$(echo ${componente}|awk -F'"' '{print $2}')
				echo "Baseline \"${baseline}\" for component \"${component}\" in snapshot \"${snapshot}\" found"
			fi
		done
		IFS=$SAVEIFS
	else
		echo "IMPOSIBLE ENCONTRAR SNAPSHOT: ${snapshot}"
	fi
}

function sincroniza(){
	echo "sincroniza (${1}).................."
	setWorkspace ${1}
	${SCM_CMD} load "${workspaceRTC}" ${LOGIN} -f "${component}"
	exitOnError $? "loading workspace from RTC"
	if [ "$1" = "" ]; then
		${SCM_CMD} compare workspace "$workspaceRTC" stream "$stream" ${LOGIN} -I sw -C "|{name}|{email}|" -D "|yyyy-MM-dd-HH:mm:ss|" -f i | tee changesetCompare.txt
		exitOnError ${PIPESTATUS[0]} "comparing workspace with stream"
		echo "${SCM_CMD} accept ${LOGIN} -C "$component" --flow-components -o -v --target "$workspaceRTC" -s "${stream}"| tee changesetAccept.txt"
		${SCM_CMD} accept ${LOGIN} -C "$component" --flow-components -o -v --target "$workspaceRTC" -s "${stream}"| tee changesetAccept.txt
		exitOnError ${PIPESTATUS[0]} "accepting changes"
		if [ "$autoResolve" = "true" ]; then
			conflict=$(grep \#- changesetAccept.txt |sort|uniq|awk {'print "."$2'}|tr '\n' ' ')
			if [ "$conflict" != "" ];then
				${SCM_CMD} resolve -p ${LOGIN} ${conflict}
				exitOnError $? "Resolving conflicts"
			fi
		fi
	else
		${SCM_CMD} compare workspace "$workspaceRTC" baseline "$baseline" -c "$2" ${LOGIN} -I sw -C "|{name}|{email}|" -D "|yyyy-MM-dd-HH:mm:ss|" -f i | tee changesetCompare.txt
		exitOnError ${PIPESTATUS[0]} "comparing workspace with stream"
	fi
}

if [ "$snapshot" != "" ]; then
	 setBaseline
fi

if [ "$baseline" = "" ]; then
	sincroniza
else
	# busca baseline en RTC
	aliasfin=""
	${SCM_CMD} list baselines -w "${stream}" -C "${component}" ${LOGIN}  2>&1 | tee baselines.txt
	exitOnError ${PIPESTATUS[0]} "List baselines on components"
	ambiguo=$(cat baselines.txt| grep "Ambiguous component")
	if [ "$ambiguo" != "" ];then
		SAVEIFS=$IFS
		IFS=$'\n'
		for line in `cat baselines.txt| grep "("`
		do
	        alias=$(echo $line|tr ')' '('|awk -F\( '{print $(NF-1)}')
	        haybaseline=$(${SCM_CMD} list baselines -w "${stream}" -C "${alias}" ${LOGIN}| grep -w "$baseline")
	        echo $haybaseline
	        if [ "$haybaseline" != "" ]; then
	        	aliasfin=$alias
	        fi
		done
		IFS=$SAVEIFS
	else
		haybaseline=$(cat baselines.txt| grep -w "$baseline")
		if [ "$haybaseline" != "" ]; then
			line=$(cat baselines.txt| grep -w Component)
			aliasfin=$(echo $line|tr ')' '('|awk -F\( '{print $(NF-1)}')
			echo "aliasfin: ${aliasfin}"
		fi
	fi

	if [ "$aliasfin" = "" ]; then
		echo "${baseline} NO EXISTE!!!"
		# si no, me bajo lo último, hago baseline y lo subo a la corriente.
		sincroniza
		cambia=$(cat changesetAccept.txt|grep "Workspace unchanged")
		if [ "$cambia" = "" ] || [ "$recreateWS" = "true" ];then
			echo "va a crear baseline.................."
			${SCM_CMD} create baseline ${LOGIN} "$workspaceRTC" "${baseline}" "${component}"
			exitOnError $? "Creating baseline"
			${SCM_CMD} deliver ${LOGIN} --source "$workspaceRTC" -t "${stream}"
			exitOnError $? "Delivering changes"
		else
			echo "No hay cambios en el workspace, NO SE PUEDE CREAR UNA NUEVA VERSION"
			exit 1
		fi
	else
		# Si existe sincronizo la versión con los archivos locales
		echo "${baseline} EXISTE!!!"
		if [ "$nosinc" = "" ]; then
			echo "actualizando archivos locales...."
			sincroniza ${baseline} ${aliasfin}
		fi
	fi
fi

if [[ $configDir ]] ; then
# Limpieza del directorio temporal de metadatos de RTC
echo "Limpiando el directorio temporal de metadatos ${configDir}..."
rm -r $configDir
fi
