#!/bin/sh

#Usage: java org.campagnelab.gobyweb.clustergateway.registration.FileSetRegistration
#   [(-p|--plugins-dir) <pluginDir>] [(-s|--storage-root-dir) <storageArea>] (-t|--tag) <tag> [(-m|--mode) <mode>] [-h|--help] --action unregister

# Sample options for a remote unregistration:
# --fileset-area gobyweb@spanky.med.cornell.edu:/home/gobyweb/TRIAL-FILES/GOBYWEB_FILES/A
# --plugins-dir /Users/mas2182/Lab/Projects/Plugins/plugins2-with-tasks/test-data/root-for-rnaselect
# --owner Manuele.FromClusterGateway
# --mode remote
# --action unregister
# --tag YERYVFK

# Sample options for a local unregistration:
# --fileset-area test-results
# --plugins-dir /Users/mas2182/Lab/Projects/Plugins/plugins2-with-tasks/test-data/root-for-rnaselect
# --owner Manuele.FromClusterGateway
# --mode local
# --action unregister
# --tag GXJAEOJ
WORKING_DIR=`dirname "$0"`
if [[ $OSTYPE == "cygwin" ]]; then
    WORKING_DIR=`cygpath -m "${WORKING_DIR}"`
fi
PARENT_DIR=`dirname ${WORKING_DIR}`
LIB_DIR=${PARENT_DIR}/lib
TARGET_DIR=${PARENT_DIR}/target
SNAPSHOT_JAR="${TARGET_DIR}/plugins-sdk.jar"
#echo "Snapshot jar: ${SNAPSHOT_JAR}"
#echo java -cp ${LIB_DIR}/plugins-sdk.jar:${SNAPSHOT_JAR}:${WORKING_DIR}/cluster_gateway.properties org.campagnelab.gobyweb.clustergateway.registration.FileSetRegistration --action register "$@"
java -cp ${LIB_DIR}/plugins-sdk.jar:${SNAPSHOT_JAR}:${WORKING_DIR}/cluster_gateway.properties org.campagnelab.gobyweb.clustergateway.registration.FileSetRegistration --action unregister "$@"

