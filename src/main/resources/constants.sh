#!/bin/sh
JOB_DIR=%%JOB_DIR%%
TMPDIR=`pwd` #TODO: temporary, it has to be replaced with the real TMPDIR when analyses and aligners will be submitted with the Cluster Gateway
TAG=%%TAG%%
ARTIFACT_REPOSITORY_DIR=%%ARTIFACT_REPOSITORY_DIR%%
INITIAL_STATE=task
RESOURCES_GOBYWEB_SERVER_SIDE_FILESETS_JAR="filesets.jar"
RESOURCES_GOBYWEB_SERVER_SIDE_DEPENDENCIES_JAR="serverside-dependencies.jar"
FILESET_COMMAND="java -cp ${RESOURCES_GOBYWEB_SERVER_SIDE_FILESETS_JAR}:${RESOURCES_GOBYWEB_SERVER_SIDE_DEPENDENCIES_JAR} org.campagnelab.gobyweb.filesets.JobInterface --fileset-area-cache ${TMPDIR} --job-tag %%TAG%%"
GOBY_DIR=${JOB_DIR}
export SGE_O_WORKDIR=${JOB_DIR}


set -x
