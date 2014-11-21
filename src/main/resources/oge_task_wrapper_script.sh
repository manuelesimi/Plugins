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
     debug "Installing mandatory plugin artifacts" "RUNNING"
     install_plugin_mandatory_artifacts
     if [ "${PLUGIN_ARTIFACTS_TASK}" != "false" ]; then
        debug "Installing plugin artifacts" "RUNNING"
        install_plugin_artifacts
     fi
}


# Pushes some job metadata to the fileset area
# param $1: space-separated list of fileset tags registered by the job
function push_job_metadata {
   tags="$@"
   rm -rf ${JOB_DIR}/${TAG}.properties
   rm -rf %FILESET_AREA%/${TAG:0:1}/${TAG}
   echo "JOB=${TAG}" >> ${JOB_DIR}/${TAG}.properties
   echo "OWNER=${OWNER}" >> ${JOB_DIR}/${TAG}.properties
   echo "PLUGIN=${PLUGIN_ID}" >> ${JOB_DIR}/${stats_file}
   echo "COMPLETED=`date +"%Y-%m-%d %T%z"`" >> ${JOB_DIR}/${TAG}.properties
   echo "TAGS=${tags}" >> ${JOB_DIR}/${TAG}.properties
   echo "SHAREDWITH=" >> ${JOB_DIR}/${TAG}.properties
   REGISTERED_TAGS=`${FILESET_COMMAND} --push --fileset-tag ${TAG} JOB_METADATA: ${JOB_DIR}/${TAG}.properties`
   debug "The following JOB_METADATA instance has been successfully registered: ${REGISTERED_TAGS}" "RUNNING"
}

function dieUponError {
  RETURN_STATUS=$?
  DESCRIPTION=$1
  if [ ! ${RETURN_STATUS} -eq 0 ]; then
       fatal "Task failed. Error description: ${DESCRIPTION}" "COMPLETED"
       exit ${RETURN_STATUS}
  fi

}



function run_task {
   ALL_REGISTERED_TAGS=""
   plugin_task
   push_job_metadata ${ALL_REGISTERED_TAGS}
   #${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_COMPLETED_STATUS} --description "Task completed" --index 0 --job-type job
   info "Task completed" "COMPLETED"
}

function setup {

    #JAVA_OPTS is used to set the amount of memory allocated to the groovy scripts.
    export JAVA_OPTS=${PLUGIN_NEED_DEFAULT_JVM_OPTIONS}

    export JOB_DIR=%JOB_DIR%
    trace "JOB _DIR is ${JOB_DIR}" "RUNNING"

    if [ -z "$TMPDIR" ]; then
        export TMPDIR=${JOB_DIR}
    fi

    CURRENT_PART=1 # Needed when reporting errors with dieUponError

    # include value definitions for automatic options:
    . %JOB_DIR%/auto-options.sh

    # define job specific constants:
    . %JOB_DIR%/constants.sh

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
    echo SGE: job current state = ${STATE}
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

    # Copy the goby and support tools to the local node
    if [ -d "${TMPDIR}" ]; then
            echo Copying goby dir to the local node ...
            /bin/cp ${GOBY_DIR}/* ${TMPDIR}
            cd ${TMPDIR}
            export TMPDIR
    fi

    # Show the java & goby.jar version
    echo "Java version"
    java ${PLUGIN_NEED_DEFAULT_JVM_OPTIONS} -version
    dieUponError "Could not obtain Java version number."

    #make sure that the dir in which reads files will be stored exists
    mkdir -p ${FILESET_TARGET_DIR}

}

setup

 #include logging functions
. %JOB_DIR%/message-functions.sh

. %JOB_DIR%/job_common_functions.sh


case ${STATE} in
    task)
        setup_task_functions
        install_resources
        LOG_FILE="run-task-`date "+%Y-%m-%d-%H:%M:%S"`.log"
        run_task 2>&1 |tee ${LOG_FILE}
        STATUS=$?
        if [ ${STATUS}==0 ]; then
         #echo "Task execution completed successfully." >>${LOG_FILE}
         jobCompleted
        else
         #echo "An error occured"
          jobFailed
          exit ${STATUS}
        fi
        ;;

    *)
        cd ${JOB_DIR}
        if [[ -z "$JOBS_HOLD_LIST" ]]; then
            HOLD_OPTION="-hold_jid ${JOBS_HOLD_LIST}"
        else
            HOLD_OPTION=""
        fi
        SUBMISSION=`qsub -N ${TAG}.submit ${HOLD_OPTION} -terse -l ${PLUGIN_NEED_PROCESS} -r y -v STATE=${INITIAL_STATE} oge_task_wrapper_script.sh`
        echo ${SUBMISSION}
        jobStarted
        ;;
esac

