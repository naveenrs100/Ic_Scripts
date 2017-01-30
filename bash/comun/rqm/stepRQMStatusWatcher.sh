#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

# List of vars needed
VARS_NEEDED="JAVA_HOME JENKINS_HOME RQM_REST_SERVICE rqmClient rqmVersion userRTC pwdRTC buildDefinition alias"
loadConfig $DIR

JAVA_CMD=${JAVA_HOME}/bin/java
rm -f resultRQM.xml
${JAVA_CMD} -jar tmp/${rqmClient}-${rqmVersion}.jar -command GET -user $userRTC -password $pwdRTC -buildDefinition "$buildDefinition" -filepath resultRQM.xml -url ${RQM_REST_SERVICE}/com.ibm.rqm.integration.service.IIntegrationService/resources/${alias}/executionsequence

echo 
cat resultRQM.xml
echo
echo