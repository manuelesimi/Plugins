#!/bin/bash -l

# Script to trigger the installation of artifacts for a resource plugin. Does nothing after installation.

slchoose sun_jdk 6.0.25 dist

function install_resource {
   echo ""
}


# This function should be called when an error condition requires to terminate the job. The first argument is a description
# of the error that will be communicated to the end-user (will be displayed in the GobyWeb job status interface).

function dieUponError {
    RETURN_STATUS=$?
    DESCRIPTION=$1

    if [ ! ${RETURN_STATUS} -eq 0 ]; then
       # Failed, no result to copy
       #publish_exceptions
       exit ${RETURN_STATUS}
    fi
}

#in case the script is re-run from the command line, we need to set here the JOB dir
if [ -z "$JOB_DIR" ]; then
    export JOB_DIR=="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
fi

    export TMPDIR=$JOB_DIR

    cd ${JOB_DIR}
    # create a fake goby directory, necessary to find artifact-manager.jar in ${JOB_DIR}/goby
    ln -s . goby

    . auto-options.sh
    . constants.sh
    . artifacts.sh


GOBY_DIR=${JOB_DIR}/goby
if [ ! -d ${GOBY_DIR} ]; then
    mkdir -p ${GOBY_DIR}
    /bin/cp ${JOB_DIR}/global_goby.jar ${GOBY_DIR}/goby.jar
    /bin/cp ${JOB_DIR}/log4j.properties ${GOBY_DIR}/
    /bin/cp ${JOB_DIR}/QueueWriter.groovy ${GOBY_DIR}/
    /bin/cp ${JOB_DIR}/TsvVcfToSqlite.groovy ${GOBY_DIR}/
    /bin/cp ${JOB_DIR}/icb-groovy-support.jar ${GOBY_DIR}/
    /bin/cp ${JOB_DIR}/artifact-manager.jar ${GOBY_DIR}/
    /bin/cp ${JOB_DIR}/serverside-dependencies.jar ${GOBY_DIR}/
    /bin/cp ${JOB_DIR}/stepslogger.jar ${GOBY_DIR}/

fi

LOG_FILE="resource-install-`date "+%Y-%m-%d-%H:%M:%S"`.log"
install_plugin_artifacts 2>&1 |tee ${LOG_FILE}
if [ $?==0 ]; then
  echo "Installation completed successfully." >>${LOG_FILE}
else
  echo "An error occured"
fi
