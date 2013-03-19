#!/bin/sh
PB_FILE="pbfile.pb"
WEB_SERVER_SSH_PREFIX="mas2182@mac150355.med.cornell.edu"
JOB_DIR="/home/gobyweb/task-tests/XXXXXY"
RESULTS_WEB_DIR="/Users/mas2182/Lab/Projects/Plugins/plugins2-with-tasks/test-data/root-for-rnaselect/instances/tasks/RNASELECT_TASK"
INITIAL_STATE=task
TAG=SVZTTHR
RESOURCES_GOBYWEB_SERVER_SIDE_FILESETS_JAR="filesets.jar"
RESOURCES_GOBYWEB_SERVER_SIDE_DEPENDENCIES_JAR="serverside-dependencies.jar"
FILESET_COMMAND="java -cp ${RESOURCES_GOBYWEB_SERVER_SIDE_FILESETS_JAR}:${RESOURCES_GOBYWEB_SERVER_SIDE_DEPENDENCIES_JAR} org.campagnelab.gobyweb.filesets.FileSetManager"
