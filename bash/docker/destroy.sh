# identificador : handle compartido por el contenedor docker y el esclavo jenkins
#	levantado en el mismo.  Se usará para el destroy

docker stop $(docker ps -a -q --filter="name=${identificador}")
docker rm -v=true $(docker ps -a -q --filter="name=${identificador}")