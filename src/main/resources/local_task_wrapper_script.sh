#!/bin/sh


 #include logging functions
. %JOB_DIR%/message-functions.sh

. %JOB_DIR%/job_common_functions.sh

function setup_task_functions {
    # define no-op function to be overridden as needed by task script:
    plugin_task() { echo; }
    # include the plugin_task function for the appropriate task:
    . ${JOB_DIR}/script.sh

}



function install_resources {
     #include needed function for resources installation
     ARTIFACT_REPOSITORY_DIR=%ARTIFACT_REPOSITORY_DIR%
     . %JOB_DIR%/artifacts.sh
      install_plugin_mandatory_artifacts
      if [ "${PLUGIN_ARTIFACTS_TASK}" != "false" ]; then
            install_plugin_artifacts
      fi
}


function run_task {
   ALL_REGISTERED_TAGS=""
   plugin_task
   push_job_metadata ${ALL_REGISTERED_TAGS}
}

    #in case the script is re-run from the command line, we need to set here the JOB dir
    if [ -z "$JOB_DIR" ]; then
        export JOB_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
    fi

    export TMPDIR=$JOB_DIR

    #JAVA_OPTS is used to set the amount of memory allocated to the groovy scripts.
    export JAVA_OPTS=${PLUGIN_NEED_DEFAULT_JVM_OPTIONS}

    cd ${JOB_DIR}
    . auto-options.sh
    . constants.sh

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

    #make sure that the dir in which reads files will be stored exists
    mkdir -p ${FILESET_TARGET_DIR}

    jobStarted
    setup_task_functions
    install_resources
    debug "Required resources installed."
    LOG_FILE="run-task-`date "+%Y-%m-%d-%H:%M:%S"`.log"
    run_task 2>&1 |tee ${LOG_FILE}
    if [ $?==0 ]; then
      jobCompleted
    else
      jobFailed
    fi