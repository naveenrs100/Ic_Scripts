docker run ${DOCKER_VOLUMENES} --name ${identificador} \
-d ${DOCKER_BASE_IMAGE}-${docker_template}:${DOCKER_SLAVE_TAG} \
-username ${SLAVE_USER} -password ${SLAVE_PWD} \
-mode=exclusive -master ${JENKINS_URL} -executors ${DOCKER_EXECUTORS} \
-labels ${identificador} -name ${identificador} ${extra_params}
