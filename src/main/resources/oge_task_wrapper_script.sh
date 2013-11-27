#!/bin/bash -l

# Execute the script from the current directory
#$ -cwd

# Combine SGE error and output files.
#$ -j y

# Cluster queue to use
#$ -q %QUEUE_NAME%

slchoose sun_jdk 6.0.25 dist

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
    install_plugin_artifacts
}

function run_task {
   plugin_task

}

function setup {
    #in case the script is re-run from the command line, we need to set here the JOB dir
    #if [ -z "$JOB_DIR" ]; then
    #    export JOB_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
    #fi

    export JOB_DIR=%JOB_DIR%
    echo "JOB _DIR is ${JOB_DIR}"

    if [ -z "$TMPDIR" ]; then
        export TMPDIR=${JOB_DIR}
    fi

    # define job specific constants:
    . %JOB_DIR%/constants.sh

    # include value definitions for automatic options:
    . %JOB_DIR%/auto-options.sh

    #JAVA_OPTS is used to set the amount of memory allocated to the groovy scripts.
    export JAVA_OPTS=${PLUGIN_NEED_DEFAULT_JVM_OPTIONS}

    echo ------------------------------------------------------
    echo This machines hostname: `hostname`
    echo ------------------------------------------------------
    echo SGE: qsub is running on ${SGE_O_HOST}
    echo SGE: originating queue is ${QUEUE}
    echo SGE: executing cell is ${SGE_CELL}
    echo SGE: working directory is ${SGE_O_WORKDIR}
    echo SGE: execution mode is ${ENVIRONMENT}
    echo SGE: execution host is ${HOSTNAME}
    echo SGE: job identifier is ${JOB_ID}
    echo SGE: job name is ${JOB_NAME}
    echo SGE: task number is ${SGE_TASK_ID}
    echo SGE: current home directory is ${SGE_O_HOME}
    echo SGE: scratch directory is ${TMPDIR}
    echo SGE: PATH = ${SGE_O_PATH}
    echo ------------------------------------------------------


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
}

setup

case ${STATE} in
    task)
        setup_task_functions
        install_resources
        LOG_FILE="run-task-`date "+%Y-%m-%d-%H:%M:%S"`.log"
        run_task 2>&1 |tee ${LOG_FILE}
        if [ $?==0 ]; then
         echo "Task execution completed successfully." >>${LOG_FILE}
        else
         echo "An error occured"
        fi
        ;;

    *)
        cd ${JOB_DIR}
        SUBMISSION=`qsub -N ${TAG}.submit -terse -v STATE=${INITIAL_STATE} oge_task_wrapper_script.sh`
        echo ${SUBMISSION}
        ;;
esac

