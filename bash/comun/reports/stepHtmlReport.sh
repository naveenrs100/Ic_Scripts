#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

# List of vars needed
VARS_NEEDED="IHS_DESCARGAS"
loadConfig $DIR