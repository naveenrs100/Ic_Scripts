#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. $DIR/../../functionsJenkins.sh

# List of vars needed
VARS_NEEDED="parentWorkspace JENKINS_HOME MAVEN_HOME JAVA_HOME DEPLOYMENT_USER DEPLOYMENT_PWD"
loadConfig $DIR

cd "${parentWorkspace}/releaseNotes"
if [ -f "versionReleaseNotes.txt" ]; then
	VERSION_RELEASENOTES=$(grep version= versionReleaseNotes.txt|awk -F= '{print $(NF)}'|tr -d '"')
	GROUPID_RELEASENOTES=$(grep groupId= versionReleaseNotes.txt|awk -F= '{print $(NF)}'|tr -d '"')
	ARTIFACTID_RELEASENOTES=$(grep artifactId= versionReleaseNotes.txt|awk -F= '{print $(NF)}'|tr -d '"')
	ARTIFACT_PATH_RELEASENOTES=$(grep artifactPath= versionReleaseNotes.txt|awk -F= '{print $(NF)}'|tr -d '"')
	URLNEXUS_RELEASENOTES=$(grep urlNexus= versionReleaseNotes.txt|awk -F= '{print $(NF)}'|tr -d '"')
	REPOSITORYID_RELEASENOTES=$(grep repositoryId= versionReleaseNotes.txt|awk -F= '{print $(NF)}'|tr -d '"')
fi

if [ "${VERSION_RELEASENOTES}" != "" ]; then
	version="${VERSION_RELEASENOTES}"
fi
if [ "${GROUPID_RELEASENOTES}" != "" ]; then
	groupId="${GROUPID_RELEASENOTES}"
fi
if [ "${ARTIFACTID_RELEASENOTES}" != "" ]; then
	artifactId="${ARTIFACTID_RELEASENOTES}"
fi
if [ "${ARTIFACT_PATH_RELEASENOTES}" != "" ]; then
	artifactPath="${ARTIFACT_PATH_RELEASENOTES}"
fi
if [ "${URLNEXUS_RELEASENOTES}" != "" ]; then
	urlNexus="${URLNEXUS_RELEASENOTES}"
fi
if [ "${REPOSITORYID_RELEASENOTES}" != "" ]; then
	repositoryId="${REPOSITORYID_RELEASENOTES}"
fi

if [ "${version}" != "" ] && [ "${groupId}" != "" ] && [ "${artifactId}" != "" ] && [ "${artifactPath}" != "" ]; then

	# Sube a nexus
	${MAVEN_HOME}/bin/mvn deploy:deploy-file -DgroupId=${groupId} -DartifactId=${artifactId} -Dversion=${version} -Dfile=${artifactPath} -Durl=${urlNexus} -DrepositoryId=${repositoryId} -DgeneratePom=false
	exitOnError $? "Uploading ${artifactId} ${version} to ${repositoryId} (${repositoryUrl})"
else
	echo "version: ${version}, groupId: ${groupId}, artifactId: ${artifactId}, artifactPath: ${artifactPath} - No se ha definido coordenadas maven para subir el artefacto!!"
fi
