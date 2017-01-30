REM Baja la arquitectura
mvn process-sources es.eci.wmb:brokermojo-maven-plugin:2.0.10.0:brokerinit

REM Compilación (genera el bar "agnóstico")
mvn es.eci.wmb:brokermojo-maven-plugin:2.0.12.0:brokercompile -Dentornos="LOCAL" -DoverrideWithOutCompile=false -Doverride=false

REM Generar el zip
mvn org.apache.maven.plugins:maven-assembly-plugin:single -Ddescriptor=maven_conf/assembly/bar.xml

REM  --> Entra UrbanCode

REM Aplicación de propiedades
mvn es.eci.wmb:brokermojo-maven-plugin:2.0.12.0:brokercompile -Dentornos="DESA" -DoverrideWithOutCompile=true -Doverride=true

REM Despliegue de cada bar en cada entorno
mvn -e es.eci.wmb:brokermojo-maven-plugin:2.0.12.0:brokerdeploy -Dentornos="DESA"