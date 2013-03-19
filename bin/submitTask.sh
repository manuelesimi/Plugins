#!/bin/sh


#Usage: java org.campagnelab.gobyweb.clustergateway.submission.ClusterGateway
#   [(-p|--plugins-dir) <pluginDir>] [(-s|--storage-root-dir) <storageArea>] [(-i|--input-filesets)[:inputFilesets1,inputFilesets2,...,inputFilesetsN ]] (-t|--task-id) <task> [(-q|--queue) <queue>] [-h|--help] [(-m|--mode) <mode>]

#Sample options:
# --job-root-dir gobyweb@spanky.med.cornell.edu:/home/gobyweb/TRIAL-FILES/GOBYWEB_SGE_JOBS
# --storage-root-dir gobyweb@spanky.med.cornell.edu:/home/gobyweb/TRIAL-FILES/GOBYWEB_FILES/A/
# --plugins-dir /Users/mas2182/Lab/Projects/Plugins/plugins2-with-tasks/test-data/root-for-rnaselect
# --queue rascals.q
# --input-filesets:TESTTAG,TESTTAG2,TESTTAG3
# --owner Manuele.FromClusterGateway
# --task-id RNASELECT_TASK
# --mode remote

java -cp ../lib/plugins-distro.jar:cluster_gateway.properties:$CLASSPATH org.campagnelab.gobyweb.clustergateway.submission.ClusterGateway "$@"
