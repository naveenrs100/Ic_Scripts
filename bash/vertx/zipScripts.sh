#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../functionsJenkins.sh

# List of vars needed
VARS_NEEDED="listFiles fileName parentWorkspace"
loadConfig $DIR

listFiles="functionsJenkins.sh ${listFiles}"
cd "$DIR/../"
zip ${fileName} ${listFiles}
mv ${fileName} ${parentWorkspace}