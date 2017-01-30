# docker_template : plantilla a utilizar
# identificador : handle compartido por el contenedor docker y el esclavo jenkins
#	levantado en el mismo.  Se usará para el destroy

./scripts/template_parser.sh \
  -t templates/Dockerfile.${docker_template}.tpl \
  -o Dockerfile \
  -s "##BASEIMAGE##=${DOCKER_REGISTRY_ECI}/${DOCKER_BASE_IMAGE_REPO}/${DOCKER_BASE_IMAGE}:${DOCKER_BASE_IMAGE_TAG}" \
  -s "##HTTP_PROXY##=${HTTP_PROXY_ECI}" \
  -s "##HTTPS_PROXY##=${HTTPS_PROXY_ECI}" \
  -s "##JENKINS_SLAVE_VERSION##=${DOCKER_SWARM_VERSION}" \
  -s "##MAVEN_VERSION##=${DOCKER_MAVEN_VERSION}" \
  -s "##SLAVE_MAVEN_HOME##=${MAVEN_HOME}" \
  -s "##NEXUS_NPM_URL##=${NEXUS_NPM_URL}" \
  -s "##NEXUS_SNAPSHOTS_URL##=${NEXUS_SNAPSHOTS_URL}" \
  -s "##NEXUS_PUBLIC_URL##=${NEXUS_PUBLIC_URL}" \
  -s "##NEXUS_RELEASES_URL##=${NEXUS_RELEASES_URL}" \
  -s "##DEPLOYMENT_USER##=${DEPLOYMENT_USER}" \
  -s "##DEPLOYMENT_PWD##=${DEPLOYMENT_PWD}" \
  -s "##JAVA_VERSION##=${DOCKER_JAVA_VERSION}" \
  -s "##SLAVE_JAVA_HOME##=${SLAVE_JAVA_HOME}" \
  -s "##GROOVY_VERSION##=${DOCKER_GROOVY_VERSION}" \
  -s "##GRADLE_VERSION##=${DOCKER_GRADLE_VERSION}" \
  -s "##GRADLE_HOME_SLAVE##=${GRADLE_HOME_SLAVE}" \
  -s "##SCMTOOLS_VERSION##=${DOCKER_SCMTOOLS_VERSION}" \
  -s "##SLAVE_SCMTOOLS_HOME##=${SCMTOOLS_HOME}" \
  -s "##NPM_GLOBAL_PACKAGES##=${DOCKER_NPM_GLOBAL_PACKAGES}" \
  -s "##SLAVE_LOCAL_UID##=${SLAVE_LOCAL_UID}" \
  -s "##SLAVE_LOCAL_USER##=${SLAVE_LOCAL_USER}" \
  -s "##ECI_PROXY_URL##=${DOCKER_PROXY_HOST}" \
  -s "##ECI_PROXY_PORT##=${DOCKER_PROXY_PORT}" \
  -s "##ECI_PROXY_USER##=${DOCKER_PROXY_USER}" \
  -s "##ECI_PROXY_PWD##=${DOCKER_PROXY_PWD} " \
  
docker build -t ${DOCKER_BASE_IMAGE}-${docker_template}:${DOCKER_SLAVE_TAG} .

docker run ${DOCKER_VOLUMENES} --name ${identificador} \
-d ${DOCKER_BASE_IMAGE}-${docker_template}:${DOCKER_SLAVE_TAG} \
-username ${SLAVE_USER} -password ${SLAVE_PWD} \
-mode=exclusive -master ${JENKINS_URL} -executors ${DOCKER_EXECUTORS} \
-labels ${identificador} -name ${identificador} ${extra_params}
