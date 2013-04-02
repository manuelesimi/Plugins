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


java -cp .../lib/plugins-sdk.jar:cluster_gateway.properties:$CLASSPATH org.campagnelab.gobyweb.clustergateway.registration.FileSetRegistration --action unregister "$@"

