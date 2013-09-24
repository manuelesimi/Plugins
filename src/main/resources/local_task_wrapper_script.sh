#!/bin/sh

function setup_task_functions {
    # define no-op function to be overridden as needed by task script:
    plugin_task() { echo; }
    # include the plugin_task function for the appropriate task:
    . ${JOB_DIR}/script.sh

}

function run_task {
   plugin_task
}

    #in case the script is re-run from the command line, we need to set here the JOB dir
    if [ -z "$JOB_DIR" ]; then
        export JOB_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
    fi

    export TMPDIR=$JOB_DIR

    cd ${JOB_DIR}
    . constants.sh
    . auto-options.sh

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
    setup_task_functions

    LOG_FILE="run-task-`date "+%Y-%m-%d-%H:%M:%S"`.log"
    run_task 2>&1 |tee ${LOG_FILE}
    if [ $?==0 ]; then
      echo "Task execution completed successfully." >>${LOG_FILE}

    else
      echo "An error occured"
    fi