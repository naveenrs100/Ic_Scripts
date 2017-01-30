if [ -d "${DEPLOYMENT_BASE_PATH}/${stream}/${component}/${deploy_env}" ]; then
    cd "${DEPLOYMENT_BASE_PATH}/${stream}/${component}/${deploy_env}"
    for dir in $(ls -d */);
    do
       ls "$dir"
       if [ -f "${dir}deployer_pom.xml" ];then
         ${MAVEN_HOME}/bin/mvn -f "${dir}/deployer_pom.xml" -U clean org.apache.maven.plugins:maven-dependency-plugin:2.1:copy-dependencies
       fi
    done
  echo " END COPY DEPENDENCIES"
fi