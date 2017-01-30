#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

# List of vars needed
VARS_NEEDED="jobInvoker JENKINS_HOME"
loadConfig $DIR

function cleanbuild {
	echo "cleaning $1"
	cd "$1"
	ls -l
	rm -Rf builds/*
	rm -Rf lastSuccesful
	rm -Rf lastStable/
	rm -Rf workspace/*
	rm -Rf modules/*
	rm -f lastSuccessful lastStable
	rm -f nextBuildNumber
	touch nextBuildNumber
	echo 1 >> nextBuildNumber
	cd -
}

cleanbuild "$JENKINS_HOME/jobs/$jobInvoker"