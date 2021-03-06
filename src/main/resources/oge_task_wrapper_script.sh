#!/bin/bash -l

# Execute the script from the current directory
#$ -cwd

# Combine SGE error and output files.
#$ -j y

# Cluster queue to use
#$ -q %QUEUE_NAME%

WRAPPER_SCRIPT_PREFIX="oge_task_wrapper_script"
. %JOB_DIR%/common.sh


function setup_task_functions {

    # define no-op function to be overridden as needed by task script:
    plugin_task() { echo; }
    # include the plugin_task function for the appropriate task:
    . ${JOB_DIR}/script.sh
    enforce_minimum_bound_on_align_parts
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
   echo "The following JOB_METADATA instance has been successfully registered: ${REGISTERED_TAGS}"
}

function dieUponError {
  RETURN_STATUS=$?
  DESCRIPTION=$1
  if [ ! ${RETURN_STATUS} -eq 0 ]; then
       echo "Task failed. Error description: ${DESCRIPTION}"
       exit ${RETURN_STATUS}
  fi

}



function run_task {
   ALL_REGISTERED_TAGS=""
   
   plugin_task
   push_job_metadata ${ALL_REGISTERED_TAGS}
   ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_COMPLETED_STATUS} --description "Task completed" --index 0 --job-type job
}

function setup {

    #JAVA_OPTS is used to set the amount of memory allocated to the groovy scripts.
    export JAVA_OPTS=${PLUGIN_NEED_DEFAULT_JVM_OPTIONS}

    export JOB_DIR=%JOB_DIR%
    echo "JOB _DIR is ${JOB_DIR}"

    CURRENT_PART=1 # Needed when reporting errors with dieUponError

    # include value definitions for automatic options:
    . %JOB_DIR%/auto-options.sh

    # define job specific constants:
    . %JOB_DIR%/constants.sh

    #JAVA_OPTS is used to set the amount of memory allocated to the groovy scripts.
    export JAVA_OPTS=${PLUGIN_NEED_DEFAULT_JVM_OPTIONS}


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

    #see where we are running
    print_OGE_env
}


case ${STATE} in
    task)
        #newPhase 1
        initializeJobEnvironment
         # delegate everything else either inside container or execute directly legacy script:
        delegate_oge_job_script ${STATE}
        #phaseCompleted
        #activityCompleted TASK_EXECUTION
        ;;

    submit)
        newActivity TASK_EXECUTION
        newPhase 1
        initializeJobEnvironment
        setup
        cd ${JOB_DIR}
        if [[ -z ${JOBS_HOLD_LIST+x} ]]; then
           HOLD_OPTION=""
        else
           HOLD_OPTION="-hold_jid ${JOBS_HOLD_LIST}"
        fi
        SUBMISSION=`qsub -N ${TAG}.submit ${HOLD_OPTION} -terse -l ${PLUGIN_NEED_PROCESS} -r y -v STATE=${INITIAL_STATE} oge_task_wrapper_script.sh`
        phaseCompleted 
        echo ${SUBMISSION}
        ;;

esac