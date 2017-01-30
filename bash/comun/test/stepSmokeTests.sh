#!/bin/bash
sleep ${delay}
out=0
SAVEIFS=$IFS
IFS='@'
echo "script: -${script}-"
if [ "${script}" != "" ] && [ "${script}" != "null" ];then
	for line in ${script}
	do
		# Trim de la linea
		line=$(echo $line|sed -e 's/^ *//g' -e 's/ *$//g')
		url=$(echo $line|awk -F\; '{print $(NF-1)}')
		nombre=$(echo $line|awk -F\; '{print $(NF-2)}')
		clave=$(echo $line|awk -F\; '{print $(NF)}')
		echo "$nombre .........Llamando: ${url}"
		wget -q $url -O ${nombre}.html
		if [ -f "${nombre}.html" ]; then
			res=$(cat ${nombre}.html|grep "$clave")
			if [ "$res" = "" ]; then
				echo "ERROR: $nombre disponible pero no encuentro $clave dentro"
				out=1
			else
				echo "$nombre OK"
			fi
		else
			echo "ERROR: $nombre NO DISPONIBLE!!!"
			out=1
		fi
	done
fi
IFS=${SAVEIFS}
exit $out