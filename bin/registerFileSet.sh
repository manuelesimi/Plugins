#!/bin/sh

#Usage: java org.campagnelab.gobyweb.clustergateway.registration.FileSetRegistration
#   [(-p|--plugins-dir) <pluginDir>] [(-s|--storage-root-dir) <storageArea>] (-t|--tag) <tag> (-i|--fileset-id) <id> [(-m|--mode) <mode>] [-h|--help] --action register entries1 entries2 ... entriesN

# Sample options for a remote registration:
# --fileset-area gobyweb@spanky.med.cornell.edu:/home/gobyweb/TRIAL-FILES/GOBYWEB_FILES/A
# --plugins-dir /Users/mas2182/Lab/Projects/Plugins/plugins2-with-tasks/test-data/root-for-rnaselect
# --source-dir /Users/mas2182/Lab/Projects/Plugins/plugins2-with-tasks/test-data/...
# --mode local
# --action register
# COMPACT_READS: *.compact-reads

# Sample options for a local registration:
# --fileset-area test-results
# --plugins-dir /Users/mas2182/Lab/Projects/Plugins/plugins2-with-tasks/test-data/root-for-rnaselect
# --owner Manuele.FromClusterGateway
# --source-dir /Users/mas2182/Lab/Projects/Plugins/plugins2-with-tasks/test-data/...
# --mode local
# --action register
# COMPACT_READS: *.compact-reads

java -cp ../lib/plugins-sdk.jar:cluster_gateway.properties:$CLASSPATH org.campagnelab.gobyweb.clustergateway.registration.FileSetRegistration --action register "$@"
